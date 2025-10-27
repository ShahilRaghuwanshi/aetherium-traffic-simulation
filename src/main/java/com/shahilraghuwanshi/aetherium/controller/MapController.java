package com.shahilraghuwanshi.aetherium.controller;

import com.shahilraghuwanshi.aetherium.dto.MapLayoutDto;
import com.shahilraghuwanshi.aetherium.service.MapService;
import org.springframework.web.bind.annotation.CrossOrigin; // Import CrossOrigin
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "*") // Keep this line for CORS
@RestController
@RequestMapping("/api/map")
public class MapController {

    private final MapService mapService;

    // Constructor Injection
    public MapController(MapService mapService) {
        this.mapService = mapService;
    }

    @GetMapping("/layout")
    public MapLayoutDto getMapLayout() {
        return mapService.getMapLayout();
    }
}