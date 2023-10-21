package com.example;


import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;


enum ChopTreeGoalStates {
    MOVING_TO_CHEST,
    MOVING_TO_TREE,
    AT_TREE,
    AT_CHEST,
    NO_TREE,
}

public class ChopTreeGoal extends Goal {
    private static final int checkInterval = 10;
    private int timer = 0;
    private final ControlledPlayer agent;
    private BlockPos targetTree = null;
    private BlockPos targetGround = null;
    private boolean movingToTree = false;
    private boolean movingToChest = false;
    private BlockPos targetChest;

    public static final Logger LOGGER = LoggerFactory.getLogger("mymod");

    public static ArrayList<BlockPos> blacklist;

    public ChopTreeGoal(ControlledPlayer agent) {
        this.agent = agent;
        if (blacklist == null) {
            blacklist = new ArrayList<>();
        }
        targetChest = agent.findChest();
    }

    @Override
    public boolean canStart() {
        boolean ret = agent.hasAxe() && !hasTooMuchWood() && getNextTree();
        return ret;
    }

    @Override
    public boolean shouldContinue() {
        boolean ret = (agent.hasAxe()
                && !hasTooMuchWood()
                && targetChest != null
                && getNextTree()
        ) || movingToChest || movingToTree;
        if (!ret) {
            agent.sayInChat("I shouldn't continue chopping tree goal");
        }
        return ret;
    }

    @Override
    public void start() {
        super.start();
        agent.sayInChat("Starting ChopTreeGoal");
        agent.setCustomName(Text.literal(agent.getRealName() + " (ChopTreeGoal)"));
    }

    @Override
    public void stop() {
        super.stop();
        agent.sayInChat("Stopping ChopTreeGoal");
        agent.setCustomName(Text.of(agent.getRealName()));
    }

    private boolean isAtTree() {
        if (targetTree == null) return false;
        return agent.getBlockPos().isWithinDistance(targetTree, 2.1)
                || (
                Math.abs(agent.getBlockX() - targetTree.getX()) <= 2
                        &&
                        Math.abs(agent.getBlockZ() - targetTree.getZ()) <= 2
        );
    }

    private boolean isAtChest() {
        if (targetChest == null) return false;
        return agent.getBlockPos().isWithinDistance(targetChest, 2.1);
    }

    private double distanceFromTree() {
        if (targetTree == null) return 0D;
        return agent.getBlockPos().getManhattanDistance(targetTree);
    }

    private void tickAtTree() {
        agent.stopNav();
        agent.setStatus("At Tree");
        movingToTree = false;
        // Start chopping the tree
        if (!isLog(agent.getWorld().getBlockState(targetTree).getBlock())) {
            LOGGER.info("Weird, My target isn't a log. Resetting.");
            resetGoal();
            return;
        }

        agent.getWorld().breakBlock(targetTree, true);
        if (isLog(agent.getWorld().getBlockState(targetTree.up()).getBlock())) {
            // Move to the next block above
            targetTree = targetTree.up();
        } else {
            agent.sayInChat("Well, I guess this tree is done.");
            resetGoal();
        }
    }

    private boolean getNextTree() {
        // if current tree is gone, find a new one
        if (
                targetTree == null
                || targetGround == null
                || !isLog(agent.getWorld().getBlockState(targetTree).getBlock())
                || inBlackList(targetTree)
        ) {
            targetTree = findTree();
        } else {
            return true; // we're fine
        }
        while (targetTree != null) {
            targetGround = findGround(targetTree);
            boolean possible = this.agent.navTo(targetGround);
            if (targetGround != null && !possible) {
                LOGGER.info("Can't navigate to " + targetGround.toShortString());
            }
            if (targetGround != null && possible) break;
            blacklist.add(targetTree);
            targetTree = findTree();
        }
        return targetTree != null;
    }

    @Override
    public void tick() {
        super.tick();
        // LOGGER.info("ChopTreeGoal tick");
        if (timer++ < checkInterval) return;
        timer = 0;

        if (movingToChest) {
            if (isAtChest()) resetGoal();
            agent.setStatus("MT:Chest " + distanceFromTree());
            return;
        }

        if (isAtTree()) {
            movingToTree = false;
            tickAtTree();
            return;
        }

        if (movingToTree) {
            agent.setStatus("MT:Tree " + distanceFromTree());
            return;
        }


        if (targetTree == null || targetGround == null) {
            if (!getNextTree()) {
                LOGGER.info("No trees found");
                agent.setStatus("No Trees");
                BlockPos standable = agent.findStandableNearChest(this.targetChest);
                agent.navTo(standable, true);
                movingToChest = true;
                return;
            }
            //agent.sayInChat("Moving to tree root (" + countWood() + "/32)");
            this.agent.equipAxe();
            //agent.sayInChat("Moving to ground at " + targetGround.toShortString());
            agent.setCustomName(Text.literal(agent.getRealName() + " (Moving)"));
            agent.navTo(targetGround);
            movingToTree = true;
            LOGGER.info("ChopTreeGoal::tick moving to tree");
        } else {
            LOGGER.info("ChopTreeGoal::tick shouldn't get here");
            LOGGER.info("targetTree " + targetTree.toShortString());
            LOGGER.info("targetGround " + targetGround.toShortString());
            LOGGER.info("movingToTree" + movingToTree);
            LOGGER.info("movingToChest" + movingToChest);
            LOGGER.info("targetChest" + targetChest.toShortString());
        }
    }

    public boolean isLog(Block block) {
        if (block == null) return false;
        return block.equals(Blocks.OAK_LOG)
                || block.equals(Blocks.SPRUCE_LOG)
                || block.equals(Blocks.BIRCH_LOG)
                || block.equals(Blocks.JUNGLE_LOG)
                || block.equals(Blocks.ACACIA_LOG)
                || block.equals(Blocks.DARK_OAK_LOG);
    }

    private boolean inBlackList(BlockPos pos) {
        for (BlockPos p : blacklist) {
            if (p.getX() == pos.getX()
                    // && p.getY() == pos.getY()
                    && p.getZ() == pos.getZ()) {
                return true;
            }
        }
        return false;
    }

    private BlockPos findTree() {
        BlockPos currentPosition = this.targetChest;
        if (currentPosition == null) {
            LOGGER.info("ChopTreeGoal::findTree Couldn't find a chest");
            return null;
        }
        LOGGER.info("ChopTreeGoal::findTree Finding a tree...");
        int range = 100;
        for (BlockPos pos : BlockPos.iterateOutwards(currentPosition, range, 40, range)) {
            BlockPos potentialPos = pos.down();
            BlockState state = agent.getWorld().getBlockState(pos);
            BlockState potentialState = agent.getWorld().getBlockState(potentialPos);
            // find bottom of tree
            while (isLog(potentialState.getBlock()) && !inBlackList(potentialPos)) {
                pos = potentialPos;
                state = potentialState;
                potentialPos = potentialPos.down();
                potentialState = agent.getWorld().getBlockState(potentialPos);
            }
            if (isLog(state.getBlock()) && !inBlackList(pos)) {
                return pos;
            }
        }
        // LOGGER.info("Didn't find a tree within 60 blocks");
        return null;
    }

    private BlockPos findGround(BlockPos tree) {
        BlockPos root = findRoot(tree);
        if (root == null) {
            LOGGER.info("Didn't find a tree root");
            return null;
        };
        // Find empty space with 2 air next to root
        Direction[] dirs = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
        for (Direction direction : dirs) {
            BlockPos adjacentPosition = root.offset(direction);
            if (MinecraftPathfindingNode.canStandOn(agent.getWorld(), adjacentPosition)) {
                return adjacentPosition;
            }
        }
        return null;
    }

    private BlockPos findRoot(BlockPos tree) {
        while (tree.getY() > 0) {
            BlockState state = agent.getWorld().getBlockState(tree.down());
            if (state.getBlock().equals(Blocks.DIRT)) {
                return tree.down();
            }
            tree = tree.down();
        }

        return null;
    }

    private void resetGoal() {
        targetTree = null;
        targetGround = null;
        movingToTree = false;
        movingToChest = false;
        agent.stopNav();
    }

    private boolean hasTooMuchWood() {
        // TODO: make this search for wood
        for (int i = 0; i < this.agent.player.getInventory().size(); i++) {
            ItemStack stack = this.agent.player.getInventory().getStack(i);
            if (stack.getCount() >= 32) {
                return true;
            }
        }
        return false;
    }

    private int countWood() {
        // TODO: make this search for wood
        int max = 0;
        for (int i = 0; i < this.agent.player.getInventory().size(); i++) {
            ItemStack stack = this.agent.player.getInventory().getStack(i);
            max = Math.max(max, stack.getCount());
        }
        return max;
    }
}

