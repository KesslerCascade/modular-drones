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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

// an instance of a behaviour to attack one specific entity.
// consists of 3 phases: move in, attack, move home
public class ArrowAttackBehaviour extends PlayerSwarmBehaviour {

    private static final int MAX_RANGE = 25;
    private static final int MAX_CONSECUTIVE_FAILED_ATTACKS = 14;
    private static final double RECOIL_STRENGTH = 0.75;
    static final int ARROW_PRIORITY_MIN = 15;
    static final int ARROW_PRIORITY_MAX = 35;

    private int consecutiveFailedAttacks = 0;
    private final int assignedPriority;

    public final LivingEntity target;
    public final Player owner;
    public final DroneServerData drone;

    public ArrowAttackBehaviour(LivingEntity target, Player owner, DroneServerData drone, int priority) {
        super(drone, owner);
        this.target = target;
        this.owner = owner;
        this.drone = drone;
        this.assignedPriority = priority;

        // give the drone half a second to move towards the target before it fires
        drone.actionCooldown = Math.max(drone.actionCooldown, 10);
    }

    @Override
    public void tick() {

        super.tick();

        if (target.isRemoved() || !target.isAlive() || !target.isAttackable()) {
            finishTask();
            return;
        }

        var toTarget = target.getEyePosition().subtract(this.drone.currentPosition).normalize();
        var shotFrom = this.drone.currentPosition.add(toTarget.scale(0.5));
        var dist = shotFrom.distanceTo(target.getEyePosition());
        if (dist > MAX_RANGE) {
            finishTask();
            return;
        }

        if (drone.actionCooldown == 0) {
            if (performAttack(dist, shotFrom)) {
                consecutiveFailedAttacks = 0;
                drone.actionCooldown = getAttackCooldown();
            } else {
                consecutiveFailedAttacks++;
                if (consecutiveFailedAttacks > MAX_CONSECUTIVE_FAILED_ATTACKS) {
                    finishTask();
                    return;
                }
            }
        }

    }

    static boolean hasLos(Level world, Vec3 from, LivingEntity target) {
        return Helpers.isLineAvailable(world, target.getEyePosition(), from);
    }

    public boolean performAttack(double dist, Vec3 shotFrom) {

        var world = owner.level();
        // for small mobs (babies etc.), aim at body center instead of eye to avoid overshooting the hitbox
        var entityHeight = target.getBoundingBox().getYsize();
        var aimBase = entityHeight < 1.0 ? target.position().add(0, entityHeight * 0.5, 0) : target.getEyePosition();
        var targetPos = aimBase.add(0, dist / 10f, 0); // adjust target slightly up for longer distances to hit

        // abort if drone no longer has LOS to target (check both actual entity pos and adjusted aim pos)
        var losContextAdjusted = new ClipContext(drone.currentPosition, targetPos, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, CollisionContext.empty());
        if (!hasLos(world, drone.currentPosition, target)
                || world.clip(losContextAdjusted).getType() != HitResult.Type.MISS)
            return false;

        // abort if owner is in the line of fire (check both aim direction and landing
        // point to bracket the arc)
        var ownerBox = owner.getBoundingBox().inflate(0.3);
        var aimDir = targetPos.subtract(shotFrom).normalize();
        var landDir = target.getEyePosition().subtract(shotFrom).normalize();
        if (ownerBox.clip(shotFrom, shotFrom.add(aimDir.scale(dist + 5))).isPresent()
                || ownerBox.clip(shotFrom, shotFrom.add(landDir.scale(dist + 5))).isPresent())
            return false;

        // shoot arrow
        var stack = new ItemStack(Items.ARROW);
        var offset = targetPos.subtract(shotFrom);
        var initialVelocity = offset.normalize().scale(2);

        var arrowEntity = new Arrow(world, shotFrom.x, shotFrom.y, shotFrom.z, stack, null);
        arrowEntity.setOwner(owner);
        arrowEntity.setDeltaMovement(initialVelocity);
        world.addFreshEntity(arrowEntity);
        world.playSound(null, shotFrom.x, shotFrom.y, shotFrom.z, SoundEvents.DISPENSER_LAUNCH, SoundSource.BLOCKS, 1.0f, 1.0f);

        // recoil: kick the drone backward opposite to the shot direction
        var recoilDir = shotFrom.subtract(targetPos).normalize();
        drone.recoilVelocity = drone.recoilVelocity.add(recoilDir.scale(RECOIL_STRENGTH));

        // particle
        if (owner.level() instanceof ServerLevel serverWorld) {
            var forward = target.getEyePosition().subtract(drone.currentPosition).normalize();
            var particleStart = drone.currentPosition.add(forward.scale(0.3f));
            serverWorld.sendParticles(ParticleTypes.SMALL_GUST, particleStart.x, particleStart.y, particleStart.z, 1, forward.x, forward.y, forward.z, 0.2f);
        }

        return true;
    }

    public int getAttackCooldown() {
        return 24;
    }

    public void finishTask() {
        drone.setIdle(owner, drone);
    }

    @Override
    public Vec3 getDesiredPosition() {
        if (target == null || target.isRemoved() || !target.isAlive())
            return null;
        var diff = target.getEyePosition().subtract(owner.getEyePosition());
        var direction = new Vec3(diff.x, 0, diff.z).normalize();
        return owner.getEyePosition().add(0, 1.0f, 0).add(direction.scale(1.2f));
    }

    @Override
    public float getCurrentYaw() {
        return Helpers.calculateYaw(drone.currentPosition, target.getEyePosition());
    }

    @Override
    public int getPriority() {
        return assignedPriority;
    }

    public static class ArrowAttackSensor implements DroneSensor {

        @Override
        public int getPriority() {
            return ARROW_PRIORITY_MAX;
        }

        @Override
        public boolean sense(DroneServerData drone, Player player) {

            var world = player.level();
            var entityRange = getTargetingRange();
            var playerHead = player.getEyePosition();

            var targets = world.getEntitiesOfClass(LivingEntity.class, new AABB(playerHead.x - entityRange, playerHead.y - entityRange, playerHead.z - entityRange, playerHead.x + entityRange, playerHead.y + entityRange, playerHead.z + entityRange), EntitySelector.LIVING_ENTITY_STILL_ALIVE.and(EntitySelector.NO_CREATIVE_OR_SPECTATOR));
            targets.sort(Comparator.comparingDouble((entity) -> entity.distanceToSqr(playerHead)));
            targets = targets.stream()
                    .filter(target -> target.isAlive() && !target.isRemoved() && target instanceof Enemy)
                    .filter(target -> !(target instanceof net.minecraft.world.entity.monster.EnderMan enderman)
                            || (!shootsProjectile() && enderman.isCreepy()))
                    .filter(target -> hasLos(world, drone.currentPosition, target))
                    .toList();

            if (targets.isEmpty()) return false;

            var bestTarget = targets.getFirst();
            var dist = playerHead.distanceTo(bestTarget.getEyePosition());
            var t = Math.clamp(1.0 - dist / entityRange, 0.0, 1.0);
            var priority = (int) Math.round(ARROW_PRIORITY_MIN + t * (ARROW_PRIORITY_MAX - ARROW_PRIORITY_MIN));
            var currentPriority = drone.getCurrentTask() != null ? drone.getCurrentTask().getPriority() : 0;
            if (priority <= currentPriority) return false;

            onTargetFound(drone, player, bestTarget, priority);
            return true;
        }

        public int getTargetingRange() {
            return 16;
        }

        public boolean shootsProjectile() {
            return true;
        }

        public void onTargetFound(DroneServerData drone, Player player, LivingEntity target, int priority) {
            drone.setCurrentTask(new ArrowAttackBehaviour(target, player, drone, priority));
        }
    }

    public static boolean isValid(RecordedBlock block, HashMap<Vec3i, BlockState> frame) {
        // is valid when facing forward (south?) and not blocked

        var blockMatches = block.state().is(TagContent.ARROW_LAUNCHER);
        if (!blockMatches) return false;

        // ensure front is free
        for (int i = 1; i < 8; i++) {
            if (frame.containsKey(block.localPos().south(i))) return false;
        }

        return true;

    }

}
