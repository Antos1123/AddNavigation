package kr.antos112.addnavigation.navigation;

import kr.antos112.addnavigation.model.NavigationSettings;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.*;

/**
 * A* pathfinder that searches walkable routes inside the current world.
 */
public final class PathFinder {
    private static final int[][] DIRECTIONS = {
            {1, 0},
            {-1, 0},
            {0, 1},
            {0, -1}
    };

    private final NavigationSettings settings;

    /**
     * Creates a pathfinder with the current navigation settings.
     *
     * @param settings runtime navigation settings
     */
    public PathFinder(NavigationSettings settings) {
        this.settings = settings;
    }

    /**
     * Finds a path from start to goal using A* search.
     *
     * @param start start location
     * @param goal goal location
     * @return block-by-block path, or an empty list when no route exists
     */
    public List<Location> findPath(Location start, Location goal) {
        if (start == null || goal == null) return List.of();
        if (start.getWorld() == null || goal.getWorld() == null) return List.of();
        if (!start.getWorld().equals(goal.getWorld())) return List.of();

        World world = start.getWorld();
        BlockPos startPos = new BlockPos(start.getBlockX(), start.getBlockY(), start.getBlockZ());
        BlockPos goalPos = new BlockPos(goal.getBlockX(), goal.getBlockY(), goal.getBlockZ());

        PriorityQueue<NodeRecord> open = new PriorityQueue<>(Comparator
                .comparingDouble(NodeRecord::fCost)
                .thenComparingDouble(NodeRecord::hCost));

        Map<BlockPos, NodeRecord> all = new HashMap<>();
        Set<BlockPos> closed = new HashSet<>();

        NodeRecord startRecord = new NodeRecord(startPos, null, 0.0, heuristic(startPos, goalPos));
        open.add(startRecord);
        all.put(startPos, startRecord);

        int explored = 0;
        while (!open.isEmpty() && explored < settings.maxSearchNodes()) {
            NodeRecord current = open.poll();
            if (!closed.add(current.pos())) continue;
            explored++;

            if (current.pos().equals(goalPos)) {
                return reconstruct(world, current);
            }

            for (BlockPos neighbour : neighbours(world, current.pos())) {
                if (closed.contains(neighbour)) continue;

                double tentativeG = current.gCost() + distance(current.pos(), neighbour);
                NodeRecord known = all.get(neighbour);
                if (known == null || tentativeG < known.gCost()) {
                    NodeRecord next = new NodeRecord(neighbour, current, tentativeG, heuristic(neighbour, goalPos));
                    all.put(neighbour, next);
                    open.add(next);
                }
            }
        }

        return List.of();
    }

    private List<Location> reconstruct(World world, NodeRecord current) {
        List<Location> path = new ArrayList<>();
        NodeRecord cursor = current;
        while (cursor != null) {
            path.add(cursor.pos().toLocation(world));
            cursor = cursor.parent();
        }
        Collections.reverse(path);
        return compress(path);
    }

    private List<Location> compress(List<Location> path) {
        if (path.size() <= 2) return path;
        List<Location> result = new ArrayList<>();
        result.add(path.getFirst());
        int lastDx = 0;
        int lastDy = 0;
        int lastDz = 0;

        for (int i = 1; i < path.size(); i++) {
            Location prev = path.get(i - 1);
            Location current = path.get(i);
            int dx = Integer.compare(current.getBlockX(), prev.getBlockX());
            int dy = Integer.compare(current.getBlockY(), prev.getBlockY());
            int dz = Integer.compare(current.getBlockZ(), prev.getBlockZ());
            if (i == 1) {
                lastDx = dx;
                lastDy = dy;
                lastDz = dz;
                result.add(current);
                continue;
            }
            if (dx != lastDx || dy != lastDy || dz != lastDz) {
                result.add(prev);
                lastDx = dx;
                lastDy = dy;
                lastDz = dz;
            }
            if (i == path.size() - 1) {
                result.add(current);
            }
        }
        return deduplicate(result);
    }

    private List<Location> deduplicate(List<Location> input) {
        List<Location> out = new ArrayList<>();
        Location last = null;
        for (Location loc : input) {
            if (last == null || !sameBlock(last, loc)) {
                out.add(loc);
                last = loc;
            }
        }
        return out;
    }

    private boolean sameBlock(Location a, Location b) {
        return a.getBlockX() == b.getBlockX()
                && a.getBlockY() == b.getBlockY()
                && a.getBlockZ() == b.getBlockZ();
    }

    private List<BlockPos> neighbours(World world, BlockPos pos) {
        List<BlockPos> result = new ArrayList<>(12);
        for (int[] dir : DIRECTIONS) {
            int nx = pos.x() + dir[0];
            int nz = pos.z() + dir[1];

            for (int dy = -settings.maxDropHeight(); dy <= settings.maxStepHeight(); dy++) {
                int ny = pos.y() + dy;
                BlockPos next = new BlockPos(nx, ny, nz);
                if (canStand(world, next)) {
                    result.add(next);
                }
            }
        }
        return result;
    }

    private boolean canStand(World world, BlockPos pos) {
        Block feet = world.getBlockAt(pos.x(), pos.y(), pos.z());
        Block head = world.getBlockAt(pos.x(), pos.y() + 1, pos.z());
        Block below = world.getBlockAt(pos.x(), pos.y() - 1, pos.z());

        if (!isFree(feet) || !isFree(head)) {
            return false;
        }
        if (!isSupport(below)) {
            return false;
        }
        return true;
    }

    private boolean isFree(Block block) {
        return block.isPassable() && !block.isLiquid();
    }

    private boolean isSupport(Block block) {
        return !block.isPassable() && !block.isLiquid();
    }

    private double heuristic(BlockPos a, BlockPos b) {
        double dx = a.x() - b.x();
        double dy = a.y() - b.y();
        double dz = a.z() - b.z();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private double distance(BlockPos a, BlockPos b) {
        double dx = a.x() - b.x();
        double dy = a.y() - b.y();
        double dz = a.z() - b.z();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private record NodeRecord(BlockPos pos, NodeRecord parent, double gCost, double hCost) {
        double fCost() {
            return gCost + hCost;
        }
    }
}
