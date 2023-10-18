package com.example;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class MovementController {
    private final AgentEntity agent;
    private final World world;
    private List<PathfindingNode> path;
    private int currentIndex;
    private MinecraftPathfindingAdapter pathfindingAdapter;
    public static final Logger LOGGER = LoggerFactory.getLogger("mymod");
    private boolean wasNullLastTime = false;


    public MovementController(AgentEntity agent, World world) {
        this.agent = agent;
        this.world = world;
        this.pathfindingAdapter = new MinecraftPathfindingAdapter(world);
    }

    public void clear() {
        this.path = null;
        this.currentIndex = 0;
    }

    public boolean moveTo(BlockPos goal) {
        LOGGER.info("MovementController::moveTo called for " + goal.toShortString());
        MinecraftPathfindingNode startNode = pathfindingAdapter.getNodeAt(agent.getBlockPos().down());
        MinecraftPathfindingNode goalNode = pathfindingAdapter.getNodeAt(goal);
        if (!startNode.notStrictCanStandOn()) {
            LOGGER.info("MovementController::moveTo start node is not standable (strict=false)");
            return false;
        }
        if (!goalNode.notStrictCanStandOn()) {
            LOGGER.info("MovementController::moveTo goal node is not standable (strict=false)");
            return false;
        }
        this.path = findPath(startNode, goalNode);
        if (path == null || path.size() == 0) {
            LOGGER.info("No path found!!");
            return false;
        }
        // Start at the beginning of the path.
        currentIndex = 0;

        LOGGER.info("MovementController::moveTo finished");
        return true;
    }

    public void tick() {
        if (path == null || currentIndex >= path.size()) {
            if (!wasNullLastTime) {
                LOGGER.info("MovementController::tick path is null or path complete");
                wasNullLastTime = true;
            }
            return;
        }

        wasNullLastTime = false;
        //LOGGER.info("MovementController::tick");

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
        agent.getNavigation().startMovingTo(currentTarget.getX(), currentTarget.getY(), currentTarget.getZ(), 0.5);
    }

    private List<PathfindingNode> findPath(PathfindingNode start, PathfindingNode goal) {
        LOGGER.info("MovementController::findPath called");
        LOGGER.info("MovementController::findPath start " + start.toString());
        LOGGER.info("MovementController::findPath goal " + goal.toString());
        AStarPathfinding pathfinding = new AStarPathfinding(world, start, goal);
        List<PathfindingNode> path = pathfinding.findPath();
        LOGGER.info("MovementController::findPath found path" + path.size() + " nodes");
        return path;
    }
}
