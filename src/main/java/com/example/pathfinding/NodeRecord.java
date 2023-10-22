package com.example.pathfinding;

import java.util.Objects;

public class NodeRecord {
    private final PathfindingNode node;
    private double costSoFar;  // g(n)
    public double estimatedTotalCost;  // f(n)
    private NodeRecord connection;  // Node from which this node can be most efficiently reached.

    public NodeRecord(PathfindingNode node, double costSoFar, double estimatedTotalCost) {
        this.node = node;
        this.costSoFar = costSoFar;
        this.estimatedTotalCost = estimatedTotalCost;
        this.connection = null;  // By default, the connection is null. We set this as we explore nodes.
    }

    public PathfindingNode getNode() {
        return node;
    }

    public double getCostSoFar() {
        return costSoFar;
    }

    public void setCostSoFar(double costSoFar) {
        this.costSoFar = costSoFar;
    }

    public double getEstimatedTotalCost() {
        return estimatedTotalCost;
    }

    public void setEstimatedTotalCost(double estimatedTotalCost) {
        this.estimatedTotalCost = estimatedTotalCost;
    }

    public NodeRecord getConnection() {
        return connection;
    }

    public void setConnection(NodeRecord connection) {
        this.connection = connection;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NodeRecord that = (NodeRecord) o;
        return node.equals(that.node);
    }

    @Override
    public int hashCode() {
        return Objects.hash(node.getX(), node.getY(), node.getZ());
    }

    @Override
    public String toString() {
        return "NodeRecord{" +
                "node=" + node +
                ", costSoFar=" + costSoFar +
                ", estimatedTotalCost=" + estimatedTotalCost +
                '}';
    }
}

