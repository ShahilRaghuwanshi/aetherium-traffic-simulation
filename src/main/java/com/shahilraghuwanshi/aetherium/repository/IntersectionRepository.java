package com.shahilraghuwanshi.aetherium.repository;

import com.shahilraghuwanshi.aetherium.model.Intersection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IntersectionRepository extends JpaRepository<Intersection, Long> {
}