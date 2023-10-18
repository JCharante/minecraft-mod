package com.example;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class MinecraftPathfindingAdapter {
    private final World world;

    public MinecraftPathfindingAdapter(World world) {
        this.world = world;
    }

    public MinecraftPathfindingNode getNodeAt(BlockPos position) {
        return new MinecraftPathfindingNode(world, position);
    }

    public MinecraftPathfindingNode getNodeAt(PathfindingNode node) {
        return new MinecraftPathfindingNode(world, new BlockPos((int) node.getX(), (int) node.getY(), (int) node.getZ()));
    }
}
