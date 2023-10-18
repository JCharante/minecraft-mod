package com.example;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class MinecraftPathfindingNode implements PathfindingNode {
    private final World world;
    private final BlockPos position;
    private static final Direction[] dirs = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};


    public MinecraftPathfindingNode(World world, BlockPos position) {
        this.world = world;
        this.position = position;
    }

    @Override
    public double getX() {
        return position.getX();
    }

    @Override
    public double getY() {
        return position.getY();
    }

    @Override
    public double getZ() {
        return position.getZ();
    }

    public BlockPos getBlockPos() {
        return position;
    }

    @Override
    public List<PathfindingNode> getAdjacentNodes() {
        List<PathfindingNode> adjacentNodes = new ArrayList<>();

        for (Direction direction : dirs) {
            BlockPos adjacentPosition = getNodeInDirection(direction);
            if (adjacentPosition != null) {
                adjacentNodes.add(new MinecraftPathfindingNode(world, adjacentPosition));
            }
        }

        return adjacentNodes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MinecraftPathfindingNode that = (MinecraftPathfindingNode) o;
        return this.getX() == that.getX() && this.getY() == that.getY() && this.getZ() == that.getZ();
    }

    private BlockPos getNodeInDirection(Direction direction)  {
        if (canStandOn(this.position.offset(direction))) {
            return this.position.offset(direction);
        } else if (canStandOn(this.position.offset(direction).up())) {
            return this.position.offset(direction).up();
        } else if (canStandOn(this.position.offset(direction).down())) {
            return this.position.offset(direction).down();
        } else {
            return null;
        }
    }

    public boolean canStandOn(BlockPos pos) {
        return canStandOn(this.world, pos);
    }


    public static boolean isPassable(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return state.isAir()
                || state.isOf(Blocks.GRASS)
                || state.isOf(Blocks.TALL_GRASS)
                // || state.isOf(Blocks.FLOWERS)  // You may need to list individual flower types
                || state.isOf(Blocks.TORCH);
    }


    public static boolean canStandOn(World world, BlockPos pos) {
        return world.getBlockState(pos).isSolidBlock(world, pos)
                && isPassable(world, pos.up()) && isPassable(world, pos.up(2));
    }

}