package com.example;

import java.util.List;

public interface PathfindingNode {
    double getX();
    double getY();
    double getZ();
    List<PathfindingNode> getAdjacentNodes();

    boolean equals(Object o);
}
