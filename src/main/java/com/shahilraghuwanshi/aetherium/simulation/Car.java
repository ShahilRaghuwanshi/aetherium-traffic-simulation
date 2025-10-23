package com.shahilraghuwanshi.aetherium.simulation;

import com.shahilraghuwanshi.aetherium.model.Intersection;
import lombok.Data;

@Data // From Lombok, for getters/setters
public class Car {

    private long id;
    private double x;
    private double y;
    private Intersection destination;

    private static long idCounter = 0;

    public Car(Intersection start) {
        this.id = idCounter++;
        this.x = start.getXCoordinate();
        this.y = start.getYCoordinate();
    }
}