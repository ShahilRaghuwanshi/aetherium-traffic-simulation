package com.shahilraghuwanshi.aetherium.simulation;

import com.shahilraghuwanshi.aetherium.model.Intersection;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data // Lombok for getters, setters, etc.
@EqualsAndHashCode(of = "intersection") // Important for comparing nodes based on intersection only
public class PathNode implements Comparable<PathNode> {

    private Intersection intersection;
    private PathNode parent; // The node we came from to reach this node
    private double gCost;    // Cost from the start node to this node
    private double hCost;    // Heuristic cost (estimated cost from this node to the end)
    private double fCost;    // Total cost (gCost + hCost)

    public PathNode(Intersection intersection) {
        this.intersection = intersection;
        this.gCost = Double.MAX_VALUE; // Initialize with infinity
        this.hCost = 0;
        this.fCost = Double.MAX_VALUE;
    }

    // A* uses this to prioritize nodes with lower fCost in the priority queue
    @Override
    public int compareTo(PathNode other) {
        return Double.compare(this.fCost, other.fCost);
    }
}