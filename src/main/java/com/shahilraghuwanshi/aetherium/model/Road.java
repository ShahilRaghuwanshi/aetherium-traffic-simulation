package com.shahilraghuwanshi.aetherium.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "roads")
@Data
public class Road {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "start_intersection_id")
    private Intersection startIntersection;

    @ManyToOne
    @JoinColumn(name = "end_intersection_id")
    private Intersection endIntersection;
}