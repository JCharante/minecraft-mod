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
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
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
        this.setCustomName(Text.of("Name Missing"));
        this.setCustomNameVisible(true);
        getNavigation().setCanSwim(true);
        getNavigation().setSpeed(0.5);
        this.movementController = new MovementController(this, world);
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
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        NbtList inventoryList = nbt.getList("Inventory", 10);
        for (int i = 0; i < inventoryList.size(); i++) {
            this.inventory.setStack(i, ItemStack.fromNbt(inventoryList.getCompound(i)));
        }
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

    public void equipAxe() {
        LOGGER.info("Equipping axe to agent.");
        // Loop through the agent's inventory and check for any type of axe
        for (int i = 0; i < this.getInventory().size(); i++) {
            ItemStack stack = this.getInventory().getStack(i);
            if (stack.getItem() instanceof AxeItem) {
                this.equipStack(EquipmentSlot.MAINHAND, stack);
            }
        }
    }

//    public void pickupItemsNearby() {
//        List<ItemEntity> itemsNearby = this.getWorld().getEntitiesByClass(ItemEntity.class, this.getBoundingBox().expand(1.5, 1.0, 1.5), itemEntity -> true);
//        for (ItemEntity itemEntity : itemsNearby) {
//            this.sendPickup(itemEntity, 1);
//            itemEntity.remove(UNLOADED_WITH_PLAYER);
//        }
//    }

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

    public void navTo(BlockPos targetBlockPos) {
        LOGGER.info("Agent navTo " + targetBlockPos);
        if (targetBlockPos == null) {
            LOGGER.info("You tried to make me navTo null!");
            return;
        };
        movementController.moveTo(targetBlockPos);
        LOGGER.info("Agent navTo " + targetBlockPos);
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

    public BlockPos findChest() {
        BlockPos currentPosition = this.getBlockPos();
        int searchRadius = 50;

        // Loop over a small area around the entity to check for chests
        for (BlockPos pos : BlockPos.iterateOutwards(currentPosition, searchRadius, 20, searchRadius)) {
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
