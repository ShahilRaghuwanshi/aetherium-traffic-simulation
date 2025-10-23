package com.shahilraghuwanshi.aetherium.simulation;

import com.shahilraghuwanshi.aetherium.model.Intersection;
import com.shahilraghuwanshi.aetherium.repository.IntersectionRepository;
import jakarta.annotation.PostConstruct; // Import for PostConstruct
import jakarta.annotation.PreDestroy;   // Import for PreDestroy
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;           // Import for Executors
import java.util.concurrent.ScheduledExecutorService; // Import for ScheduledExecutorService
import java.util.concurrent.TimeUnit;           // Import for TimeUnit

@Service
public class SimulationService {

    private final IntersectionRepository intersectionRepository;
    private final List<Car> cars = new CopyOnWriteArrayList<>(); // Thread-safe list for cars
    private final Random random = new Random();
    private List<Intersection> allIntersections = new ArrayList<>();

    // Simulation loop fields
    private ScheduledExecutorService scheduler;
    private static final int SIMULATION_TICK_RATE_MS = 33; // Approx 30 times per second

    // Constructor Injection
    public SimulationService(IntersectionRepository intersectionRepository) {
        this.intersectionRepository = intersectionRepository;
        loadIntersections(); // Load intersections when the service starts
    }

    // Load all intersections from the database into memory for quick access
    private void loadIntersections() {
        allIntersections = intersectionRepository.findAll();
        if (allIntersections.isEmpty()) {
            System.err.println("Warning: No intersections found in the database. Cannot spawn cars.");
        }
    }

    // Method to add a new car to the simulation
    public void spawnCar() {
        if (allIntersections.isEmpty()) {
            System.err.println("Cannot spawn car: No intersections loaded.");
            return;
        }

        // Pick a random start intersection
        Intersection start = allIntersections.get(random.nextInt(allIntersections.size()));

        // Pick a random destination intersection (different from start)
        Intersection destination;
        do {
            destination = allIntersections.get(random.nextInt(allIntersections.size()));
        } while (allIntersections.size() > 1 && destination.getId().equals(start.getId())); // Ensure destination is different if possible

        // Create and add the car
        Car newCar = new Car(start);
        newCar.setDestination(destination);
        cars.add(newCar);

        System.out.println("Spawned Car ID: " + newCar.getId() + " at (" + start.getXCoordinate() + "," + start.getYCoordinate() + ") heading to (" + destination.getXCoordinate() + "," + destination.getYCoordinate() + ")");
    }

    // --- Simulation Loop Logic ---

    @PostConstruct // Runs after the service is created
    public void startSimulationLoop() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        // Schedule updateSimulation to run repeatedly
        scheduler.scheduleAtFixedRate(this::updateSimulation, 0, SIMULATION_TICK_RATE_MS, TimeUnit.MILLISECONDS);
        System.out.println("Simulation loop started.");
    }

    @PreDestroy // Runs before the service is destroyed
    public void stopSimulationLoop() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            System.out.println("Simulation loop stopped.");
        }
    }

    // This is the main method that will be called repeatedly by the scheduler
    private void updateSimulation() {
        // Define a simple speed for the cars (pixels per tick)
        double speed = 2.0;

        // Loop through each car in our list
        for (Car car : cars) {
            Intersection destination = car.getDestination();
            if (destination == null) continue; // Skip if car has no destination

            // Calculate the difference in x and y coordinates
            double deltaX = destination.getXCoordinate() - car.getX();
            double deltaY = destination.getYCoordinate() - car.getY();

            // Calculate the distance to the destination
            double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);

            // Check if the car is very close to the destination
            if (distance < speed) {
                // If close enough, just snap to the destination
                car.setX(destination.getXCoordinate());
                car.setY(destination.getYCoordinate());
                // Optional: We could remove the car or give it a new destination here later
                System.out.println("Car ID: " + car.getId() + " arrived at destination (" + destination.getXCoordinate() + "," + destination.getYCoordinate() + ").");
                // For now, let's just remove the car when it arrives
                cars.remove(car);
            } else {
                // Calculate the normalized direction vector (unit vector)
                double normalizedX = deltaX / distance;
                double normalizedY = deltaY / distance;

                // Calculate the movement for this tick
                double moveX = normalizedX * speed;
                double moveY = normalizedY * speed;

                // Update the car's position
                car.setX(car.getX() + moveX);
                car.setY(car.getY() + moveY);

                // Print the updated position (Uncomment for detailed debugging)
                // System.out.println("Car ID: " + car.getId() + " moved to (" + String.format("%.2f", car.getX()) + "," + String.format("%.2f", car.getY()) + ")");
            }
        }
    }

    // Method to get the current list of cars (for sending to frontend later)
    public List<Car> getCars() {
        // Return a copy to prevent modification outside the service
        return new ArrayList<>(cars);
    }
}