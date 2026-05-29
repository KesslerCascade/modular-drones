package rearth.drone.behaviour;

import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.RaycastContext;
import rearth.drone.DroneServerData;
import rearth.drone.RecordedBlock;
import rearth.init.TagContent;
import rearth.util.Helpers;

import java.util.Comparator;
import java.util.HashMap;

// an instance of a behaviour to attack one specific entity.
// consists of 3 phases: move in, attack, move home
public class ArrowAttackBehaviour extends PlayerSwarmBehaviour {
    
    private static final int MAX_RANGE = 25;
    
    public final LivingEntity target;
    public final PlayerEntity owner;
    public final DroneServerData drone;

    public ArrowAttackBehaviour(LivingEntity target, PlayerEntity owner, DroneServerData drone) {
        super(drone, owner);
        this.target = target;
        this.owner = owner;
        this.drone = drone;
    }
    
    @Override
    public void tick() {
        
        super.tick();
        
        if (target.isRemoved() || !target.isAlive() || !target.isAttackable()) finishTask();
        
        var shotFrom = this.owner.getEyePos().add(0, 1.2, 0);
        var dist = shotFrom.distanceTo(target.getEyePos());
        if (dist > MAX_RANGE) finishTask();
        
        if (drone.actionCooldown == 0) {
            if (performAttack(dist, shotFrom)) {
                drone.actionCooldown = getAttackCooldown();
            }
        }
        
    }
    
    public boolean performAttack(double dist, Vec3d shotFrom) {
        
        var world = owner.getWorld();
        var targetPos = target.getEyePos().add(0, dist / 10f, 0); // adjust target slightly up for longer distances to
                                                                  // hit

        // abort if drone no longer has LOS to target (check both actual entity pos and
        // adjusted aim pos)
        var losContextActual = new RaycastContext(drone.currentPosition, target.getEyePos(),
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, ShapeContext.absent());
        var losContextAdjusted = new RaycastContext(drone.currentPosition, targetPos, RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE, ShapeContext.absent());
        if (world.raycast(losContextActual).getType() != HitResult.Type.MISS
                || world.raycast(losContextAdjusted).getType() != HitResult.Type.MISS)
            return false;

        // abort if owner is in the line of fire (check both aim direction and landing
        // point to bracket the arc)
        var ownerBox = owner.getBoundingBox().expand(0.3);
        var aimDir = targetPos.subtract(shotFrom).normalize();
        var landDir = target.getEyePos().subtract(shotFrom).normalize();
        if (ownerBox.raycast(shotFrom, shotFrom.add(aimDir.multiply(dist + 5))).isPresent()
                || ownerBox.raycast(shotFrom, shotFrom.add(landDir.multiply(dist + 5))).isPresent())
            return false;

        // shoot arrow
        var stack = new ItemStack(Items.ARROW);
        var offset = targetPos.subtract(shotFrom);
        var initialVelocity = offset.normalize().multiply(2);
        
        var arrowEntity = new ArrowEntity(world, shotFrom.x, shotFrom.y, shotFrom.z, stack, null);
        arrowEntity.setVelocity(initialVelocity);
        world.spawnEntity(arrowEntity);
        
        // particle
        if (owner.getWorld() instanceof ServerWorld serverWorld) {
            var forward = target.getEyePos().subtract(drone.currentPosition).normalize();
            var particleStart = drone.currentPosition.add(forward.multiply(0.3f));
            serverWorld.spawnParticles(ParticleTypes.SMALL_GUST, particleStart.x, particleStart.y, particleStart.z, 1, forward.x, forward.y, forward.z, 0.2f);
        }

        return true;
    }
    
    public int getAttackCooldown() {
        return 24;
    }
    
    public void finishTask() {
        drone.setCurrentTask(new PlayerSwarmBehaviour(drone, owner));
    }
    
    @Override
    public float getCurrentYaw() {
        return Helpers.calculateYaw(drone.currentPosition, target.getEyePos());
    }
    
    @Override
    public int getPriority() {
        return 55;
    }
    
    public static class ArrowAttackSensor implements DroneSensor {
        
        @Override
        public int getPriority() {
            return 35;
        }
        
        @Override
        public boolean sense(DroneServerData drone, PlayerEntity player) {
            
            var world = player.getWorld();
            var entityRange = getTargetingRange();
            var playerHead = player.getEyePos();
            
            var targets = world.getEntitiesByClass(LivingEntity.class, new Box(playerHead.x - entityRange, playerHead.y - entityRange, playerHead.z - entityRange, playerHead.x + entityRange, playerHead.y + entityRange, playerHead.z + entityRange), EntityPredicates.VALID_LIVING_ENTITY.and(EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR));
            targets.sort(Comparator.comparingDouble((entity) -> entity.squaredDistanceTo(playerHead)));
            targets = targets.stream()
                    .filter(target -> target.isAlive() && !target.isRemoved() && target instanceof Monster)
                    .filter(target -> {
                        var losContext = new RaycastContext(playerHead, target.getEyePos(),
                                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE,
                                ShapeContext.absent());
                        return world.raycast(losContext).getType() == HitResult.Type.MISS;
                    })
                    .toList();
            
            if (!targets.isEmpty()) {
                onTargetFound(drone, player, targets.getFirst());
                return true;
            }
            
            return false;
        }
        
        public int getTargetingRange() {
            return 16;
        }
        
        public void onTargetFound(DroneServerData drone, PlayerEntity player, LivingEntity target) {
            drone.setCurrentTask(new ArrowAttackBehaviour(target, player, drone));
        }
    }
    
    public static boolean isValid(RecordedBlock block, HashMap<Vec3i, BlockState> frame) {
        // is valid when facing forward (south?) and not blocked
        
        var blockMatches = block.state().isIn(TagContent.ARROW_LAUNCHER);
        if (!blockMatches) return false;
        
        // ensure front is free
        for (int i = 1; i < 8; i++) {
            if (frame.containsKey(block.localPos().south(i))) return false;
        }
        
        return true;
        
    }
    
}
