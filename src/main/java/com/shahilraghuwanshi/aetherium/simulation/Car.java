package com.shahilraghuwanshi.aetherium.simulation;

import com.shahilraghuwanshi.aetherium.model.Intersection;
import lombok.Data;

import java.util.List; // Make sure to import java.util.List

@Data // From Lombok, for getters/setters
public class Car {

    private long id;
    private double x;
    private double y;

    // The complete path from start to finish
    private List<Intersection> path;

    // The index of the *next* intersection in the path we are moving towards
    private int currentPathIndex;

    private static long idCounter = 0;

    public Car(Intersection start, List<Intersection> path) {
        this.id = idCounter++;
        this.x = start.getXCoordinate();
        this.y = start.getYCoordinate();
        this.path = path;
        this.currentPathIndex = 1; // Start by moving towards the second intersection in the path (index 1)
    }

    /**
     * Gets the *final* destination of the car's journey.
     */
    public Intersection getDestination() {
        if (path == null || path.isEmpty()) {
            return null;
        }
        return path.get(path.size() - 1);
    }

    /**
     * Gets the *next* intersection the car is currently moving towards.
     */
    public Intersection getCurrentTargetIntersection() {
        if (path == null || currentPathIndex >= path.size()) {
            return null; // No target or path is finished
        }
        return path.get(currentPathIndex);
    }

    /**
     * Call this when the car reaches its current target intersection
     * to make it target the *next* one in the path.
     */
    public void advanceToNextTarget() {
        currentPathIndex++;
    }

    /**
     * Checks if the car has reached the end of its path.
     */
    public boolean hasReachedFinalDestination() {
        return path != null && currentPathIndex >= path.size();
    }
}