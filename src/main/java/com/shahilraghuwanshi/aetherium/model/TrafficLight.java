package com.shahilraghuwanshi.aetherium.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

@Entity
@Table(name = "traffic_lights")
@Data
public class TrafficLight {

    public enum State {
        NS_GREEN, // North-South Green, East-West Red
        EW_GREEN  // East-West Green, North-South Red
        // We could add yellow states later if needed
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "intersection_id")
    @ToString.Exclude // Avoid infinite loop in toString with Intersection
    private Intersection intersection;

    @Enumerated(EnumType.STRING) // Store the state as a string in the DB
    private State currentState;

    private int stateDurationSeconds = 20; // How long each state (NS_GREEN, EW_GREEN) lasts

    @Transient // This field is not stored in the database
    private double timeInCurrentState = 0; // Tracks time elapsed in the current state (in seconds)

    // Default constructor for JPA
    public TrafficLight() {
        this.currentState = State.NS_GREEN; // Default state
    }

    // Constructor for associating with an intersection
    public TrafficLight(Intersection intersection) {
        this(); // Call default constructor
        this.intersection = intersection;
    }

    /**
     * Updates the traffic light's timer and potentially changes its state.
     * @param deltaTime The time elapsed since the last update (in seconds).
     * @return true if the state changed, false otherwise.
     */
    public boolean updateState(double deltaTime) {
        timeInCurrentState += deltaTime;
        if (timeInCurrentState >= stateDurationSeconds) {
            // Switch state
            if (currentState == State.NS_GREEN) {
                currentState = State.EW_GREEN;
            } else {
                currentState = State.NS_GREEN;
            }
            timeInCurrentState = 0; // Reset timer
            System.out.println("Traffic Light ID: " + id + " at Intersection ID: " + (intersection != null ? intersection.getId() : "null") + " changed state to: " + currentState);
            return true; // State changed
        }
        return false; // State did not change
    }
}