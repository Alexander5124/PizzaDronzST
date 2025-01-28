package uk.ac.ed.inf;

import uk.ac.ed.inf.ilp.data.LngLat;
import uk.ac.ed.inf.ilp.data.NamedRegion;

import java.util.*;

/**
 * The {@code SimpleRouteCalculator} class calculates the shortest path between two points,
 * avoiding no-fly zones, and optionally disallowing departure from the central area once entered
 * (for "return path" scenarios).
 */
public class RouteCalculator {
    private final LngLatHandler handler;
    private final NamedRegion[] noFlyRegions;
    private final NamedRegion centralArea;

    // Caches for outward vs. return paths
    private static final Map<Pair<LngLat, LngLat>, List<LngLat>> OUTWARD_PATH_CACHE = new HashMap<>();
    private static final Map<Pair<LngLat, LngLat>, List<LngLat>> RETURN_PATH_CACHE  = new HashMap<>();

    // The 16 compass angles in increments of 22.5
    private static final double[] COMPASS_ANGLES = {
            0.0, 22.5, 45.0, 67.5,
            90.0, 112.5, 135.0, 157.5,
            180.0, 202.5, 225.0, 247.5,
            270.0, 292.5, 315.0, 337.5
    };

    /**
     * Constructs the route calculator with the given data sources.
     *
     * @param baseUrl Base URL for fetching no-fly zones and central area data.
     */
    public RouteCalculator(String baseUrl) {
        this.handler = new LngLatHandler();

        RestServiceClient client   = new RestServiceClient(baseUrl);
        this.noFlyRegions          = client.getNoFlyZones();

        // In some course specs, 'centralArea' might already be a NamedRegion directly.
        NamedRegion tempCentral    = client.getCentralArea();
        this.centralArea           = new NamedRegion("central", tempCentral.vertices());
    }

    /**
     * Constructs the route calculator directly with arrays of no-fly zones and a central area.
     * This is helpful in tests where you want to inject your own data.
     */
    public RouteCalculator(NamedRegion[] noFlyRegions, NamedRegion centralArea) {
        this.handler      = new LngLatHandler();
        this.noFlyRegions = noFlyRegions;
        this.centralArea  = centralArea;
    }

    /**
     * Finds a path from {@code startLngLat} to {@code endLngLat}.
     *
     * @param startLngLat   The starting coordinate.
     * @param endLngLat     The destination coordinate.
     * @param isReturnPath  If {@code true}, once the path has entered the central area,
     *                      it must not leave again.
     * @return The sequence of coordinates from start to end (including both),
     *         or an empty list if no path is found.
     */
    public List<LngLat> findPath(LngLat startLngLat, LngLat endLngLat, boolean isReturnPath) {
        // Decide which cache we will use
        Map<Pair<LngLat, LngLat>, List<LngLat>> pathCache =
                isReturnPath ? RETURN_PATH_CACHE : OUTWARD_PATH_CACHE;

        Pair<LngLat, LngLat> cacheKey = new Pair<>(startLngLat, endLngLat);
        if (pathCache.containsKey(cacheKey)) {
            // Return a copy of the cached path to avoid external modifications
            return new ArrayList<>(pathCache.get(cacheKey));
        }

        // Compute a new path using A*
        List<LngLat> path = calculatePath(startLngLat, endLngLat, isReturnPath);

        // Store in cache
        pathCache.put(cacheKey, new ArrayList<>(path));
        return path;
    }

    /**
     * Clears the cached paths for both outward and return paths.
     * Useful if underlying geometry changes or for certain test setups.
     */
    public void resetState() {
        OUTWARD_PATH_CACHE.clear();
        RETURN_PATH_CACHE.clear();
    }

    // ------------------------------------------------------------------
    // Internal A* Implementation
    // ------------------------------------------------------------------

    private List<LngLat> calculatePath(LngLat start, LngLat end, boolean isReturnPath) {
        // Priority Queue for A*, sorting by f(n) = g(n) + h(n)
        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.gScore + n.hScore));

        // We will store a single Node per unique coordinate
        Map<LngLat, Node> allNodes   = new HashMap<>();
        Set<Node> visited           = new HashSet<>();
        Map<LngLat, Double> gScores = new HashMap<>();

        // Create start node
        Node startNode     = new Node(start);
        startNode.gScore   = 0.0;
        startNode.hScore   = handler.distanceTo(start, end);

        openSet.add(startNode);
        allNodes.put(start, startNode);
        gScores.put(start, 0.0);

        // If isReturnPath == true, we only become "locked" once we physically enter the central area
        boolean hasEnteredCentralArea = isReturnPath && handler.isInCentralArea(start, centralArea);

        while (!openSet.isEmpty()) {
            Node current = openSet.poll();
            if (visited.contains(current)) {
                continue;
            }
            visited.add(current);

            // If we are "close enough" to the target:
            if (handler.isCloseTo(current.coordinate, end)) {
                return reconstructPath(current);
            }

            // Expand neighbors (16 compass moves)
            for (LngLat neighborPos : generateNeighborPositions(current.coordinate)) {
                if (!isValidMove(current.coordinate, neighborPos, hasEnteredCentralArea)) {
                    continue;
                }

                Node neighbor = allNodes.computeIfAbsent(neighborPos, Node::new);

                // If already visited, skip
                if (visited.contains(neighbor)) {
                    continue;
                }

                // If we haven't entered central area yet, check whether this move enters it
                boolean neighborInCentral = handler.isInCentralArea(neighborPos, centralArea);
                boolean nextHasEntered    = hasEnteredCentralArea || neighborInCentral;

                double tentativeGScore = current.gScore + handler.distanceTo(current.coordinate, neighborPos);
                if (tentativeGScore < gScores.getOrDefault(neighborPos, Double.POSITIVE_INFINITY)) {
                    neighbor.parent = current;
                    neighbor.gScore = tentativeGScore;
                    neighbor.hScore = handler.distanceTo(neighborPos, end);
                    neighbor.hasEnteredCentral = nextHasEntered; // track for neighbor if needed

                    gScores.put(neighborPos, tentativeGScore);
                    openSet.add(neighbor);
                }
            }
        }
        // No path found => return empty
        return Collections.emptyList();
    }

    private List<LngLat> generateNeighborPositions(LngLat current) {
        List<LngLat> neighbors = new ArrayList<>();
        for (double angle : COMPASS_ANGLES) {
            neighbors.add(handler.nextPosition(current, angle));
        }
        return neighbors;
    }

    /**
     * Checks whether a move from {@code current} to {@code next} is valid:
     *  - Must not be inside any no-fly region.
     *  - Must not intersect any no-fly zone boundary.
     *  - If we have already entered the central area (hasEnteredCentralArea==true),
     *    we cannot leave it again.
     */
    private boolean isValidMove(LngLat current, LngLat next, boolean hasEnteredCentralArea) {
        // If we are "locked" in central area, disallow moves that exit the central area
        if (hasEnteredCentralArea && !handler.isInCentralArea(next, centralArea)) {
            return false;
        }
        // Cannot be inside a no-fly region
        if (isInNoFlyRegion(next)) {
            return false;
        }
        // Cannot cross any no-fly boundary
        if (intersectsNoFlyZone(current, next)) {
            return false;
        }
        return true;
    }

    private boolean intersectsNoFlyZone(LngLat p1, LngLat p2) {
        for (NamedRegion region : noFlyRegions) {
            if (handler.doLineSegmentsIntersect(p1, p2, region)) {
                return true;
            }
        }
        return false;
    }

    private boolean isInNoFlyRegion(LngLat position) {
        for (NamedRegion region : noFlyRegions) {
            if (handler.isInRegion(position, region)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Reconstructs the path from the final node back to the start.
     */
    private List<LngLat> reconstructPath(Node finalNode) {
        LinkedList<LngLat> path = new LinkedList<>();
        Node current = finalNode;
        while (current != null) {
            path.addFirst(current.coordinate);
            current = current.parent;
        }
        return path;
    }

    // ------------------------------------------------------------------
    // Internal Node class for A*
    // ------------------------------------------------------------------
    private static class Node {
        final LngLat coordinate;
        Node parent;

        double gScore = Double.POSITIVE_INFINITY;
        double hScore = Double.POSITIVE_INFINITY;

        // Whether this node's path has entered the central area
        boolean hasEnteredCentral = false;

        Node(LngLat coordinate) {
            this.coordinate = coordinate;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Node)) return false;
            Node node = (Node) o;
            return Objects.equals(coordinate, node.coordinate);
        }

        @Override
        public int hashCode() {
            return Objects.hash(coordinate);
        }
    }
}
