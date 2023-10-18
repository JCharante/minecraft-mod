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


public class ChopTreeGoal extends Goal {
    private static final int checkInterval = 10;
    private int timer = 0;
    private final AgentEntity agent;
    private BlockPos targetTree = null;
    private BlockPos targetGround = null;
    private boolean movingToTree = false;

    private int movingTimerSec = 0;
    private BlockPos lastPos;

    public static final Logger LOGGER = LoggerFactory.getLogger("mymod");

    public static ArrayList<BlockPos> blacklist;

    public ChopTreeGoal(AgentEntity agent) {
        this.agent = agent;
        if (blacklist == null) {
            blacklist = new ArrayList<>();
        }
    }

    @Override
    public boolean canStart() {
        boolean ret = agent.hasAxe() && !hasTooMuchWood();
        return ret;
    }

    @Override
    public boolean shouldContinue() {
        boolean ret = agent.hasAxe() && !hasTooMuchWood();
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

    private double distanceFromTree() {
        if (targetTree == null) return 0D;
        return agent.getBlockPos().getManhattanDistance(targetTree);
    }

    @Override
    public void tick() {
        super.tick();
        // LOGGER.info("ChopTreeGoal tick");
        if (timer++ < checkInterval) return;
        timer = 0;

        //LOGGER.info("ChopTreeGoal tick");

        if (targetTree == null || targetGround == null) {
            agent.setCustomName(Text.literal(agent.getRealName() + " (No Target)"));
            targetTree = findTree();
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

            if (targetTree == null) {
                agent.sayInChat("I can't find any trees.");
                resetGoal();
                return;
            }

            agent.sayInChat("Moving to tree root (" + countWood() + "/32)");
            this.agent.equipAxe();
            agent.sayInChat("Moving to ground at " + targetGround.toShortString());
            agent.setCustomName(Text.literal(agent.getRealName() + " (Moving)"));
            movingToTree = true;
        } else if (isAtTree()) {
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
                targetTree = targetTree.up();  // Move to the next block above
            } else {
                agent.sayInChat("Well, I guess this tree is done.");
                resetGoal();
            }
        } else if (movingToTree) {
            //movingTimerSec++;
            agent.setStatus("MoveTree " + distanceFromTree());
            // LOGGER.info("Moving to tree");
            // agent.navTo(targetGround);
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
        BlockPos currentPosition = agent.findChest();
        if (currentPosition == null) {
            LOGGER.info("ChopTreeGoal::findTree Couldn't find a chest");
            return null;
        }
        for (BlockPos pos : BlockPos.iterateOutwards(currentPosition, 60, 20, 60)) {  // Sample search radius values
            BlockState state = agent.getWorld().getBlockState(pos);
            if (isLog(state.getBlock()) && !inBlackList(pos)) {
                return pos;
            }
        }
        LOGGER.info("Didn't find a tree within 60 blocks");
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
        agent.stopNav();
    }

    private boolean hasTooMuchWood() {
        // TODO: make this search for wood
        for (int i = 0; i < this.agent.getInventory().size(); i++) {
            ItemStack stack = this.agent.getInventory().getStack(i);
            if (stack.getCount() >= 32) {
                return true;
            }
        }
        return false;
    }

    private int countWood() {
        // TODO: make this search for wood
        int max = 0;
        for (int i = 0; i < this.agent.getInventory().size(); i++) {
            ItemStack stack = this.agent.getInventory().getStack(i);
            max = Math.max(max, stack.getCount());
        }
        return max;
    }
}

