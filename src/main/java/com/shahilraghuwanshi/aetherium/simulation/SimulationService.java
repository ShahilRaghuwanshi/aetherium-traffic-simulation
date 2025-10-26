package com.shahilraghuwanshi.aetherium.simulation;

import com.shahilraghuwanshi.aetherium.model.Intersection;
import com.shahilraghuwanshi.aetherium.model.Road;
import com.shahilraghuwanshi.aetherium.repository.IntersectionRepository;
import com.shahilraghuwanshi.aetherium.repository.RoadRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class SimulationService {

    private final IntersectionRepository intersectionRepository;
    private final RoadRepository roadRepository;
    private final SimulationWebSocketHandler webSocketHandler;

    private final List<Car> cars = new CopyOnWriteArrayList<>();
    private final Random random = new Random();
    private List<Intersection> allIntersections = new ArrayList<>();
    private Map<Long, List<Intersection>> adjacencyList = new HashMap<>();

    private ScheduledExecutorService scheduler;
    private static final int SIMULATION_TICK_RATE_MS = 33; // Approx 30 times per second
    private static final int MAX_CARS = 50; // Maximum number of cars allowed

    @Autowired
    public SimulationService(IntersectionRepository intersectionRepository,
                             RoadRepository roadRepository,
                             SimulationWebSocketHandler webSocketHandler) {
        this.intersectionRepository = intersectionRepository;
        this.roadRepository = roadRepository;
        this.webSocketHandler = webSocketHandler;
        loadMapData();
    }

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

    // --- A* Pathfinding (Keep as is) ---
    public List<Intersection> findShortestPath(Intersection start, Intersection end) {
        if (start == null || end == null || start.equals(end)) return Collections.emptyList();
        PriorityQueue<PathNode> openSet = new PriorityQueue<>();
        Set<Intersection> closedSet = new HashSet<>();
        Map<Intersection, PathNode> nodeMap = new HashMap<>();

        PathNode startNode = new PathNode(start);
        startNode.setGCost(0);
        startNode.setHCost(calculateHeuristic(start, end));
        startNode.setFCost(startNode.getHCost());
        nodeMap.put(start, startNode);
        openSet.add(startNode);

        while (!openSet.isEmpty()) {
            PathNode current = openSet.poll();
            if (current.getIntersection().equals(end)) return reconstructPath(current);
            closedSet.add(current.getIntersection());

            for (Intersection neighborIntersection : getNeighbors(current.getIntersection())) {
                if (closedSet.contains(neighborIntersection)) continue;
                PathNode neighborNode = nodeMap.computeIfAbsent(neighborIntersection, PathNode::new);
                double tentativeGCost = current.getGCost() + calculateDistance(current.getIntersection(), neighborIntersection);

                if (tentativeGCost < neighborNode.getGCost()) {
                    neighborNode.setParent(current);
                    neighborNode.setGCost(tentativeGCost);
                    neighborNode.setHCost(calculateHeuristic(neighborIntersection, end));
                    neighborNode.setFCost(neighborNode.getGCost() + neighborNode.getHCost());
                    if (!openSet.contains(neighborNode)) openSet.add(neighborNode);
                    else { openSet.remove(neighborNode); openSet.add(neighborNode); }
                }
            }
        }
        return Collections.emptyList();
    }
    private List<Intersection> getNeighbors(Intersection intersection) { return adjacencyList.getOrDefault(intersection.getId(), Collections.emptyList()); }
    private double calculateDistance(Intersection a, Intersection b) { double dx = a.getXCoordinate() - b.getXCoordinate(); double dy = a.getYCoordinate() - b.getYCoordinate(); return Math.sqrt(dx * dx + dy * dy); }
    private double calculateHeuristic(Intersection from, Intersection to) { return calculateDistance(from, to); }
    private List<Intersection> reconstructPath(PathNode endNode) { List<Intersection> path = new LinkedList<>(); PathNode current = endNode; while (current != null) { path.add(0, current.getIntersection()); current = current.getParent(); } return path; }
    // --- End A* ---


    public void spawnCar() {
        if (allIntersections.isEmpty() || allIntersections.size() < 2) {
            System.err.println("Cannot spawn car: Need at least two intersections.");
            return;
        }
        Intersection start = allIntersections.get(random.nextInt(allIntersections.size()));
        Intersection destination;
        do { destination = allIntersections.get(random.nextInt(allIntersections.size())); } while (destination.getId().equals(start.getId()));
        List<Intersection> path = findShortestPath(start, destination);
        if (path.isEmpty() || path.size() < 2) { System.err.println("Could not find a valid path for the car from " + start.getId() + " to " + destination.getId()); return; }
        Car newCar = new Car(start, path);
        cars.add(newCar);
        // System.out.println("Spawned Car ID: " + newCar.getId() + " at (" + start.getXCoordinate() + "," + start.getYCoordinate() + ") Path length: " + path.size()); // Optional detailed spawn log
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
        // Spawn new cars periodically if below the limit
        boolean spawnedNewCar = false;
        if (cars.size() < MAX_CARS && random.nextInt(100) < 5) { // Approx 5% chance each tick to spawn
            spawnCar();
            spawnedNewCar = true; // Mark that a car was potentially spawned
        }

        double speed = 2.0;
        boolean stateChanged = spawnedNewCar; // Start tracking if state changed (spawn counts)

        // Use iterator for safe removal
        Iterator<Car> iterator = cars.iterator();
        while (iterator.hasNext()) {
            Car car = iterator.next();

            if (car.hasReachedFinalDestination()) {
                // System.out.println("Car ID: " + car.getId() + " arrived at final destination."); // Optional arrival log
                iterator.remove();
                stateChanged = true;
                continue;
            }

            Intersection target = car.getCurrentTargetIntersection();
            if (target == null) {
                iterator.remove();
                stateChanged = true;
                continue;
            }

            double deltaX = target.getXCoordinate() - car.getX();
            double deltaY = target.getYCoordinate() - car.getY();
            double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);

            if (distance < speed) {
                car.setX(target.getXCoordinate());
                car.setY(target.getYCoordinate());
                car.advanceToNextTarget();
                // System.out.println("Car ID: " + car.getId() + " reached intersection " + target.getId()); // Optional intersection log
                stateChanged = true;
            } else {
                double normalizedX = deltaX / distance;
                double normalizedY = deltaY / distance;
                double moveX = normalizedX * speed;
                double moveY = normalizedY * speed;
                car.setX(car.getX() + moveX);
                car.setY(car.getY() + moveY);
                stateChanged = true; // Car position updated
            }
        }

        // --- TEMPORARY CHANGE FOR DEBUGGING ---
        // Always broadcast the current state on every tick, regardless of stateChanged
        webSocketHandler.broadcast(getCars());
        // --- END TEMPORARY CHANGE ---

    }
    // --- End Simulation Loop ---


    public List<Car> getCars() {
        // Return a copy for thread safety when broadcasting
        List<Car> carCopy = new ArrayList<>();
        for (Car car : cars) {
            // Create a simple copy (adjust if Car becomes more complex)
            if (car.getPath() == null || car.getPath().isEmpty()) continue; // Skip cars with no path (shouldn't happen)
            Car copy = new Car(car.getPath().get(0), new ArrayList<>(car.getPath())); // Recreate with path start
            copy.setId(car.getId());
            copy.setX(car.getX());
            copy.setY(car.getY());
            copy.setCurrentPathIndex(car.getCurrentPathIndex());
            carCopy.add(copy);
        }
        return carCopy;
    }
}