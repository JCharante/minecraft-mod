package com.example;

import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class AStarPathfinding {

    private final World world;
    private final PathfindingNode start;
    private final PathfindingNode goal;
    public static final Logger LOGGER = LoggerFactory.getLogger("mymod");

    public AStarPathfinding(World world, PathfindingNode start, PathfindingNode goal) {
        LOGGER.info("AStarPathfinding::constructor");
        this.world = world;
        this.start = start;
        this.goal = goal;
    }


    public List<PathfindingNode> findPath() {
        LOGGER.info("AStarPathfinding::findPath called");
        List<NodeRecord> openList = new ArrayList<>();
        List<NodeRecord> closedList = new ArrayList<>();

        // Add the start node
        NodeRecord startRecord = new NodeRecord(start, 0, heuristic(start, goal));
        openList.add(startRecord);

        NodeRecord current = null;
        int iterations = 0;
        List<PathfindingNode> path = new ArrayList<>();
        while (!openList.isEmpty()) {
            iterations++;
            current = getSmallestElement(openList);
//            if (iterations % 100 == 0) {
//                LOGGER.info("AStarPathfinding::findPath iterations: " + iterations + " manhattan distance: " + heuristic(current.getNode(), goal));
//            }

            if (iterations > 1000) return path;

            if (current.getNode().equals(goal)) {
                LOGGER.info("AStarPathfinding::findPath found goal");
                break; // Found the goal
            }

            List<PathfindingNode> neighbors = current.getNode().getAdjacentNodes();
            for (PathfindingNode neighbor : neighbors) {
                double costSoFar = current.getCostSoFar() + neighbor.getCost();

                // Check if node is in the closed list
                NodeRecord closedListRecord = findNodeInList(closedList, neighbor);
                if (closedListRecord != null) {
                    continue;
                }

                // Check if it's in the open list
                NodeRecord openListRecord = findNodeInList(openList, neighbor);
                if (openListRecord == null) {
                    // We've not seen this node before, so add it to the open list
                    NodeRecord record = new NodeRecord(neighbor, costSoFar, costSoFar + heuristic(neighbor, goal));
                    record.setConnection(current);  // set the parent/connection
                    openList.add(record);
                } else if (openListRecord.getCostSoFar() > costSoFar) {
                    // We found a shorter route to this node, so update its cost and connection
                    openListRecord.setCostSoFar(costSoFar);
                    openListRecord.setEstimatedTotalCost(costSoFar + heuristic(neighbor, goal));
                    openListRecord.setConnection(current);
                }
            }

            // Move current node to closed list
            openList.remove(current);
            closedList.add(current);
        }

        // Once we're done, we should have a path from the start node to the goal node
        // Now, we reconstruct the path from the goal node back to the start
        while (current != null) {
            path.add(0, current.getNode());
            current = current.getConnection();
        }

        LOGGER.info("AStarPathfinding::findPath returning path");
        return path;
    }

    private double heuristic(PathfindingNode a, PathfindingNode b) {
        // This can be a simple Manhattan distance for now.
        return 1 * (Math.abs(a.getX() - b.getX()) + Math.abs(a.getY() - b.getY()) + Math.abs(a.getZ() - b.getZ()));
    }

    private NodeRecord getSmallestElement(List<NodeRecord> list) {
        NodeRecord smallest = null;
        for (NodeRecord nodeRecord : list) {
            if (smallest == null || nodeRecord.estimatedTotalCost < smallest.estimatedTotalCost) {
                smallest = nodeRecord;
            }
        }
        return smallest;
    }

    // Helper function to find a node in a given list
    private NodeRecord findNodeInList(List<NodeRecord> list, PathfindingNode node) {
        for (NodeRecord record : list) {
            if (record.getNode().equals(node)) {
                return record;
            }
        }
        return null;
    }
}
