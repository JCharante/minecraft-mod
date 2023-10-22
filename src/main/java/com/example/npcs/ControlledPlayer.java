package com.example.npcs;

import com.example.pathfinding.MinecraftPathfindingNode;
import com.example.npcs.bare.CustomPlayer;
import com.example.npcs.goals.ChopTreeGoal;
import com.example.npcs.goals.GetEquipmentGoal;
import com.google.gson.*;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class ControlledPlayer {
    public CustomPlayer player;
    public ArrayList<Goal> goals;
    public Goal currentGoal;
    public String role;
    public static final Logger LOGGER = LoggerFactory.getLogger("mymod");
    private MovementController movementController;
    private CustomMoveControl moveControl;

    public String username;

    public ControlledPlayer() {
        initVars();
    }

    public ControlledPlayer(CustomPlayer player) {
        initVars();
        setPlayer(player);
    }

    private void initVars() {
        goals = new ArrayList<>();
        role = "";
    }

    public CustomMoveControl getMoveControl() {
        return moveControl;
    }

    public MovementController getMovementController() {
        return movementController;
    }

    public void setPlayer(CustomPlayer player) {
        this.player = player;
        username = player.getGameProfile().getName();
        movementController = new MovementController(this, player.getWorld());
        moveControl = new CustomMoveControl(this);
    }

    public void setRole(String role) {
        sayInChat("Setting role to " + role);
        this.role = role;
        setGoalsForRole();
    }

    private void setGoalsForRole() {
        goals.clear();
        if (role.equals("lumberjack")) {
            goals.add(new GetEquipmentGoal(this));
            goals.add(new ChopTreeGoal(this));
        }
    }

    private void tickGoals() {
        if (currentGoal != null && !currentGoal.shouldContinue() && currentGoal.canStop()) {
            currentGoal.stop();
            currentGoal = null;
        }
        if (currentGoal == null) {
            for (Goal goal : goals) {
                if (goal.canStart()) {
                    currentGoal = goal;
                    currentGoal.start();
                    break;
                }
            }
        }
        if (currentGoal != null) {
            currentGoal.tick();
        }
    }

    public void tick() {
        tickGoals();
        player.playerTick();
        movementController.tick();
        moveControl.tick();
    }

    public void performAction() {
        // Call methods on the player instance to control its behavior.
        // For example, making the player jump:
        player.sendMessage(Text.literal("Hello, world!"), false);
        player.jump();
    }

    public BlockPos findChest() {
        // TODO: cache this value
        BlockPos currentPosition = player.getBlockPos();
        int searchRadius = 100;

        // Loop over a small area around the entity to check for chests
        for (BlockPos pos : BlockPos.iterateOutwards(currentPosition, searchRadius, 30, searchRadius)) {
            BlockState state = player.getWorld().getBlockState(pos);
            Block block = state.getBlock();

            if (block instanceof ChestBlock) {
                BlockEntity be = player.getWorld().getBlockEntity(pos);
                return pos;
            }
        }
        return null;
    }

    public boolean hasAxe() {
        // Loop through the agent's inventory and check for any type of axe
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() instanceof AxeItem) {
                return true;
            }
        }
        return false;
    }

    public String getUsername() {
        return this.username; // player.getGameProfile().getName();
    }

    public String getRealName() {
        return getUsername();
    }

    public BlockPos getBlockPos() {
        return player.getBlockPos();
    }

    public int getBlockX() {
        return player.getBlockX();
    }

    public int getBlockY() {
        return player.getBlockY();
    }

    public int getBlockZ() {
        return player.getBlockZ();
    }

    public void sayInChat(String message) {
        if (player == null) return;
        if (!player.getWorld().isClient) {
            MinecraftServer server = player.getWorld().getServer();
            if (server != null) {
                Text text = Text.literal(getUsername() + ": " + message);
                server.getPlayerManager().broadcast(text, false);
            }
        }
    }

    @Deprecated
    public void setCustomName(Text text) {
        // player.setCustomName(text);
    }

    public void setStatus(String status) {
        player.setCustomName(Text.of(getUsername() + " " + status));
        // player.setStatus(status);
    }

    public void stopNav() {
        //
    }

    public boolean navTo(BlockPos targetBlockPos, boolean tpIfFail) {
        LOGGER.info("Agent navTo " + targetBlockPos);
        if (targetBlockPos == null) {
            LOGGER.info("You tried to make me navTo null!");
            return false;
        };
        LOGGER.info("Agent navTo " + targetBlockPos);
        boolean possible = movementController.moveTo(targetBlockPos);
        if (possible || !tpIfFail) return possible;
        BlockPos chest = this.findChest();
        if (chest == null) return false;
        BlockPos ground = this.findStandableNearChest(chest);
        if (ground == null) return false;
        player.teleport(ground.getX(), ground.getY() + 2, ground.getZ());
        sayInChat("Sorry, but I had to teleport to the ground near the chest after finding myself stuck.");
        return false;
    }

    public boolean navTo(BlockPos targetBlockPos) {
        return this.navTo(targetBlockPos, false);
    }

    public BlockPos findStandableNearChest(BlockPos chest) {
        // Starting by going down one block from the chest
        BlockPos startingPosition = chest.down();

        Direction[] dirs = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

        // Checking the block directly beneath the chest first
        if (MinecraftPathfindingNode.canStandOn(this.getWorld(), startingPosition)) {
            return startingPosition;
        }

        // Then checking the blocks adjacent to that
        for (Direction direction : dirs) {
            BlockPos adjacentPosition = startingPosition.offset(direction);
            if (MinecraftPathfindingNode.canStandOn(this.getWorld(), adjacentPosition)) {
                return adjacentPosition;
            }
        }

        // If no suitable block is found adjacent to the chest, we check the blocks at a distance of 2 from the chest.
        for (Direction direction : dirs) {
            BlockPos fartherPosition = startingPosition.offset(direction, 2); // Going two blocks in each direction
            if (MinecraftPathfindingNode.canStandOn(this.getWorld(), fartherPosition)) {
                return fartherPosition;
            }
        }

        return null; // Return null if no suitable position is found
    }

    public World getWorld() {
        return player.getWorld();
    }

    public void equipAxe() {
        // Loop through the agent's inventory and check for any type of axe
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() instanceof AxeItem) {
                LOGGER.info("Equipping axe to agent. Slot " + i);
                player.setCurrentHand(Hand.MAIN_HAND);
                player.equipStack(EquipmentSlot.MAINHAND, stack);
                player.setCurrentHand(Hand.MAIN_HAND);
                ItemStack mainHandItem = player.getEquippedStack(EquipmentSlot.MAINHAND);
                LOGGER.info("Item in main hand after equipAxe: " + mainHandItem);
            }
        }
    }

    public static class ControlledPlayerSerializer implements JsonSerializer<ControlledPlayer>, JsonDeserializer<ControlledPlayer> {
        @Override
        public JsonElement serialize(ControlledPlayer src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject result = new JsonObject();
            result.add("role", new JsonPrimitive(src.role));
            result.add("username", new JsonPrimitive(src.getUsername()));
            return result;
        }
        @Override
        public ControlledPlayer deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
            if (json.isJsonObject()) {
                JsonObject jsonObject = json.getAsJsonObject();
                String role = jsonObject.get("role").getAsString();
                ControlledPlayer controlledPlayer = new ControlledPlayer();
                controlledPlayer.username = jsonObject.get("username").getAsString();
                controlledPlayer.setRole(role);
                return controlledPlayer;
            }
            throw new JsonParseException("Invalid ControlledPlayer JSON structure.");
        }
    }
}
