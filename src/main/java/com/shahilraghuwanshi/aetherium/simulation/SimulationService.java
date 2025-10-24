package com.shahilraghuwanshi.aetherium.simulation;

import com.shahilraghuwanshi.aetherium.model.Intersection;
import com.shahilraghuwanshi.aetherium.model.Road; // Import Road
import com.shahilraghuwanshi.aetherium.repository.IntersectionRepository;
import com.shahilraghuwanshi.aetherium.repository.RoadRepository; // Import RoadRepository
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.util.*; // Import necessary util classes
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class SimulationService {

    private final IntersectionRepository intersectionRepository;
    private final RoadRepository roadRepository; // Add RoadRepository
    private final List<Car> cars = new CopyOnWriteArrayList<>();
    private final Random random = new Random();
    private List<Intersection> allIntersections = new ArrayList<>();
    private Map<Long, List<Intersection>> adjacencyList = new HashMap<>(); // Store road network

    private ScheduledExecutorService scheduler;
    private static final int SIMULATION_TICK_RATE_MS = 33; // Approx 30 times per second

    // Constructor Injection (add RoadRepository)
    public SimulationService(IntersectionRepository intersectionRepository, RoadRepository roadRepository) {
        this.intersectionRepository = intersectionRepository;
        this.roadRepository = roadRepository; // Initialize RoadRepository
        loadMapData(); // Load intersections and build adjacency list
    }

    // Load map data and build the adjacency list for pathfinding
    private void loadMapData() {
        allIntersections = intersectionRepository.findAll();
        List<Road> allRoads = roadRepository.findAll();

        if (allIntersections.isEmpty()) {
            System.err.println("Warning: No intersections found. Cannot build road network.");
            return;
        }
        if (allRoads.isEmpty()) {
            System.err.println("Warning: No roads found. Cannot build road network.");
        }

        // Initialize adjacency list
        for (Intersection intersection : allIntersections) {
            adjacencyList.put(intersection.getId(), new ArrayList<>());
        }

        // Populate adjacency list based on roads (bidirectional)
        for (Road road : allRoads) {
            if (road.getStartIntersection() != null && road.getEndIntersection() != null) {
                adjacencyList.get(road.getStartIntersection().getId()).add(road.getEndIntersection());
                adjacencyList.get(road.getEndIntersection().getId()).add(road.getStartIntersection());
            }
        }
        System.out.println("Road network adjacency list built.");
    }

    // --- A* Pathfinding Implementation ---

    /**
     * Finds the shortest path between two intersections using the A* algorithm.
     * @param start The starting intersection.
     * @param end The destination intersection.
     * @return A list of intersections representing the path, or an empty list if no path is found.
     */
    public List<Intersection> findShortestPath(Intersection start, Intersection end) {
        if (start == null || end == null || start.equals(end)) {
            return Collections.emptyList();
        }

        // Priority queue stores nodes to visit, ordered by fCost
        PriorityQueue<PathNode> openSet = new PriorityQueue<>();
        // Set stores nodes already visited
        Set<Intersection> closedSet = new HashSet<>();
        // Map stores the PathNode for each Intersection for quick lookup
        Map<Intersection, PathNode> nodeMap = new HashMap<>();

        // Initialize start node
        PathNode startNode = new PathNode(start);
        startNode.setGCost(0);
        startNode.setHCost(calculateHeuristic(start, end));
        startNode.setFCost(startNode.getHCost());
        nodeMap.put(start, startNode);
        openSet.add(startNode);

        while (!openSet.isEmpty()) {
            PathNode current = openSet.poll(); // Get node with lowest fCost

            // Path found! Reconstruct and return it.
            if (current.getIntersection().equals(end)) {
                return reconstructPath(current);
            }

            closedSet.add(current.getIntersection());

            // Explore neighbors
            for (Intersection neighborIntersection : getNeighbors(current.getIntersection())) {
                if (closedSet.contains(neighborIntersection)) {
                    continue; // Skip already visited neighbors
                }

                PathNode neighborNode = nodeMap.computeIfAbsent(neighborIntersection, PathNode::new);

                double tentativeGCost = current.getGCost() + calculateDistance(current.getIntersection(), neighborIntersection);

                // If this path to the neighbor is better than any previous path
                if (tentativeGCost < neighborNode.getGCost()) {
                    neighborNode.setParent(current);
                    neighborNode.setGCost(tentativeGCost);
                    neighborNode.setHCost(calculateHeuristic(neighborIntersection, end));
                    neighborNode.setFCost(neighborNode.getGCost() + neighborNode.getHCost());

                    // Add/update neighbor in the open set
                    if (!openSet.contains(neighborNode)) {
                        openSet.add(neighborNode);
                    } else {
                        // Re-add to update priority if cost changed (simple way, not most efficient)
                        openSet.remove(neighborNode);
                        openSet.add(neighborNode);
                    }
                }
            }
        }

        return Collections.emptyList(); // No path found
    }

    // Helper: Get adjacent intersections based on the road network
    private List<Intersection> getNeighbors(Intersection intersection) {
        return adjacencyList.getOrDefault(intersection.getId(), Collections.emptyList());
    }

    // Helper: Calculate Euclidean distance (straight-line) between two intersections
    private double calculateDistance(Intersection a, Intersection b) {
        double dx = a.getXCoordinate() - b.getXCoordinate();
        double dy = a.getYCoordinate() - b.getYCoordinate();
        return Math.sqrt(dx * dx + dy * dy);
    }

    // Helper: Calculate heuristic cost (Euclidean distance to the end)
    private double calculateHeuristic(Intersection from, Intersection to) {
        return calculateDistance(from, to);
    }

    // Helper: Reconstruct the path from the end node back to the start
    private List<Intersection> reconstructPath(PathNode endNode) {
        List<Intersection> path = new LinkedList<>();
        PathNode current = endNode;
        while (current != null) {
            path.add(0, current.getIntersection()); // Add to the beginning to reverse the path
            current = current.getParent();
        }
        return path;
    }


    // --- Spawn Logic (Updated) ---

    public void spawnCar() {
        if (allIntersections.isEmpty() || allIntersections.size() < 2) {
            System.err.println("Cannot spawn car: Need at least two intersections.");
            return;
        }

        Intersection start = allIntersections.get(random.nextInt(allIntersections.size()));
        Intersection destination;
        do {
            destination = allIntersections.get(random.nextInt(allIntersections.size()));
        } while (destination.getId().equals(start.getId()));

        // *** Calculate the path using A* ***
        List<Intersection> path = findShortestPath(start, destination);

        if (path.isEmpty() || path.size() < 2) {
            System.err.println("Could not find a valid path for the car from " + start.getId() + " to " + destination.getId());
            return; // Don't spawn if no path
        }

        // *** Create car WITH the path ***
        Car newCar = new Car(start, path);
        cars.add(newCar);

        System.out.println("Spawned Car ID: " + newCar.getId() + " at (" + start.getXCoordinate() + "," + start.getYCoordinate() + ") Path length: " + path.size());
    }


    // --- Simulation Loop Logic (Updated) ---

    @PostConstruct
    public void startSimulationLoop() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::updateSimulation, 0, SIMULATION_TICK_RATE_MS, TimeUnit.MILLISECONDS);
        System.out.println("Simulation loop started.");
    }

    @PreDestroy
    public void stopSimulationLoop() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            System.out.println("Simulation loop stopped.");
        }
    }

    private void updateSimulation() {
        double speed = 2.0; // Pixels per tick

        for (Car car : cars) {
            if (car.hasReachedFinalDestination()) {
                System.out.println("Car ID: " + car.getId() + " arrived at final destination.");
                cars.remove(car); // Remove car when it finishes its path
                continue;
            }

            // *** Get the NEXT intersection in the path ***
            Intersection target = car.getCurrentTargetIntersection();
            if (target == null) { // Should not happen if path is valid
                cars.remove(car);
                continue;
            }

            double deltaX = target.getXCoordinate() - car.getX();
            double deltaY = target.getYCoordinate() - car.getY();
            double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);

            if (distance < speed) {
                // Arrived at the current target intersection
                car.setX(target.getXCoordinate());
                car.setY(target.getYCoordinate());
                // *** Advance to the NEXT intersection in the path ***
                car.advanceToNextTarget();
                System.out.println("Car ID: " + car.getId() + " reached intersection " + target.getId());
            } else {
                // Move towards the current target intersection
                double normalizedX = deltaX / distance;
                double normalizedY = deltaY / distance;
                double moveX = normalizedX * speed;
                double moveY = normalizedY * speed;

                car.setX(car.getX() + moveX);
                car.setY(car.getY() + moveY);
            }
        }
    }


    // Method to get the current list of cars (for sending to frontend later)
    public List<Car> getCars() {
        return new ArrayList<>(cars); // Return a copy
    }
}