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

    private final double cost;
    private static final Direction[] dirs = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};


    public MinecraftPathfindingNode(World world, BlockPos position, double cost) {
        this.world = world;
        this.position = position;
        this.cost = cost;
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
    public double getCost() {
        return this.cost;
    }

    @Override
    public List<PathfindingNode> getAdjacentNodes() {
        List<PathfindingNode> adjacentNodes = new ArrayList<>();

        for (Direction direction : dirs) {
            BlockPos adjacentPosition = getNodeInDirection(direction);
            if (adjacentPosition == null) continue;
            if (adjacentPosition.getY() > this.getY() && !canJumpFrom(this.world, this.position)) continue;
            if (adjacentPosition.getY() < this.getY() && !canJumpFrom(this.world, adjacentPosition)) continue;

            double adjacentCost = 0.5;
            if (adjacentPosition.getY() > this.getY()) adjacentCost += 0.3; // uphill penalty
            if (adjacentPosition.getY() < this.getY()) adjacentCost += 0.1; // downhill is easier
            BlockState state = world.getBlockState(adjacentPosition);
            boolean goodRoadMaterial = state.isOf(Blocks.BIRCH_WOOD)
                    || state.isOf(Blocks.BIRCH_STAIRS)
                    || state.isOf(Blocks.OAK_WOOD)
                    || state.isOf(Blocks.OAK_STAIRS)
                    || state.isOf(Blocks.JUNGLE_WOOD)
                    || state.isOf(Blocks.JUNGLE_STAIRS)
                    || state.isOf(Blocks.STONE_STAIRS)
                    || state.isOf(Blocks.COBBLESTONE)
                    || state.isOf(Blocks.COBBLESTONE_STAIRS);
            if (!goodRoadMaterial) adjacentCost += 0.5;

            adjacentNodes.add(new MinecraftPathfindingNode(world, adjacentPosition, adjacentCost));
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

    public boolean canStandOn() {
        return canStandOn(this.world, this.position);
    }

    public boolean notStrictCanStandOn() {
        return notStrictCanStandOn(this.world, this.position);
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

    public static boolean notStrictCanStandOn(World world, BlockPos pos) {
        return canStandOn(world, pos) || canStandOn(world, pos.down());
    }


    /**
     * Like canStandOn, but checks for 3 blocks of air above
     * @param world
     * @param pos
     * @return
     */
    public static boolean canJumpFrom(World world, BlockPos pos) {
        return world.getBlockState(pos).isSolidBlock(world, pos)
                && isPassable(world, pos.up())
                && isPassable(world, pos.up(2))
                && isPassable(world, pos.up(3));
    }

    public String toString() {
        return position.toShortString();
    }

}