package com.example;

import java.util.List;

public interface PathfindingNode {
    double getX();
    double getY();
    double getZ();
    List<PathfindingNode> getAdjacentNodes();

    double getCost();

    boolean equals(Object o);
}
