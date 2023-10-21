package com.example;


import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Reimplementation of net.minecraft.entity.ai.control.MoveControl;
 */
public class CustomMoveControl {
    private final ControlledPlayer agent;
    private double nextX;
    private double nextY;
    private double nextZ;
    private boolean hasTarget;

    public CustomMoveControl(ControlledPlayer agent) {
        this.agent = agent;
    }

    public void moveTo(double x, double y, double z) {
        this.nextX = x;
        this.nextY = y;
        this.nextZ = z;
        this.hasTarget= true;
    }

    public void tick() {
        if (!hasTarget) {
            return;
        }

        double speedMultiplier= 0.5; // Velocity modifier (you might want to adjust this)
        double distanceX = nextX - agent.player.getX();
        double distanceY = nextY - agent.player.getY();
        double distanceZ = nextZ - agent.player.getZ();
        double distance = Math.sqrt(distanceX * distanceX + distanceY * distanceY + distanceZ * distanceZ);


        Vec3d currentPos = agent.player.getPos();
        Vec3d nextPos = new Vec3d( nextX, nextY, nextZ);

        if (shouldJumpToNextNode(currentPos, nextPos)) {
            agent.player.jump();
        }

        Vec3d newVel = new Vec3d(distanceX, distanceY, distanceZ).multiply(speedMultiplier / distance);
        agent.player.setVelocity(newVel);

        if (agent.getBlockPos().isWithinDistance(new BlockPos((int) nextX, (int) nextY, (int) nextZ),  1.5)) { // Close enough to target
            this.hasTarget = false; // Reached the mark
        }
    }

    private enum PathNodeType {
        DANGER_FIRE,
        DANGER_OTHER,
        WALKABLE_DOOR,
        DEFAULT
    }

    private PathNodeType getPathNodeType(BlockPos pos) {
        BlockState blockState = agent.player.getWorld().getBlockState(pos);
        Block block = blockState.getBlock();

        // Here it comes how we can decide the block type, you need to specify your conditions for each enum

        if(blockState.isIn(BlockTags.FIRE)) { // Any kind of fire block
            return PathNodeType.DANGER_FIRE;
        } else if(block instanceof DoorBlock && blockState.get(DoorBlock.OPEN)) { // Any open door
            return PathNodeType.WALKABLE_DOOR;
        } else if(!blockState.canPathfindThrough(agent.player.getWorld(), pos, NavigationType.LAND)) { // Any kind of block that mobs can't pass
            return PathNodeType.DANGER_OTHER;
        }

        return PathNodeType.DEFAULT;
    }

    private boolean canJumpToNext(BlockPos blockPos) {
        return getPathNodeType(blockPos) != PathNodeType.DANGER_FIRE && getPathNodeType(blockPos) != PathNodeType.DANGER_OTHER && getPathNodeType(blockPos) != PathNodeType.WALKABLE_DOOR;
    }

    private boolean shouldJumpToNextNode(Vec3d currentPos, Vec3d nextPos) {
        BlockPos nextBlockPos = new BlockPos((int) nextPos.x, (int) nextPos.y, (int) nextPos.z);
        if (!canJumpToNext(nextBlockPos.up()) && nextPos.y - currentPos.y > 0.5D) {
            return canJumpToNext(nextBlockPos.up(2));
        } else {
            return false;
        }
    }

//    private boolean shouldJumpToNextNode(Vec3d currentPos, Vec3d nextPos, Vec3d nextNextPos) {
//        if (nextPos.distanceTo(currentPos) > 2.0) {
//            return false;
//        } else {
//            Vec3d vec3d3 = nextPos.subtract(currentPos);
//            Vec3d vec3d4 = nextNextPos.subtract(currentPos);
//            double d = vec3d3.lengthSquared();
//            double e = vec3d4.lengthSquared();
//            boolean bl = e < d;
//            boolean bl2 = d < 0.5;
//            if (!bl && !bl2) {
//                return false;
//            } else {
//                Vec3d vec3d5 = vec3d3.normalize();
//                Vec3d vec3d6 = vec3d4.normalize();
//                return vec3d6.dotProduct(vec3d5) < 0.0;
//            }
//        }
//    }
}
