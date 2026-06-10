package rearth.drone.behaviour;

import rearth.drone.DroneServerData;
import rearth.util.Helpers;

import static rearth.drone.DroneController.SIMPLEX;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class PlayerSwarmBehaviour implements DroneBehaviour {

    private static final float SWARM_RANGE = 5.0f;

    private final DroneServerData drone;
    private final Player owner;

    public PlayerSwarmBehaviour(DroneServerData drone, Player owner) {
        this.drone = drone;
        this.owner = owner;
    }

    @Override
    public void tick() {

        if (owner.isRemoved()) {
            drone.setCurrentTask(null);
        }

        var distToPlayer = drone.currentPosition.distanceTo(owner.getEyePosition());

        if (distToPlayer > SWARM_RANGE) {
            drone.setTarget(owner.level(), getIdlePositionTarget());
        } else {
            drone.currentTargetPosition = getIdlePositionTarget();
            drone.nextTargetPosition = null;
        }
    }
    
    @Override
    public float getCurrentYaw() {
        var playerDist = drone.currentPosition.distanceTo(owner.getEyePosition());
        if (playerDist > 5) {
            return Helpers.calculateYaw(drone.currentPosition, owner.getEyePosition());
        }
        return owner.yHeadRot;
    }
    
    @Override
    public int getPriority() {
        return 1;
    }
    
    // override to provide a preferred position that will be tried before the overhead noise positions
    public Vec3 getDesiredPosition() {
        return null;
    }

    // circles overhead in a random manner, with slight Y variations.
    // falls back to a fixed overhead then a behind-player position if the
    // noise-driven spot is inside a block.
    public Vec3 getIdlePositionTarget() {
        var world = owner.level();
        var playerHead = owner.getEyePosition();
        var overheadCenter = playerHead.add(0, 0.5f, 0);

        var playerYaw = Math.toRadians(owner.yHeadRot - 90);
        var playerBackDir = new Vec3(Math.cos(playerYaw), 0, Math.sin(playerYaw)).normalize();
        
        var time = world.getGameTime();
        var sampledX = time / 100f;
        
        var x = SIMPLEX.getValue(sampledX, 0);
        var y = SIMPLEX.getValue(sampledX, 5000);
        var z = SIMPLEX.getValue(sampledX + 5000, 5000);

        var desired = getDesiredPosition();
        if (desired != null) {
            var noisyDesired = desired.add(new Vec3(x * 0.5, y / 6, z * 0.5));
            if (isPositionClearRadius(noisyDesired, 0.5))
                return noisyDesired;
            if (isPositionClearRadius(desired, 0.5))
                return desired;
        }
        
        var noiseOverhead = overheadCenter.add(new Vec3(x, y / 3, z)).add(playerBackDir.scale(0.9f));
        if (isPositionClearRadius(noiseOverhead, 0.5))
            return noiseOverhead;

        var fixedOverhead = playerHead.add(0, 1.2f, 0).add(playerBackDir.scale(0.9f));
        if (isPositionClearRadius(fixedOverhead, 0.3))
            return fixedOverhead;

        var directlyOverhead = playerHead.add(0, 0.6f, 0);
        if (isPositionClearRadius(directlyOverhead, 0.15))
            return directlyOverhead;

        var behindPlayer = owner.position().add(0, 0.9, 0).add(playerBackDir.scale(2.0f));
        if (isPositionClearRadius(behindPlayer, 0.15))
            return behindPlayer;

        return drone.currentPosition;
    }

    // checks a box of the given radius around pos for any solid collision shape
    private boolean isPositionClearRadius(Vec3 pos, double radius) {
        var box = new AABB(pos.x - radius, pos.y - radius, pos.z - radius, pos.x + radius, pos.y + radius,
                pos.z + radius);
        return owner.level().noCollision(box);
    }
}
