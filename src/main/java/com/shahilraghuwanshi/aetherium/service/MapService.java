package com.shahilraghuwanshi.aetherium.service;

import com.shahilraghuwanshi.aetherium.dto.MapLayoutDto;
import com.shahilraghuwanshi.aetherium.repository.IntersectionRepository;
import com.shahilraghuwanshi.aetherium.repository.RoadRepository;
import com.shahilraghuwanshi.aetherium.repository.TrafficLightRepository;
import org.springframework.stereotype.Service;

@Service
public class MapService {

    private final IntersectionRepository intersectionRepository;
    private final RoadRepository roadRepository;
    private final TrafficLightRepository trafficLightRepository;

    // Constructor Injection
    public MapService(IntersectionRepository intersectionRepository,
                      RoadRepository roadRepository,
                      TrafficLightRepository trafficLightRepository) {
        this.intersectionRepository = intersectionRepository;
        this.roadRepository = roadRepository;
        this.trafficLightRepository = trafficLightRepository;
    }

    public MapLayoutDto getMapLayout() {
        MapLayoutDto mapLayout = new MapLayoutDto();
        mapLayout.setIntersections(intersectionRepository.findAll());
        mapLayout.setRoads(roadRepository.findAll());
        mapLayout.setTrafficLights(trafficLightRepository.findAll());
        return mapLayout;
    }
}