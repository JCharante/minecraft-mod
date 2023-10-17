package com.example;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AgentEntity extends PathAwareEntity {
    public static final Logger LOGGER = LoggerFactory.getLogger("mymod");
    private final SimpleInventory inventory;
    private AgentRole currentRole;

    public AgentEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
        this.inventory = new SimpleInventory(9);  // Inventory with 9 slots
        this.setCustomName(Text.of("Name Missing"));
        this.setCustomNameVisible(true);
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
        if (this.currentRole == null) {
            this.currentRole = AgentRole.LUMBERJACK;
            this.addGoals();
        }
    }

    private void addGoals() {
        LOGGER.info("addGoals method called." + this.currentRole);

        if (this.currentRole == AgentRole.LUMBERJACK) {
            LOGGER.info("Set goals for Lumberjack.");
            this.goalSelector.add(1, new GetEquipmentGoal(this));
        } else {
            LOGGER.info("Didn't set any goals.");
        }
    }


    public void sayInChat(String message) {

        if (!this.getWorld().isClient) {
            MinecraftServer server = this.getWorld().getServer();
            if (server != null) {
                MutableText text = (MutableText) this.getDisplayName();
                text.append(": ");
                text.append(message);
                server.getPlayerManager().broadcast(text, false);
            }
        }
    }

}
