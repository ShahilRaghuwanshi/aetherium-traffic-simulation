package com.shahilraghuwanshi.aetherium.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "intersections")
@Data
public class Intersection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int xCoordinate;
    private int yCoordinate;
    private boolean hasTrafficLight;
}