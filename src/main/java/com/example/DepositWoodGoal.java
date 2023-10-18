package com.example;

import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DepositWoodGoal extends Goal {
    public static final Logger LOGGER = LoggerFactory.getLogger("mymod");
    private final AgentEntity agent;
    private final int checkInterval = 10;
    private int timer = 0;
    private boolean movingToChest;
    private BlockPos targetChest = null;
    private BlockPos targetGround = null;


    public DepositWoodGoal(AgentEntity agent) {
        this.agent = agent;
    }

    @Override
    public boolean canStart() {
        return hasTooMuchWood();
    }

    @Override
    public boolean shouldContinue() {
        return hasTooMuchWood() && targetChest != null;
    }

    @Override
    public void tick() {
        if (timer++ < checkInterval) return;
        timer = 0;

        if (targetChest == null || targetGround == null) {
            targetChest = agent.findChest();
            targetGround = agent.findStandableNearChest(targetChest);
            if (targetChest == null) {
                agent.sayInChat("Hmm, no chests near me to dropoff my wood.");
                return;
            };
            if (targetGround == null) {
                agent.sayInChat("There is a chest, but I can't reach it.");
                return;
            }
            agent.navTo(targetGround);
            movingToChest = true;
            agent.setStatus("Going to chest");
            agent.sayInChat("Alright boss, I'm coming back to a chest.");

        } else if (movingToChest) {
            // agent.navTo(targetGround);
            agent.setStatus("Moving to chest");
            if (agent.getBlockPos().isWithinDistance(targetChest, 2.5)) {
                agent.stopNav();
                agent.setStatus("At chest");
                agent.sayInChat("I'm at the chest!");
                dropOffWood(targetChest);
                movingToChest = false;
                targetChest = null;
            } else {
                LOGGER.info("DepositWoodGoal::tick ground is " + agent.getBlockPos().getManhattanDistance(targetChest));
            }
        }
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

    @Override
    public void start() {
        super.start();
        //agent.sayInChat("Starting DepositWoodGoal");
        //agent.setCustomName(Text.literal(agent.getRealName() + " (DepositWoodGoal)"));
    }

    @Override
    public void stop() {
        super.stop();
        agent.stopNav();
        //agent.sayInChat("Stopping DepositWoodGoal");
        //agent.setCustomName(Text.of(agent.getRealName()));
    }


    @SuppressWarnings("UnstableApiUsage")
    private void dropOffWood(BlockPos chestPos) {
        LOGGER.info("dropOffWood method called.");
        if (chestPos == null) return;
        BlockState blockState = agent.getWorld().getBlockState(chestPos);
        if (blockState.getBlock() instanceof ChestBlock) {
            // BlockEntity blockEntity = agent.getWorld().getBlockEntity(chestPos);

            //

            InventoryStorage simpleInventoryStorage = InventoryStorage.of(agent.getInventory(), null);
            ChestBlockEntity chestBlockEntity = (ChestBlockEntity) agent.getWorld().getBlockEntity(chestPos);
            if (chestBlockEntity == null) return;
            InventoryStorage chestInventoryStorage = InventoryStorage.of(chestBlockEntity, null);

            for (int i = 0; i < agent.getInventory().size(); i++) {
                ItemStack itemStack = agent.getInventory().getStack(i);
                // If the item stack isn't empty and it's not an axe, try to put it in the chest.
                if (!itemStack.isEmpty() && !(itemStack.getItem() instanceof AxeItem)) {
                    ItemVariant itemVariant = ItemVariant.of(itemStack);
                    Transaction tx = Transaction.openOuter();
                    long amountExtracted = simpleInventoryStorage.extract(itemVariant, itemStack.getCount(), tx);
                    long amountInserted = chestInventoryStorage.insert(itemVariant, amountExtracted, tx);
                    if (amountExtracted == amountInserted) {
                        tx.commit();
                    }
                }
            }


            //
//
//            if (blockEntity instanceof ChestBlockEntity) {
//                ChestBlockEntity chest = (ChestBlockEntity) blockEntity;
//
//                for (int i = 0; i < agent.getInventory().size(); i++) {
//                    ItemStack itemStack = agent.getInventory().getStack(i);
//
//                    // If the item stack isn't empty and it's not an axe, try to put it in the chest.
//                    if (!itemStack.isEmpty() && !(itemStack.getItem() instanceof AxeItem)) {
//
//                        // Transfer the item to the chest.
//                        // This will try to add the item to the chest, and return any leftovers.
//                        ItemStack leftover = ItemScatterer.spawn(agent.getWorld(), chestPos.getX(), chestPos.getY(), chestPos.getZ(), itemStack);
//
//                        // If there's any items that couldn't fit in the chest, put them back in the agent's inventory.
//                        if (!leftover.isEmpty()) {
//                            agent.getInventory().setStack(i, leftover);
//                        } else {
//                            agent.getInventory().setStack(i, ItemStack.EMPTY);
//                        }
//                    }
//                }
//            }
        }
    }
}
