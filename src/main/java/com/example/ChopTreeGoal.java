package com.example;


import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ChopTreeGoal extends Goal {
    private static final int checkInterval = 20;
    private int timer = 0;
    private final AgentEntity agent;
    private BlockPos targetTree = null;
    private boolean movingToTree = false;

    private int movingTimerSec = 0;

    public static final Logger LOGGER = LoggerFactory.getLogger("mymod");

    public ChopTreeGoal(AgentEntity agent) {
        this.agent = agent;
    }

    @Override
    public boolean canStart() {
        return agent.hasAxe() && !hasTooMuchWood();
    }

    @Override
    public boolean shouldContinue() {
        return agent.hasAxe() && !hasTooMuchWood();
    }

    @Override
    public void tick() {
        if (timer++ < checkInterval) return;
        timer = 0;

        if (targetTree == null) {
            targetTree = findTree();
            if (targetTree == null) return;

            agent.sayInChat("Moving to tree (" + countWood() + "/32)");
            this.agent.equipAxe();
            this.agent.navTo(targetTree);
            movingTimerSec = 0;
            movingToTree = true;
        } else if (movingToTree) {
            movingTimerSec++;
            LOGGER.info("Moving to tree");
            agent.navTo(targetTree);
            boolean isAtTree = agent.getBlockPos().isWithinDistance(targetTree, 0.5)
                    || (agent.getBlockX() != targetTree.getX() && agent.getBlockZ() != targetTree.getZ());
            if (!isAtTree) {  // Check if within 2 blocks of target tree or below
                LOGGER.info("Not yet within 2 blocks of target");
            }

            if (movingTimerSec > 30) {
                BlockPos base = agent.findChest();
                if (base == null) {
                    agent.sayInChat("I can't find base. I'm just going to stand here.");
                    resetGoal();
                    return;
                }
                agent.sayInChat("Hey Boss, I'm stuck. I'm going back to base.");
                agent.navTo(base);
                return;
            }

            if (!isAtTree) return;

            LOGGER.info("Within 2 blocks of target");
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

    private BlockPos findTree() {
        BlockPos currentPosition = agent.getBlockPos();
        for (BlockPos pos : BlockPos.iterateOutwards(currentPosition, 30, 10, 30)) {  // Sample search radius values
            BlockState state = agent.getWorld().getBlockState(pos);
            if (isLog(state.getBlock())) {
                return pos;
            }
        }
        LOGGER.info("Didn't find a tree within 30 blocks");
        return null;
    }

    private void resetGoal() {
        targetTree = null;
        movingToTree = false;
        movingTimerSec = 0;
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

