package com.example.npcs.goals;


import com.example.npcs.ControlledPlayer;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;


/**
 * ┌────────────────────────────────────────────────────────┐
 * │                                                        │
 * │   ┌─────────┐                                    ┌─────┴────────┐
 * │   │         │            ┌─────────────────┐     │              │
 * │   │  START  ├────────────►                 │     │              │
 * │   │         │            │ MOVING_TO_CHEST ├────►│   AT CHEST   │
 * │   └───┬─────┘            │                 │     │              │
 * │       │                  └───▲─────────────┘     └──────┬───────┘
 * │       │                      │                          │
 * │       │                      │                          │
 * │  ┌────▼──────┐         ┌─────┴────┐                     │
 * └──►  NO_TREE  ◄─────────┤          │              ┌──────▼────────┐
 *    └────┬──────┘         │  AT_TREE │              │               │
 *         │                │          │              │     END       │
 *         │                └────▲─────┘              │               │
 *         │                     │                    └───────────────┘
 *     ┌───▼────────────┐        │
 *     │                ├────────┘
 *     │ MOVING_TO_TREE │
 *     │                │
 *     └────────────────┘
 */

enum ChopTreeGoalStates {
    MOVING_TO_CHEST,
    MOVING_TO_TREE,
    AT_TREE,
    AT_CHEST,
    NO_TREE,
    START,
    END
}

abstract class GoalState<State> {
    public State state;
    State getState() {
        return this.state;
    }
    void tick() {

    }
    GoalState<State> nextState() {
        return this;
    }
}

class AtTreeState extends GoalState<ChopTreeGoalStates> {
    ControlledPlayer cp;
    BlockPos tree;
    boolean isChopping = false;

    public AtTreeState(ControlledPlayer cp, BlockPos tree) {
        this.state = ChopTreeGoalStates.AT_TREE;
        this.cp = cp;
        this.tree = tree;
    }

    static boolean canStart(ControlledPlayer cp, BlockPos tree) {
        return cp.hasAxe()
                && tree != null
                && tree.isWithinDistance(cp.getBlockPos(), 2.0)
                && ChopTreeGoal.isLog(cp.getWorld().getBlockState(tree).getBlock());
    }

    static boolean shouldStart() {
        return true;
    }

    @Override
    GoalState<ChopTreeGoalStates> nextState() {
        if (
                isChopping
                || (
                        cp.hasAxe()
                        && tree != null
                        && tree.isWithinDistance(cp.getBlockPos(), 2.0)
                        && ChopTreeGoal.isLog(cp.getWorld().getBlockState(tree).getBlock())
                )
        ) {
            return this;
        } else if (NoTreeState.canStart() && NoTreeState.shouldStart()) {
            return new NoTreeState(this.cp);
        } else {
            return new EndState(this.cp);
        }
    }

    void tick() {
        Block blockAtTreePos = cp.getWorld().getBlockState(tree).getBlock();
        if (isChopping && !ChopTreeGoal.isLog(blockAtTreePos)) {
            // tree is gone
            cp.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, tree, cp.player.getHorizontalFacing()));
            isChopping = false;
        } else if (!isChopping) {
            isChopping = true;
            cp.player.swingHand(cp.player.getActiveHand());
            cp.player.networkHandler.sendPacket(
                    new PlayerActionC2SPacket(
                            PlayerActionC2SPacket.Action.START_DESTROY_BLOCK,
                            tree,
                            cp.player.getHorizontalFacing()
                    )
            );
        }
    }
}

class EndState extends GoalState<ChopTreeGoalStates> {
    ControlledPlayer cp;

    public EndState(ControlledPlayer cp) {
        this.cp = cp;
    }
    static boolean canStart() {
        return true;
    }

    static boolean shouldStart(ControlledPlayer cp) {
        return !cp.hasAxe(); // no breaks until axe breaks
    }

    public GoalState<ChopTreeGoalStates> nextState() {
        return this;
    }
}

class AtChestState extends GoalState<ChopTreeGoalStates> {
    ControlledPlayer cp;
    BlockPos chestPos;

    public AtChestState(ControlledPlayer cp, BlockPos chestPos) {
        this.state = ChopTreeGoalStates.AT_CHEST;
        this.cp = cp;
        this.chestPos = chestPos;
    }

    static boolean canStart(ControlledPlayer cp, BlockPos chestPos) {
        return ChopTreeGoal.countWood(cp) >= 32
                && chestPos != null
                && chestPos.isWithinDistance(cp.getBlockPos(), 2.0);
    }

    static boolean shouldStart() {
        return true;
    }

    GoalState<ChopTreeGoalStates> nextState() {
        if (ChopTreeGoal.countWood(this.cp) >= 32 && chestPos.isWithinDistance(cp.getBlockPos(), 2.0)) {
            return this;
        } else if (ChopTreeGoal.countWood(this.cp) >= 32
                && MovingToChestState.canStart(cp)
                && MovingToChestState.shouldStart(cp)
        ) {
            return new MovingToChestState(this.cp, chestPos);
        } else {
            return new NoTreeState(this.cp);
        }
    }

    void tick() {
        if (cp.getWorld().getBlockState(chestPos).getBlock() instanceof ChestBlock) {
            PlayerInventory playerInventory = cp.player.getInventory();
            ChestBlockEntity chestBlockEntity = (ChestBlockEntity) cp.getWorld().getBlockEntity(chestPos);
            if (chestBlockEntity == null) return;
            InventoryStorage chestInventoryStorage = InventoryStorage.of(chestBlockEntity, null);

            for (int i = 0; i < playerInventory.size(); i++) {
                ItemStack itemStack = playerInventory.getStack(i);

                if (!itemStack.isEmpty() && !(itemStack.getItem() instanceof AxeItem)) {
                    ItemVariant itemVariant = ItemVariant.of(itemStack);

                    Transaction tx = Transaction.openOuter();
                    long amountExtracted = itemStack.getCount();
                    playerInventory.removeStack(i);
                    long amountInserted = chestInventoryStorage.insert(itemVariant, amountExtracted, tx);

                    if (amountExtracted == amountInserted) {
                        tx.commit();
                    }
                }
            }
        }
    }
}

class MovingToTreeState extends GoalState<ChopTreeGoalStates> {
    ControlledPlayer cp;
    BlockPos tree;
    BlockPos groundNearTree;
    int timer = 40;
    int interval = 60;

    public MovingToTreeState(ControlledPlayer cp, BlockPos tree, BlockPos groundNearTree) {
        this.state = ChopTreeGoalStates.MOVING_TO_TREE;
        this.cp = cp;
        this.tree = tree;
        this.groundNearTree = groundNearTree;
    }

    static boolean canStart(BlockPos targetTree, BlockPos targetGround) {
        return targetTree != null && targetGround != null;
    }

    static boolean shouldStart() {
        return true;
    }

    @Override
    GoalState<ChopTreeGoalStates> nextState() {
        if (AtTreeState.canStart(cp, tree) && AtTreeState.shouldStart()) {
            return new AtTreeState(cp, tree);
        } else {
            return this;
        }
    }

    @Override
    void tick() {
        if (timer++ >= interval) {
            ChopTreeGoal.LOGGER.info("ChopTreeGoal::MovingToTreeState::tick in interval called");
            timer = 0;
            cp.navTo(groundNearTree);
        }
    }
}

class MovingToChestState extends GoalState<ChopTreeGoalStates> {
    ControlledPlayer cp;
    BlockPos chestPos;
    BlockPos groundNearChest;

    int timer = 0;
    int interval = 40;
    public MovingToChestState(ControlledPlayer cp, BlockPos chest) {
        this.state = ChopTreeGoalStates.MOVING_TO_CHEST;
        this.cp = cp;
        this.chestPos = chest;
        this.groundNearChest = cp.findStandableNearChest(chest);
    }

    static boolean canStart(ControlledPlayer cp) {
        return cp.findChest() != null;
    }

    static boolean shouldStart(ControlledPlayer cp) {
        return ChopTreeGoal.countWood(cp) >= 32;
    }

    GoalState<ChopTreeGoalStates> nextState() {
        if (
                chestPos.isWithinDistance(cp.getBlockPos(), 2.0)
                && AtChestState.canStart(cp, chestPos)
                && AtChestState.shouldStart()
        ) {
            return new AtChestState(cp, chestPos);
        } else {
            return this;
        }
    }

    void tick() {
        if (timer++ > interval) {
            timer = 0;
            cp.navTo(groundNearChest);
        }
    }
}

class NoTreeState extends GoalState<ChopTreeGoalStates> {
    ControlledPlayer cp;
    BlockPos tree;
    BlockPos groundNearTree;
    public NoTreeState(ControlledPlayer cp) {
        this.state = ChopTreeGoalStates.NO_TREE;
        this.cp = cp;
        onStart();
    }

    static boolean canStart() {
        return true;
    }

    static boolean shouldStart() {
        return true;
    }
    GoalState<ChopTreeGoalStates> nextState() {
        if (groundNearTree != null && MovingToTreeState.canStart(tree, groundNearTree) && MovingToTreeState.shouldStart()) {
            return new MovingToTreeState(this.cp, tree, groundNearTree);
        } else {
            return this;
        }
    }

    void onStart() {
        ChopTreeGoal.LOGGER.info("ChopTreeGoal::NoTreeState::onStart called");
        if (tree == null) {
            BlockPos home = this.cp.findChest();
            if (home == null) {
                return;
            }
            int range = 100;
            for (BlockPos pos : BlockPos.iterateOutwards(home, range, 40, range)) {
                BlockPos potentialPos = pos.down();
                BlockState state = cp.getWorld().getBlockState(pos);
                BlockState potentialState = cp.getWorld().getBlockState(potentialPos);
                // find bottom of tree
                while (ChopTreeGoal.isLog(potentialState.getBlock()) && ChopTreeGoal.notInBlacklist(potentialPos)) {
                    pos = potentialPos;
                    state = potentialState;
                    potentialPos = potentialPos.down();
                    potentialState = cp.getWorld().getBlockState(potentialPos);
                }
                if (ChopTreeGoal.isLog(state.getBlock()) && ChopTreeGoal.notInBlacklist(pos)) {
                    this.tree = pos;
                    groundNearTree = cp.findStandableNearChest(tree);
                    if (groundNearTree == null) {
                        ChopTreeGoal.blacklist.add(tree);
                        this.tree = null;
                    } else {
                        return;
                    }
                }
            }
        }
    }

    void tick() {
        ChopTreeGoal.LOGGER.info("ChopTreeGoal::NoTreeState::tick called");
    }
}

class StartState extends GoalState<ChopTreeGoalStates> {
    ControlledPlayer cp;
    public StartState(ControlledPlayer cp) {
        this.state = ChopTreeGoalStates.START;
        this.cp = cp;
    }

    static boolean canStart(ControlledPlayer cp) {
        return cp.hasAxe();
    }

    GoalState<ChopTreeGoalStates> nextState() {
        // transition to next state depending on factors
        if (MovingToChestState.canStart(cp) && MovingToChestState.shouldStart(cp)) {
            return new MovingToChestState(this.cp, cp.findChest());
        } else if (NoTreeState.canStart() && NoTreeState.shouldStart()) {
            return new NoTreeState(this.cp);
        } else {
            return this;
        }
    }

    void tick() {
        ChopTreeGoal.LOGGER.info("ChopTreeGoal::StartState::tick called");
    }
}

public class ChopTreeGoal extends Goal {
    private final ControlledPlayer cp;

    public static final Logger LOGGER = LoggerFactory.getLogger("mymod");

    public static ArrayList<BlockPos> blacklist;
    GoalState<ChopTreeGoalStates> currentState;

    public ChopTreeGoal(ControlledPlayer agent) {
        this.cp = agent;
        if (blacklist == null) {
            blacklist = new ArrayList<>();
        }
    }

    @Override
    public boolean canStart() {
        return StartState.canStart(this.cp);
    }

    @Override
    public boolean shouldContinue() {
        return !(currentState instanceof EndState);
    }

    @Override
    public void start() {
        super.start();
        currentState = new StartState(this.cp);
    }

    @Override
    public void stop() {
        super.stop();
        currentState = new EndState(this.cp);
    }

    @Override
    public void tick() {
        super.tick();
        currentState = currentState.nextState();
        currentState.tick();
    }

    public static boolean isLog(Block block) {
        if (block == null) return false;
        return block.equals(Blocks.OAK_LOG)
                || block.equals(Blocks.SPRUCE_LOG)
                || block.equals(Blocks.BIRCH_LOG)
                || block.equals(Blocks.JUNGLE_LOG)
                || block.equals(Blocks.ACACIA_LOG)
                || block.equals(Blocks.DARK_OAK_LOG);
    }

    public static boolean notInBlacklist(BlockPos pos) {
        for (BlockPos p : ChopTreeGoal.blacklist) {
            if (p.getX() == pos.getX()
                    // && p.getY() == pos.getY()
                    && p.getZ() == pos.getZ()) {
                return false;
            }
        }
        return true;
    }


    public static int countWood(ControlledPlayer cp) {
        // TODO: make this search for wood
        int max = 0;
        for (int i = 0; i < cp.player.getInventory().size(); i++) {
            ItemStack stack = cp.player.getInventory().getStack(i);
            max = Math.max(max, stack.getCount());
        }
        return max;
    }
}

