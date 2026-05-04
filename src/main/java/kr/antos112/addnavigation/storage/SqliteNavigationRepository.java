package kr.antos112.addnavigation.storage;

import kr.antos112.addnavigation.model.NavigationPoint;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.Optional;

/**
 * SQLite-backed implementation of {@link NavigationRepository}.
 */
public final class SqliteNavigationRepository implements NavigationRepository {
    private final File databaseFile;
    private Connection connection;

    /**
     * Creates a repository bound to the given database file.
     *
     * @param databaseFile SQLite database file
     */
    public SqliteNavigationRepository(File databaseFile) {
        this.databaseFile = databaseFile;
    }

    /**
     * Opens the database connection and creates the schema when needed.
     */
    @Override
    public void init() {
        try {
            if (databaseFile.getParentFile() != null) {
                databaseFile.getParentFile().mkdirs();
            }
            connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS navigation_points (
                            name TEXT PRIMARY KEY,
                            world_name TEXT NOT NULL,
                            x REAL NOT NULL,
                            y REAL NOT NULL,
                            z REAL NOT NULL
                        )
                        """);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to initialize SQLite repository", e);
        }
    }

    /**
     * Saves or updates a point in the SQLite table.
     */
    @Override
    public synchronized boolean save(NavigationPoint point) {
        String sql = """
                INSERT INTO navigation_points(name, world_name, x, y, z)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT(name) DO UPDATE SET
                    world_name = excluded.world_name,
                    x = excluded.x,
                    y = excluded.y,
                    z = excluded.z
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, normalize(point.name()));
            ps.setString(2, point.worldName());
            ps.setDouble(3, point.x());
            ps.setDouble(4, point.y());
            ps.setDouble(5, point.z());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to save navigation point", e);
        }
    }

    /**
     * Deletes a point by its lowercase-normalized name.
     */
    @Override
    public synchronized boolean delete(String name) {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM navigation_points WHERE name = ?")) {
            ps.setString(1, normalize(name));
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to delete navigation point", e);
        }
    }

    /**
     * Finds a point by name.
     */
    @Override
    public synchronized Optional<NavigationPoint> findByName(String name) {
        try (PreparedStatement ps = connection.prepareStatement("""
                SELECT name, world_name, x, y, z
                FROM navigation_points
                WHERE name = ?
                """)) {
            ps.setString(1, normalize(name));
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(new NavigationPoint(
                        rs.getString("name"),
                        rs.getString("world_name"),
                        rs.getDouble("x"),
                        rs.getDouble("y"),
                        rs.getDouble("z")
                ));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load navigation point", e);
        }
    }

    /**
     * Loads all saved navigation points.
     */
    @Override
    public synchronized Collection<NavigationPoint> findAll() {
        Collection<NavigationPoint> points = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement("""
                SELECT name, world_name, x, y, z
                FROM navigation_points
                ORDER BY name ASC
                """);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                points.add(new NavigationPoint(
                        rs.getString("name"),
                        rs.getString("world_name"),
                        rs.getDouble("x"),
                        rs.getDouble("y"),
                        rs.getDouble("z")
                ));
            }
            return points;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to list navigation points", e);
        }
    }

    /**
     * Closes the database connection.
     */
    @Override
    public synchronized void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
            }
            connection = null;
        }
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT);
    }
}
