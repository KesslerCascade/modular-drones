package rearth.drone.behaviour;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import rearth.drone.DroneServerData;
import rearth.util.Helpers;

import static rearth.drone.DroneController.SIMPLEX;

public class PlayerSwarmBehaviour implements DroneBehaviour {
    
    private final DroneServerData drone;
    private final PlayerEntity owner;
    
    public PlayerSwarmBehaviour(DroneServerData drone, PlayerEntity owner) {
        this.drone = drone;
        this.owner = owner;
    }
    
    @Override
    public void tick() {
        
        if (owner.isRemoved()) {
            drone.setCurrentTask(null);
        }
        
        drone.targetPosition = getIdlePositionTarget();
    }
    
    @Override
    public float getCurrentYaw() {
        var playerDist = drone.currentPosition.distanceTo(owner.getEyePos());
        if (playerDist > 5) {
            return Helpers.calculateYaw(drone.currentPosition, owner.getEyePos());
        }
        return owner.headYaw;
    }
    
    @Override
    public int getPriority() {
        return 1;
    }
    
    // circles overhead in a random manner, with slight Y variations.
    // falls back to a fixed overhead then a behind-player position if the
    // noise-driven spot is inside a block.
    public Vec3d getIdlePositionTarget() {
        var world = owner.getWorld();
        var playerHead = owner.getEyePos();
        var overheadCenter = playerHead.add(0, 0.5f, 0);
        
        var playerYaw = Math.toRadians(owner.headYaw - 90);
        var playerBackDir = new Vec3d(Math.cos(playerYaw), 0, Math.sin(playerYaw)).normalize();
        
        var time = world.getTime();
        var sampledX = time / 100f;
        
        var x = SIMPLEX.sample(sampledX, 0);
        var y = SIMPLEX.sample(sampledX, 5000);
        var z = SIMPLEX.sample(sampledX + 5000, 5000);
        
        var noiseOverhead = overheadCenter.add(new Vec3d(x, y / 3, z)).add(playerBackDir.multiply(0.9f));
        if (isPositionClearRadius(noiseOverhead, 0.5))
            return noiseOverhead;

        var fixedOverhead = playerHead.add(0, 1.2f, 0).add(playerBackDir.multiply(0.9f));
        if (isPositionClearRadius(fixedOverhead, 0.3))
            return fixedOverhead;

        var directlyOverhead = playerHead.add(0, 0.6f, 0);
        if (isPositionClearRadius(directlyOverhead, 0.15))
            return directlyOverhead;

        var behindPlayer = owner.getPos().add(0, 0.9, 0).add(playerBackDir.multiply(2.0f));
        if (isPositionClearRadius(behindPlayer, 0.15))
            return behindPlayer;

        return drone.currentPosition;
    }

    // checks a box of the given radius around pos for any solid collision shape
    private boolean isPositionClearRadius(Vec3d pos, double radius) {
        var box = new Box(pos.x - radius, pos.y - radius, pos.z - radius, pos.x + radius, pos.y + radius,
                pos.z + radius);
        return owner.getWorld().isSpaceEmpty(box);
    }
}
