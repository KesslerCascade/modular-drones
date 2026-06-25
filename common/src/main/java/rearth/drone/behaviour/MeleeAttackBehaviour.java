package rearth.drone.behaviour;

import rearth.drone.DroneServerData;
import rearth.drone.RecordedBlock;
import rearth.init.TagContent;
import rearth.util.Helpers;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

// an instance of a behaviour to attack one specific entity.
// consists of 3 phases: move in, attack, move home
public class MeleeAttackBehaviour implements DroneBehaviour {

    private static final int MAX_RANGE = 25;
    private static final float HIT_RANGE = 1.25f;
    private static final int ATTACK_COOLDOWN = 21;
    static final int MELEE_PRIORITY_MIN = 11;
    static final int MELEE_PRIORITY_MAX = 25;
    public static final int PLAYER_INITIATED_PRIORITY = 13;

    private final LivingEntity target;
    private final Player owner;
    private final DroneServerData drone;
    private final int assignedPriority;

    private AttackPhase phase;

    public MeleeAttackBehaviour(LivingEntity target, Player owner, DroneServerData drone, int priority) {
        this.target = target;
        this.owner = owner;
        this.drone = drone;
        this.assignedPriority = priority;
        this.phase = AttackPhase.MOVING_IN;
    }

    @Override
    public void tick() {

        switch (phase) {

            // sets target to entity, and if too far / close enough updates phase
            case MOVING_IN -> {

                drone.setTarget(owner.level(), target.getEyePosition());

                var dist = drone.currentPosition.distanceTo(target.getEyePosition());
                var playerDist = drone.currentPosition.distanceTo(owner.getEyePosition());

                if (dist > MAX_RANGE || playerDist > MAX_RANGE) {
                    phase = AttackPhase.MOVING_HOME;
                } else if (dist < HIT_RANGE) {
                    phase = AttackPhase.ATTACKING;
                }

            }

            // keeps attacking the entity after a specific cooldown
            case ATTACKING -> {

                var dist = drone.currentPosition.distanceTo(target.getEyePosition());
                if (dist > HIT_RANGE * 2) {
                    phase = AttackPhase.MOVING_IN;
                    return;
                }

                if (!target.isAttackable() || !target.isAlive() || target.isRemoved()) {
                    phase = AttackPhase.MOVING_HOME;
                    return;
                }

                drone.setTarget(owner.level(), target.getEyePosition());
                if (drone.actionCooldown == 0) {
                    // do attack
                    var damage = 2; // todo
                    target.hurt(new DamageSource(owner.level().registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.PLAYER_ATTACK), owner), damage);
                    drone.actionCooldown = ATTACK_COOLDOWN;

                    if (owner.level() instanceof ServerLevel serverWorld) {
                        var middle = drone.currentPosition.add(target.getEyePosition()).scale(0.5f);
                        var forward = target.getEyePosition().subtract(drone.currentPosition).normalize();
                        serverWorld.sendParticles(ParticleTypes.SWEEP_ATTACK, middle.x, middle.y, middle.z, 1, forward.x, forward.y, forward.z, 0.2f);
                    }
                }

            }
            case MOVING_HOME -> {
                drone.setTarget(owner.level(), owner.getEyePosition().add(0, 0.5, 0));
                var dist = drone.currentPosition.distanceTo(owner.getEyePosition());
                if (dist < HIT_RANGE * 2) {
                    this.finishTask();
                }
            }
        }

    }

    public void finishTask() {
        drone.setIdle(owner, drone);
    }

    @Override
    public float getCurrentYaw() {

        if (phase == AttackPhase.MOVING_HOME)
            return Helpers.calculateYaw(drone.currentPosition, owner.getEyePosition());

        if (phase == AttackPhase.ATTACKING) {
            var progress = drone.actionCooldown / (float) ATTACK_COOLDOWN;
            return Helpers.calculateYaw(drone.currentPosition, target.getEyePosition()) + progress * 90;
        }

        return Helpers.calculateYaw(drone.currentPosition, target.getEyePosition());
    }

    @Override
    public float getExtraRoll() {

        if (phase == AttackPhase.ATTACKING) {
            var time = owner.level().getGameTime();
            return (float) (Math.sin(time / 2f) * 20);
        }

        return DroneBehaviour.super.getExtraRoll();
    }

    @Override
    public int getPriority() {
        return phase == AttackPhase.MOVING_HOME ? 3 : assignedPriority;
    }

    private enum AttackPhase {
        MOVING_IN, ATTACKING, MOVING_HOME
    }

    public static class MeleeAttackSensor implements DroneSensor {

        @Override
        public int getPriority() {
            return MELEE_PRIORITY_MAX;
        }

        @Override
        public boolean sense(DroneServerData drone, Player player) {

            var world = player.level();
            var entityRange = 16;
            var playerHead = player.getEyePosition();

            var targets = world.getEntitiesOfClass(LivingEntity.class, new AABB(playerHead.x - entityRange, playerHead.y - entityRange, playerHead.z - entityRange, playerHead.x + entityRange, playerHead.y + entityRange, playerHead.z + entityRange), EntitySelector.LIVING_ENTITY_STILL_ALIVE.and(EntitySelector.NO_CREATIVE_OR_SPECTATOR));
            targets.sort(Comparator.comparingDouble((entity) -> entity.distanceToSqr(drone.currentPosition)));
            targets = targets.stream()
                    .filter(target -> target.isAlive() && !target.isRemoved() && target instanceof Enemy)
                    .filter(target -> Helpers.isValidAttackTarget(target, false))
                    .filter(target -> Helpers.getDronePath(world, drone.currentPosition, target.getEyePosition()).isReachable())
                    .toList();

            if (targets.isEmpty()) return false;

            var bestTarget = targets.getFirst();
            var dist = drone.currentPosition.distanceTo(bestTarget.getEyePosition());
            var t = Math.clamp(1.0 - dist / entityRange, 0.0, 1.0);
            var priority = (int) Math.round(MELEE_PRIORITY_MIN + t * (MELEE_PRIORITY_MAX - MELEE_PRIORITY_MIN));
            var currentPriority = drone.getCurrentTask() != null ? drone.getCurrentTask().getPriority() : 0;
            if (priority <= currentPriority) return false;

            drone.setCurrentTask(new MeleeAttackBehaviour(bestTarget, player, drone, priority));
            return true;
        }
    }

    public static boolean isValid(RecordedBlock block, HashMap<Vec3i, BlockState> frame) {
        // is valid when facing forward (south) and not blocked

        var blockMatches = block.state().is(TagContent.MELEE_DAMAGE);
        if (!blockMatches) return false;

        // ensure front is free
        for (int i = 1; i < 8; i++) {
            if (frame.containsKey(block.localPos().south(i))) return false;
        }

        return true;

    }

}
