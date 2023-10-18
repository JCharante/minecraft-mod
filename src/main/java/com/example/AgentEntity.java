package com.example;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static net.minecraft.entity.Entity.RemovalReason.UNLOADED_WITH_PLAYER;

public class AgentEntity extends PathAwareEntity {
    public static final Logger LOGGER = LoggerFactory.getLogger("mymod");
    private final SimpleInventory inventory;
    private AgentRole currentRole;
    private MovementController movementController;

    private String name;


    public AgentEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
        this.inventory = new SimpleInventory(9);  // Inventory with 9 slots
        this.setCustomNameVisible(true);
        getNavigation().setCanSwim(true);
        getNavigation().setSpeed(0.5);
        this.movementController = new MovementController(this, world);
        this.setPersistent();
    }


    public void setRealName(String name) {
        this.name = name;
    }

    public String getRealName() {
        return this.name;
    }

    public SimpleInventory getInventory() {
        return this.inventory;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        NbtList inventoryList = new NbtList();
        for (int i = 0; i < this.inventory.size(); i++) {
            NbtCompound itemNbt = new NbtCompound();
            this.inventory.getStack(i).writeNbt(itemNbt);
            inventoryList.add(itemNbt);
        }
        nbt.put("Inventory", inventoryList);
        if (this.name != null) {
            nbt.put("AgentRealName", NbtString.of(this.name));
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        NbtList inventoryList = nbt.getList("Inventory", 10);
        for (int i = 0; i < inventoryList.size(); i++) {
            this.inventory.setStack(i, ItemStack.fromNbt(inventoryList.getCompound(i)));
        }
        if (nbt.contains("AgentRealName")) {
            this.name = nbt.getString("AgentRealName");
        }
    }


    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        // Add logging or chat message here to see when the entity is removed and under what conditions
        sayInChat("I was removed!");
        LOGGER.info("AgentEntity removed.");
    }



    @Override
    public void tick() {
        super.tick();
        if (!this.getWorld().isClient) {
            MinecraftServer server = this.getWorld().getServer();
            if (server != null) {
                movementController.tick();
                this.pickupItemsNearby();
                if (this.currentRole == null) {
                    this.currentRole = AgentRole.LUMBERJACK;
                    this.addGoals();
                }
            }
        }
    }

    private void addGoals() {
        LOGGER.info("addGoals method called." + this.currentRole);

        if (this.currentRole == AgentRole.LUMBERJACK) {
            LOGGER.info("Set goals for Lumberjack.");
            this.goalSelector.add(1, new GetEquipmentGoal(this));
            this.goalSelector.add(2, new DepositWoodGoal(this));
            this.goalSelector.add(3, new ChopTreeGoal(this));
        } else {
            LOGGER.info("Didn't set any goals.");
        }
    }

    public void sayInChat(String message) {

        if (!this.getWorld().isClient) {
            MinecraftServer server = this.getWorld().getServer();
            if (server != null) {
                Text text = Text.literal(this.name + ": " + message);
                server.getPlayerManager().broadcast(text, false);
            }
        }
    }

    public boolean hasAxe() {
        // Loop through the agent's inventory and check for any type of axe
        for (int i = 0; i < this.getInventory().size(); i++) {
            ItemStack stack = this.getInventory().getStack(i);
            if (stack.getItem() instanceof AxeItem) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void checkDespawn() {
        return;
    }

    public void equipAxe() {
        // Loop through the agent's inventory and check for any type of axe
        for (int i = 0; i < this.getInventory().size(); i++) {
            ItemStack stack = this.getInventory().getStack(i);
            if (stack.getItem() instanceof AxeItem) {
                LOGGER.info("Equipping axe to agent. Slot " + i);
                this.setCurrentHand(Hand.MAIN_HAND);
                this.equipStack(EquipmentSlot.MAINHAND, stack);
                this.setCurrentHand(Hand.MAIN_HAND);
                ItemStack mainHandItem = this.getEquippedStack(EquipmentSlot.MAINHAND);
                LOGGER.info("Item in main hand after equipAxe: " + mainHandItem);
            }
        }
    }

    public void pickupItemsNearby() {
        List<ItemEntity> itemsNearby = this.getWorld().getEntitiesByClass(ItemEntity.class, this.getBoundingBox().expand(1.5, 1.0, 1.5), itemEntity -> true);
        for (ItemEntity itemEntity : itemsNearby) {
            ItemStack itemStack = itemEntity.getStack();

            // Add the item to the inventory. Returns any items that couldn't fit.
            ItemStack leftover = this.inventory.addStack(itemStack);

            // If all items were added to the inventory, remove the item entity. Otherwise, update its stack.
            if (leftover.isEmpty()) {
                itemEntity.remove(UNLOADED_WITH_PLAYER);
            } else {
                itemEntity.setStack(leftover);
            }

            // For the pickup animation and sound.
            this.sendPickup(itemEntity, itemStack.getCount() - leftover.getCount());
        }
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
        this.teleport(ground.getX(), ground.getY() + 2, ground.getZ());
        sayInChat("Sorry, but I had to teleport to the ground near the chest after finding myself stuck.");
        return false;
    }

    public boolean navTo(BlockPos targetBlockPos) {
        return this.navTo(targetBlockPos, false);
    }

    public void stopNav() {
        movementController.clear();
    }

    @Override
    protected void dropEquipment(DamageSource source, int lootingMultiplier, boolean allowDrops) {
        super.dropEquipment(source, lootingMultiplier, allowDrops);
        sayInChat("I died!");

        // Loop through the agent's inventory and drop all items
        for (int i = 0; i < this.getInventory().size(); i++) {
            ItemStack stack = this.getInventory().getStack(i);
            if (!stack.isEmpty()) {
                this.dropStack(stack);
            }
        }
    }

    @Override
    public void onDeath(DamageSource source) {
        super.onDeath(source);
        sayInChat("I died due to " + source.getName() + "!");
    }



    public BlockPos findChest() {
        BlockPos currentPosition = this.getBlockPos();
        int searchRadius = 100;

        // Loop over a small area around the entity to check for chests
        for (BlockPos pos : BlockPos.iterateOutwards(currentPosition, searchRadius, 30, searchRadius)) {
            BlockState state = this.getWorld().getBlockState(pos);
            Block block = state.getBlock();

            if (block instanceof ChestBlock) {
                BlockEntity be = this.getWorld().getBlockEntity(pos);
                return pos;
            }
        }
        return null;
    }

    public void setStatus(String status) {
        this.setCustomName(Text.literal(this.getRealName() + " (" + status + ")"));
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

}
