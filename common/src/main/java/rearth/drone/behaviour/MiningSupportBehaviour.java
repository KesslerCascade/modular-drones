package rearth.drone.behaviour;

import net.minecraft.block.BlockState;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import rearth.Drones;
import rearth.drone.DroneServerData;
import rearth.drone.RecordedBlock;
import rearth.init.TagContent;
import rearth.util.Helpers;

import java.util.HashMap;

// lets the drone support with mining a single block.
// this is done by moving the block to the side of the block,
// and then increasing the player mining speed
public class MiningSupportBehaviour implements DroneBehaviour {
    
    private static final int MAX_RANGE = 10;
    private static final float REACH = 0.6f;
    private static final int WAIT_TIME = 14;
    
    private final BlockPos target;
    private final PlayerEntity owner;
    private final DroneServerData drone;
    private final BlockState startState;
    
    private SupportPhase phase;
    
    public MiningSupportBehaviour(BlockPos target, PlayerEntity owner, DroneServerData drone) {
        this.target = target;
        this.owner = owner;
        this.drone = drone;
        this.phase = SupportPhase.MOVING_IN;
        this.startState = owner.getWorld().getBlockState(target);
    }
    
    @Override
    public void tick() {
        
        if (!phase.equals(SupportPhase.WAITING)) {
            if (!owner.getWorld().getBlockState(target).equals(startState)) {
                finishMining();
                return;
            }
            
            if (owner instanceof ServerPlayerEntity serverPlayer) {
                var currentlyMiningPos = serverPlayer.interactionManager.miningPos;
                var stillMining = serverPlayer.interactionManager.mining;
                
                if (!currentlyMiningPos.equals(target) || !stillMining) {
                    finishMining();
                    return;
                }
            }
        }
        
        switch (phase) {
            case MOVING_IN -> {

                drone.setTarget(owner.getWorld(), getTargetPosition());
                
                var ownerDist = owner.getEyePos().distanceTo(drone.currentPosition);
                if (ownerDist > MAX_RANGE) {
                    finishTask();
                    return;
                }
                
                var targetDist = drone.currentPosition.distanceTo(drone.currentTargetPosition);
                if (targetDist < REACH) {
                    phase = SupportPhase.SUPPORTING;
                    
                    var miningInstance = owner.getAttributeInstance(EntityAttributes.PLAYER_BLOCK_BREAK_SPEED);
                    if (miningInstance != null && !miningInstance.hasModifier(Drones.id("drone_mine_bonus")))
                        miningInstance.addTemporaryModifier(new EntityAttributeModifier(Drones.id("drone_mine_bonus"), 5f, EntityAttributeModifier.Operation.ADD_VALUE));
                }
                
                
            }
            case SUPPORTING -> {
                drone.setTarget(owner.getWorld(), getTargetPosition());
                
                var ownerDist = owner.getEyePos().distanceTo(drone.currentPosition);
                if (ownerDist > MAX_RANGE) {
                    finishTask();
                }
            }
            case WAITING -> {
                if (drone.actionCooldown == 0)
                    finishTask();
            }
        }
        
    }
    
    private void finishMining() {
        phase = SupportPhase.WAITING;
        drone.actionCooldown = WAIT_TIME;
    }
    
    @Override
    public float getExtraRoll() {
        
        if (phase == SupportPhase.SUPPORTING) {
            var time = owner.getWorld().getTime();
            return (float) (Math.sin(time / 2f) * 20);
        }
        
        return DroneBehaviour.super.getExtraRoll();
    }
    
    @Override
    public void onStopped() {
        
        // delay this by one tick so the effect is still there when the block is being broken, this avoids weird sync issues
        Drones.DELAYED_ACTIONS.add(() -> {
            var miningInstance = owner.getAttributeInstance(EntityAttributes.PLAYER_BLOCK_BREAK_SPEED);
            if (miningInstance != null)
                miningInstance.removeModifier(Drones.id("drone_mine_bonus"));
        });
    }
    
    public void finishTask() {
        drone.setIdle(owner, drone);
    }
    
    @Override
    public float getCurrentYaw() {
        return Helpers.calculateYaw(drone.currentPosition, target.toCenterPos());
    }
    
    @Override
    public int getPriority() {
        return phase == SupportPhase.WAITING ? 3 : 20;
    }
    
    private Vec3d getTargetPosition() {
        
        var playerPos = owner.getEyePos();
        var blockCenter = target.toCenterPos();
        
        var playerDir = blockCenter.subtract(playerPos).normalize();
        var playerUp = new Vec3d(0, 1, 0);
        var sideDirection = playerDir.crossProduct(playerUp);
        var otherSideDirection = sideDirection.multiply(-1f);
        
        var potentialPosA = blockCenter.add(sideDirection).add(playerDir.multiply(-1));
        var potentialPosB = blockCenter.add(otherSideDirection).add(playerDir.multiply(-1));
        var potentialPosC = blockCenter.add(playerDir.multiply(-2)).add(0, -0.6, 0);
        
        if (Helpers.isPositionAvailable(owner.getWorld(), potentialPosA, playerPos)) {
            return potentialPosA;
        } else if (Helpers.isPositionAvailable(owner.getWorld(), potentialPosB, playerPos)) {
            return potentialPosB;
        } else {
            return potentialPosC;
        }
        
    }
    
    private enum SupportPhase {
        MOVING_IN, SUPPORTING, WAITING
    }
    
    public static boolean isValidMiningTarget(World world, BlockPos pos) {
        var state = world.getBlockState(pos);
        return !state.isAir() && !state.isLiquid() && state.getHardness(world, pos) > 0.1f;
    }
    
    public static boolean isValid(RecordedBlock block, HashMap<Vec3i, BlockState> frame) {
        // is valid when facing forward (south) and not blocked
        
        var blockMatches = block.state().isIn(TagContent.MINING_TOOLS);
        if (!blockMatches) return false;
        
        // ensure front is free
        for (int i = 1; i < 8; i++) {
            if (frame.containsKey(block.localPos().south(i))) return false;
        }
        
        return true;
        
    }
}
