package com.shahilraghuwanshi.aetherium.dto;

import com.shahilraghuwanshi.aetherium.model.Intersection;
import com.shahilraghuwanshi.aetherium.model.Road;
import com.shahilraghuwanshi.aetherium.model.TrafficLight;
import lombok.Data;
<<<<<<< HEAD
=======

>>>>>>> main
import java.util.List;

@Data
public class MapLayoutDto {
    private List<Intersection> intersections;
    private List<Road> roads;
    private List<TrafficLight> trafficLights;
}