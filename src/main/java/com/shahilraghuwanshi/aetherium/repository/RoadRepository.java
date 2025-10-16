package com.shahilraghuwanshi.aetherium.repository;

import com.shahilraghuwanshi.aetherium.model.Road;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoadRepository extends JpaRepository<Road, Long> {
}