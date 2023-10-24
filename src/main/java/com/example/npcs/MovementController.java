package com.example.npcs;

import com.example.pathfinding.AStarPathfinding;
import com.example.pathfinding.MinecraftPathfindingAdapter;
import com.example.pathfinding.MinecraftPathfindingNode;
import com.example.pathfinding.PathfindingNode;
import com.example.util.LogManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class MovementController {
    private final ControlledPlayer agent;
    private final World world;
    private List<PathfindingNode> path;
    private int currentIndex;
    private MinecraftPathfindingAdapter pathfindingAdapter;
    public static final Logger LOGGER = LoggerFactory.getLogger("mymod");
    private boolean wasNullLastTime = false;


    public MovementController(ControlledPlayer agent, World world) {
        this.agent = agent;
        this.world = world;
        this.pathfindingAdapter = new MinecraftPathfindingAdapter(world);
    }

    public void clear() {
        this.path = null;
        this.currentIndex = 0;
    }

    public boolean moveTo(BlockPos goal) {
        LogManager.info("MovementController-moveTo", "called for " + goal.toShortString());
        MinecraftPathfindingNode startNode = pathfindingAdapter.getNodeAt(agent.getBlockPos().down());
        MinecraftPathfindingNode goalNode = pathfindingAdapter.getNodeAt(goal);
        if (!startNode.notStrictCanStandOn()) {
            LogManager.info("MovementController-moveTo", "start node is not standable (strict=false)");
            return false;
        }
        if (!goalNode.notStrictCanStandOn()) {
            LogManager.info("MovementController-moveTo", "goal node is not standable (strict=false)");
            return false;
        }
        this.path = findPath(startNode, goalNode);
        if (path == null || path.size() == 0) {
            LogManager.info("MovementController-moveTo", "No path found!!");
            return false;
        }
        // Start at the beginning of the path.
        currentIndex = 0;

        LogManager.info("MovementController-moveTo", "reached end of moveTo, path found");
        return true;
    }

    public boolean isIdle() {
          return path == null || currentIndex >= path.size();
    }

    public void tick() {
        LogManager.info("MovementController-tick", "tick called");
        if (path == null || currentIndex >= path.size()) {
            if (!wasNullLastTime) {
                LogManager.info("MovementController-tick", "path is null or path complete");
                wasNullLastTime = true;
            }
            return;
        }

        wasNullLastTime = false;

        PathfindingNode currentNode = path.get(currentIndex);
        BlockPos currentTarget = pathfindingAdapter.getNodeAt(currentNode).getBlockPos();

        // If the agent is close enough to the current target node, move to the next node.
        if (agent.getBlockPos().isWithinDistance(currentTarget,  1.5)) {
            // LOGGER.info("MovementController::tick is within distance to current target");
            currentIndex++;
            if (currentIndex < path.size()) {
                currentTarget = pathfindingAdapter.getNodeAt(path.get(currentIndex)).getBlockPos();
            } else {
                // Reached the end of the path.
                return;
            }
        }

        // LOGGER.info("Using built in navigation");
        // Use the built-in navigation to move to the current target.
        agent.getMoveControl().moveTo(currentTarget.getX(), currentTarget.getY(), currentTarget.getZ());
    }

    private List<PathfindingNode> findPath(PathfindingNode start, PathfindingNode goal) {
//        LOGGER.info("MovementController::findPath called");
//        LOGGER.info("MovementController::findPath start " + start.toString());
//        LOGGER.info("MovementController::findPath goal " + goal.toString());
        AStarPathfinding pathfinding = new AStarPathfinding(world, start, goal);
        List<PathfindingNode> path = pathfinding.findPath();
//        LOGGER.info("MovementController::findPath found path" + path.size() + " nodes");
        return path;
    }
}
