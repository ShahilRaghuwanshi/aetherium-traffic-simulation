package com.shahilraghuwanshi.aetherium.repository;

import com.shahilraghuwanshi.aetherium.model.TrafficLight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TrafficLightRepository extends JpaRepository<TrafficLight, Long> {
}