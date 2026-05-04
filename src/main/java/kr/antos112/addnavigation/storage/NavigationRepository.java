package kr.antos112.addnavigation.storage;

import kr.antos112.addnavigation.model.NavigationPoint;

import java.util.Collection;
import java.util.Optional;

/**
 * Persistence abstraction for navigation points.
 * <p>
 * The default implementation uses SQLite, but other storage engines can be
 * plugged in by implementing this interface.
 */
public interface NavigationRepository extends AutoCloseable {

    /**
     * Initializes the underlying storage connection and schema.
     */
    void init();

    /**
     * Saves or updates a navigation point.
     *
     * @param point navigation point to persist
     * @return true when the save succeeded
     */
    boolean save(NavigationPoint point);

    /**
     * Removes a navigation point by name.
     *
     * @param name point name
     * @return true when the point existed and was deleted
     */
    boolean delete(String name);

    /**
     * Finds a navigation point by name.
     *
     * @param name point name
     * @return matching point if present
     */
    Optional<NavigationPoint> findByName(String name);

    /**
     * Loads every stored navigation point.
     *
     * @return all known points
     */
    Collection<NavigationPoint> findAll();

    /**
     * Closes the repository and releases any resources.
     */
    @Override
    void close();
}
