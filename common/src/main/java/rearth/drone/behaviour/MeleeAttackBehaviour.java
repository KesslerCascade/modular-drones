package rearth.drone.behaviour;

import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3i;
import rearth.drone.DroneServerData;
import rearth.drone.RecordedBlock;
import rearth.init.TagContent;
import rearth.util.Helpers;

import java.util.Comparator;
import java.util.HashMap;

// an instance of a behaviour to attack one specific entity.
// consists of 3 phases: move in, attack, move home
public class MeleeAttackBehaviour implements DroneBehaviour {
    
    private static final int MAX_RANGE = 25;
    private static final float HIT_RANGE = 1.25f;
    private static final int ATTACK_COOLDOWN = 21;
    
    private final LivingEntity target;
    private final PlayerEntity owner;
    private final DroneServerData drone;
    
    private AttackPhase phase;
    
    public MeleeAttackBehaviour(LivingEntity target, PlayerEntity owner, DroneServerData drone) {
        this.target = target;
        this.owner = owner;
        this.drone = drone;
        this.phase = AttackPhase.MOVING_IN;
    }
    
    @Override
    public void tick() {
        
        switch (phase) {
            
            // sets target to entity, and if too far / close enough updates phase
            case MOVING_IN -> {
                
                drone.targetPosition = target.getEyePos();
                
                var dist = drone.currentPosition.distanceTo(target.getEyePos());
                var playerDist = drone.currentPosition.distanceTo(owner.getEyePos());
                
                if (dist > MAX_RANGE || playerDist > MAX_RANGE) {
                    phase = AttackPhase.MOVING_HOME;
                } else if (dist < HIT_RANGE) {
                    phase = AttackPhase.ATTACKING;
                }
                
            }
            
            // keeps attacking the entity after a specific cooldown
            case ATTACKING -> {
                
                var dist = drone.currentPosition.distanceTo(target.getEyePos());
                if (dist > HIT_RANGE * 2) {
                    phase = AttackPhase.MOVING_IN;
                    return;
                }
                
                if (!target.isAttackable() || !target.isAlive() || target.isRemoved()) {
                    phase = AttackPhase.MOVING_HOME;
                    return;
                }
                
                drone.targetPosition = target.getEyePos();
                if (drone.actionCooldown == 0) {
                    // do attack
                    var damage = 2; // todo
                    target.damage(new DamageSource(owner.getWorld().getRegistryManager().get(RegistryKeys.DAMAGE_TYPE).entryOf(DamageTypes.PLAYER_ATTACK), owner), damage);
                    drone.actionCooldown = ATTACK_COOLDOWN;
                    
                    if (owner.getWorld() instanceof ServerWorld serverWorld) {
                        var middle = drone.currentPosition.add(target.getEyePos()).multiply(0.5f);
                        var forward = target.getEyePos().subtract(drone.currentPosition).normalize();
                        serverWorld.spawnParticles(ParticleTypes.SWEEP_ATTACK, middle.x, middle.y, middle.z, 1, forward.x, forward.y, forward.z, 0.2f);
                    }
                }
                
            }
            case MOVING_HOME -> {
                drone.targetPosition = owner.getEyePos().add(0, 0.5, 0);
                var dist = drone.currentPosition.distanceTo(owner.getEyePos());
                if (dist < HIT_RANGE * 2) {
                    this.finishTask();
                }
            }
        }
        
    }
    
    public void finishTask() {
        drone.setCurrentTask(new PlayerSwarmBehaviour(drone, owner));
    }
    
    @Override
    public float getCurrentYaw() {
        
        if (phase == AttackPhase.MOVING_HOME)
            return Helpers.calculateYaw(drone.currentPosition, owner.getEyePos());
        
        if (phase == AttackPhase.ATTACKING) {
            var progress = drone.actionCooldown / (float) ATTACK_COOLDOWN;
            return Helpers.calculateYaw(drone.currentPosition, target.getEyePos()) + progress * 90;
        }
        
        return Helpers.calculateYaw(drone.currentPosition, target.getEyePos());
    }
    
    @Override
    public float getExtraRoll() {
        
        if (phase == AttackPhase.ATTACKING) {
            var time = owner.getWorld().getTime();
            return (float) (Math.sin(time / 2f) * 20);
        }
        
        return DroneBehaviour.super.getExtraRoll();
    }
    
    @Override
    public int getPriority() {
        return phase == AttackPhase.MOVING_HOME ? 3 : 80;
    }
    
    private enum AttackPhase {
        MOVING_IN, ATTACKING, MOVING_HOME
    }
    
    public static class MeleeAttackSensor implements DroneSensor {
        
        @Override
        public int getPriority() {
            return 40;
        }
        
        @Override
        public boolean sense(DroneServerData drone, PlayerEntity player) {
            
            var world = player.getWorld();
            var entityRange = 16;
            var playerHead = player.getEyePos();
            
            var targets = world.getEntitiesByClass(LivingEntity.class, new Box(playerHead.x - entityRange, playerHead.y - entityRange, playerHead.z - entityRange, playerHead.x + entityRange, playerHead.y + entityRange, playerHead.z + entityRange), EntityPredicates.VALID_LIVING_ENTITY.and(EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR));
            targets.sort(Comparator.comparingDouble((entity) -> entity.squaredDistanceTo(playerHead)));
            targets = targets.stream()
                    .filter(target -> target.isAlive() && !target.isRemoved() && target instanceof Monster)
                    .filter(target -> !(target instanceof net.minecraft.entity.mob.EndermanEntity enderman) || enderman.isAngry())
                    .toList();
            
            if (!targets.isEmpty()) {
                drone.setCurrentTask(new MeleeAttackBehaviour(targets.getFirst(), player, drone));
                return true;
            }
            
            return false;
        }
    }
    
    public static boolean isValid(RecordedBlock block, HashMap<Vec3i, BlockState> frame) {
        // is valid when facing forward (south) and not blocked
        
        var blockMatches = block.state().isIn(TagContent.MELEE_DAMAGE);
        if (!blockMatches) return false;
        
        // ensure front is free
        for (int i = 1; i < 8; i++) {
            if (frame.containsKey(block.localPos().south(i))) return false;
        }
        
        return true;
        
    }
    
}
