package kr.antos112.addnavigation.navigation;

import kr.antos112.addnavigation.model.NavigationSettings;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.*;

/**
 * 현재 월드 내에서 이동 가능한 경로를 찾는 A* 경로 탐색 알고리즘.
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
     * 현재 탐색 설정으로 경로 탐색기를 생성합니다.
     *
     * @param settings runtime navigation settings
     */
    public PathFinder(NavigationSettings settings) {
        this.settings = settings;
    }

    /**
     * A* 탐색을 사용하여 시작점에서 목표점까지의 경로를 찾습니다.
     *
     * @param start 시작 위치
     * @param goal 목표 위치
     * @return block-by-block path, or an empty list when no route exists
     */
    public List<Location> findPath(Location start, Location goal) {
        if (start == null || goal == null) return List.of();
        if (start.getWorld() == null || goal.getWorld() == null) return List.of();
        if (!start.getWorld().equals(goal.getWorld())) return List.of();

        World world = start.getWorld();
        BlockPos rawStartPos = new BlockPos(start.getBlockX(), start.getBlockY(), start.getBlockZ());
        BlockPos rawGoalPos = new BlockPos(goal.getBlockX(), goal.getBlockY(), goal.getBlockZ());
        Map<BlockPos, Boolean> standCache = new HashMap<>(4096);
        Optional<BlockPos> optionalStart = findNearestWalkable(
                world,
                rawStartPos,
                settings.startSearchRadius(),
                settings.verticalSearchRange(),
                standCache
        );
        List<BlockPos> goalCandidates = findWalkableGoalCandidates(world, rawGoalPos, standCache);

        if (optionalStart.isEmpty() || goalCandidates.isEmpty()) {
            return List.of();
        }

        BlockPos startPos = optionalStart.get();
        Set<BlockPos> goalSet = new HashSet<>(goalCandidates);

        PriorityQueue<NodeRecord> open = new PriorityQueue<>(Comparator
                .comparingDouble(NodeRecord::fCost)
                .thenComparingDouble(NodeRecord::hCost));

        Map<BlockPos, NodeRecord> all = new HashMap<>();
        Set<BlockPos> closed = new HashSet<>();

        NodeRecord startRecord = new NodeRecord(startPos, null, 0.0, heuristic(startPos, rawGoalPos));
        open.add(startRecord);
        all.put(startPos, startRecord);

        int explored = 0;
        while (!open.isEmpty() && explored < settings.maxSearchNodes()) {
            NodeRecord current = open.poll();
            if (!closed.add(current.pos())) continue;
            explored++;

            if (goalSet.contains(current.pos())) {
                return reconstruct(world, current);
            }

            for (BlockPos neighbour : neighbours(world, current.pos(), standCache)) {
                if (closed.contains(neighbour)) continue;

                double tentativeG = current.gCost() + distance(current.pos(), neighbour);
                NodeRecord known = all.get(neighbour);
                if (known == null || tentativeG < known.gCost()) {
                    NodeRecord next = new NodeRecord(neighbour, current, tentativeG, heuristic(neighbour, rawGoalPos));
                    all.put(neighbour, next);
                    open.add(next);
                }
            }
        }

        return List.of();
    }

    private Optional<BlockPos> findNearestWalkable(
            World world,
            BlockPos origin,
            int radius,
            int verticalRange,
            Map<BlockPos, Boolean> standCache
    ) {
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        int safeRadius = Math.max(0, radius);
        int safeVerticalRange = Math.max(0, verticalRange);

        for (int dy = -safeVerticalRange; dy <= safeVerticalRange; dy++) {
            for (int dx = -safeRadius; dx <= safeRadius; dx++) {
                for (int dz = -safeRadius; dz <= safeRadius; dz++) {
                    if (dx * dx + dz * dz > safeRadius * safeRadius) {
                        continue;
                    }

                    BlockPos candidate = new BlockPos(origin.x() + dx, origin.y() + dy, origin.z() + dz);
                    if (!canStand(world, candidate, standCache)) {
                        continue;
                    }

                    double distance = dx * dx + dz * dz + Math.abs(dy) * 0.25;
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = candidate;
                    }
                }
            }
        }

        return Optional.ofNullable(best);
    }

    private List<BlockPos> findWalkableGoalCandidates(World world, BlockPos origin, Map<BlockPos, Boolean> standCache) {
        List<BlockPos> candidates = new ArrayList<>();
        int radius = Math.max(0, settings.goalSearchRadius());
        int verticalRange = Math.max(0, settings.verticalSearchRange());

        for (int dy = -verticalRange; dy <= verticalRange; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dz * dz > radius * radius) {
                        continue;
                    }

                    BlockPos candidate = new BlockPos(origin.x() + dx, origin.y() + dy, origin.z() + dz);
                    if (canStand(world, candidate, standCache)) {
                        candidates.add(candidate);
                    }
                }
            }
        }

        candidates.sort(Comparator
                .comparingDouble((BlockPos pos) -> distance(pos, origin))
                .thenComparingInt(BlockPos::y)
                .thenComparingInt(BlockPos::x)
                .thenComparingInt(BlockPos::z));
        return candidates;
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

    private List<BlockPos> neighbours(World world, BlockPos pos, Map<BlockPos, Boolean> standCache) {
        List<BlockPos> result = new ArrayList<>(12);
        for (int[] dir : DIRECTIONS) {
            int nx = pos.x() + dir[0];
            int nz = pos.z() + dir[1];

            for (int dy = -settings.maxDropHeight(); dy <= settings.maxStepHeight(); dy++) {
                int ny = pos.y() + dy;
                BlockPos next = new BlockPos(nx, ny, nz);
                if (canStand(world, next, standCache)) {
                    result.add(next);
                }
            }
        }
        return result;
    }

    private boolean canStand(World world, BlockPos pos, Map<BlockPos, Boolean> standCache) {
        Boolean cached = standCache.get(pos);
        if (cached != null) {
            return cached;
        }

        Block feet = world.getBlockAt(pos.x(), pos.y(), pos.z());
        Block head = world.getBlockAt(pos.x(), pos.y() + 1, pos.z());
        Block below = world.getBlockAt(pos.x(), pos.y() - 1, pos.z());

        if (!isFree(feet) || !isFree(head)) {
            standCache.put(pos, false);
            return false;
        }
        if (!isSupport(below)) {
            standCache.put(pos, false);
            return false;
        }
        standCache.put(pos, true);
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
