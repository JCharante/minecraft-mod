package com.example;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class GetEquipmentGoal extends Goal {
    public static final Logger LOGGER = LoggerFactory.getLogger("mymod");

    private final ControlledPlayer agent;
    private final int checkInterval = 100; // Adjust as needed, this checks every 5 seconds
    private int timer = 0;
    private BlockPos targetChest = null;
    private boolean movingToChest = false;

    public GetEquipmentGoal(ControlledPlayer agent) {
        this.agent = agent;
    }

    @Override
    public boolean canStart() {
        // This goal can start if the agent doesn't have an axe
        return !agent.hasAxe();
    }

    @Override
    public void start() {
        super.start();
        agent.sayInChat("Starting GetEquipmentGoal");
        agent.setStatus("Getting Equipment");
    }

    @Override
    public void stop() {
        super.stop();
        agent.sayInChat("Stopping GetEquipmentGoal");
        agent.setStatus("");
    }

    @Override
    public boolean canStop() {
        return agent.hasAxe();
    }

    @Override
    public boolean shouldContinue() {
        if (agent.hasAxe()) {
            agent.sayInChat("I have an axe!");
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (agent.hasAxe() || timer++ < checkInterval) return;

        timer = 0;

        if (targetChest == null) {
            // Find a chest
            targetChest = findChestWithAxe();
            if (targetChest == null) {
                agent.sayInChat("Well, there's no axes nearby..");
                return;
            } else {
                agent.sayInChat("Okay, moving to chest.");
            }
            BlockPos ground = agent.findStandableNearChest(targetChest);
            this.agent.navTo(ground);
            movingToChest = true;
            return;
        }

        if (movingToChest) {
            agent.sayInChat("Still walking...");
            if (agent.getBlockPos().isWithinDistance(targetChest, 2.0)) {  // Check if within 2 blocks of target chest
                // Interact with the chest
                BlockEntity be = agent.getWorld().getBlockEntity(targetChest);
                if (be instanceof ChestBlockEntity) {
                    ChestBlockEntity chest = (ChestBlockEntity) be;
                    for (int i = 0; i < chest.size(); i++) {
                        ItemStack stack = chest.getStack(i);
                        if (stack.getItem() instanceof AxeItem) {
                            // Take the axe
                            ItemStack agentAxe = stack.copy();
                            chest.removeStack(i, 1);  // Remove one axe from the chest
                            agent.player.getInventory().insertStack(agentAxe);  // Add to agent's inventory
                            this.equipAxe();
                            this.targetChest = null;
                            this.movingToChest = false;
                            return;
                        }
                    }
                }
            }
        }
    }

    private void equipAxe() {
        // TODO: fix
        for (int i = 0; i < agent.player.getInventory().size(); i++) {
            ItemStack stack = agent.player.getInventory().getStack(i);
            if (stack.getItem() instanceof AxeItem) {
                agent.player.equipStack(EquipmentSlot.MAINHAND, stack);
            }
        }
    }

    private BlockPos findChestWithAxe() {
        BlockPos chestPos = agent.findChest();
        if (chestPos == null) {
            agent.sayInChat("I can't find a chest!");
            return null;
        }
        BlockState state = agent.getWorld().getBlockState(chestPos);
        Block block = state.getBlock();

        if (block instanceof ChestBlock) {
            BlockEntity be = agent.getWorld().getBlockEntity(chestPos);
            if (be instanceof ChestBlockEntity) {
                ChestBlockEntity chest = (ChestBlockEntity) be;
                for (int i = 0; i < chest.size(); i++) {
                    ItemStack stack = chest.getStack(i);
                    if (stack.getItem() instanceof AxeItem) {
                        return chestPos;
                    }
                }
            }
        }
        return null;
    }
}
