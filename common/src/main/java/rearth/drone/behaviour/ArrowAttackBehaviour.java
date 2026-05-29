package rearth.drone.behaviour;

import net.minecraft.block.BlockState;
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
import net.minecraft.util.math.Vec3i;
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
    
    private int attackCooldown = 0;
    
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
        
        if (attackCooldown <= 0) {
            
            attackCooldown = getAttackCooldown();
            
            performAttack(dist, shotFrom);
            
        } else {
            attackCooldown--;
        }
        
    }
    
    public void performAttack(double dist, Vec3d shotFrom) {
        
        // shoot arrow
        var world = owner.getWorld();
        var stack = new ItemStack(Items.ARROW);
        var targetPos = target.getEyePos().add(0, dist / 10f, 0);   // adjust target slightly up for longer distances to hit
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
    }
    
    public int getAttackCooldown() {
        return 24;
    }
    
    public void finishTask() {
        drone.setCurrentTask(new PlayerSwarmBehaviour(drone, owner));
    }

    @Override
    public Vec3d getDesiredPosition() {
        if (target == null || target.isRemoved() || !target.isAlive())
            return null;
        var direction = target.getEyePos().subtract(owner.getEyePos()).normalize();
        return owner.getEyePos().add(0, 0.5f, 0).add(direction.multiply(0.9f));
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
            targets = targets.stream().filter(target -> target.isAlive() && !target.isRemoved() && target instanceof Monster).toList();
            
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
