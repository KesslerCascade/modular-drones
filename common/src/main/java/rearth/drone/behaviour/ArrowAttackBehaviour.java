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
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
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
    private static final int MAX_CONSECUTIVE_FAILED_ATTACKS = 14;
    private static final double RECOIL_STRENGTH = 0.75;
    static final int ARROW_PRIORITY_MIN = 15;
    static final int ARROW_PRIORITY_MAX = 35;

    private int consecutiveFailedAttacks = 0;
    private final int assignedPriority;

    public final LivingEntity target;
    public final PlayerEntity owner;
    public final DroneServerData drone;

    public ArrowAttackBehaviour(LivingEntity target, PlayerEntity owner, DroneServerData drone, int priority) {
        super(drone, owner);
        this.target = target;
        this.owner = owner;
        this.drone = drone;
        this.assignedPriority = priority;
    }

    @Override
    public void tick() {

        super.tick();

        if (target.isRemoved() || !target.isAlive() || !target.isAttackable()) {
            finishTask();
            return;
        }

        var toTarget = target.getEyePos().subtract(this.drone.currentPosition).normalize();
        var shotFrom = this.drone.currentPosition.add(toTarget.multiply(0.5));
        var dist = shotFrom.distanceTo(target.getEyePos());
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

    static boolean hasLos(World world, Vec3d from, LivingEntity target) {
        return Helpers.isLineAvailable(world, target.getEyePos(), from);
    }

    public boolean performAttack(double dist, Vec3d shotFrom) {

        var world = owner.getWorld();
        // for small mobs (babies etc.), aim at body center instead of eye to avoid overshooting the hitbox
        var entityHeight = target.getBoundingBox().getLengthY();
        var aimBase = entityHeight < 1.0 ? target.getPos().add(0, entityHeight * 0.5, 0) : target.getEyePos();
        var targetPos = aimBase.add(0, dist / 10f, 0); // adjust target slightly up for longer distances to hit

        // abort if drone no longer has LOS to target (check both actual entity pos and adjusted aim pos)
        var losContextAdjusted = new RaycastContext(drone.currentPosition, targetPos, RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE, ShapeContext.absent());
        if (!hasLos(world, drone.currentPosition, target)
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
        arrowEntity.setOwner(owner);
        arrowEntity.setVelocity(initialVelocity);
        world.spawnEntity(arrowEntity);
        world.playSound(null, shotFrom.x, shotFrom.y, shotFrom.z, SoundEvents.BLOCK_DISPENSER_LAUNCH, SoundCategory.BLOCKS, 1.0f, 1.0f);

        // recoil: kick the drone backward opposite to the shot direction
        var recoilDir = shotFrom.subtract(targetPos).normalize();
        drone.recoilVelocity = drone.recoilVelocity.add(recoilDir.multiply(RECOIL_STRENGTH));

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
        drone.setIdle(owner, drone);
    }

    @Override
    public Vec3d getDesiredPosition() {
        if (target == null || target.isRemoved() || !target.isAlive())
            return null;
        var direction = target.getEyePos().subtract(owner.getEyePos()).normalize();
        return owner.getEyePos().add(0, 1.0f, 0).add(direction.multiply(1.2f));
    }

    @Override
    public float getCurrentYaw() {
        return Helpers.calculateYaw(drone.currentPosition, target.getEyePos());
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
        public boolean sense(DroneServerData drone, PlayerEntity player) {

            var world = player.getWorld();
            var entityRange = getTargetingRange();
            var playerHead = player.getEyePos();

            var targets = world.getEntitiesByClass(LivingEntity.class, new Box(playerHead.x - entityRange, playerHead.y - entityRange, playerHead.z - entityRange, playerHead.x + entityRange, playerHead.y + entityRange, playerHead.z + entityRange), EntityPredicates.VALID_LIVING_ENTITY.and(EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR));
            targets.sort(Comparator.comparingDouble((entity) -> entity.squaredDistanceTo(playerHead)));
            targets = targets.stream()
                    .filter(target -> target.isAlive() && !target.isRemoved() && target instanceof Monster)
                    .filter(target -> !(target instanceof net.minecraft.entity.mob.EndermanEntity enderman)
                            || (!shootsProjectile() && enderman.isAngry()))
                    .filter(target -> hasLos(world, drone.currentPosition, target))
                    .toList();

            if (targets.isEmpty()) return false;

            var bestTarget = targets.getFirst();
            var dist = playerHead.distanceTo(bestTarget.getEyePos());
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

        public void onTargetFound(DroneServerData drone, PlayerEntity player, LivingEntity target, int priority) {
            drone.setCurrentTask(new ArrowAttackBehaviour(target, player, drone, priority));
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
