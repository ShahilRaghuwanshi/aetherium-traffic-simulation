package com.shahilraghuwanshi.aetherium.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "traffic_lights")
@Data
public class TrafficLight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "intersection_id")
    private Intersection intersection;

    private String currentState;
}