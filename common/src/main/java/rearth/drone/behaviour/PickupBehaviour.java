package rearth.drone.behaviour;

import org.jetbrains.annotations.Nullable;
import rearth.drone.DroneController;
import rearth.drone.DroneServerData;
import rearth.drone.RecordedBlock;
import rearth.init.TagContent;
import rearth.util.Helpers;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

// enabled with a lodestone and observer
public class PickupBehaviour implements DroneBehaviour {

    private static final int MAX_RANGE = 25;
    private static final float PICKUP_RANGE = 0.75f;
    private static final int CONSOLIDATION_RANGE = 8;

    private final Player owner;
    private final DroneServerData drone;
    private @Nullable ItemEntity target;

    private PickupPhase phase;

    // Start in MOVING_IN toward a specific target
    public PickupBehaviour(Player owner, DroneServerData drone, ItemEntity target) {
        this.owner = owner;
        this.drone = drone;
        this.target = target;
        this.phase = PickupPhase.MOVING_IN;
    }

    // Start in MOVING_HOME to deliver already-carried item (e.g. resume after
    // combat)
    public PickupBehaviour(Player owner, DroneServerData drone) {
        this.owner = owner;
        this.drone = drone;
        this.target = null;
        this.phase = PickupPhase.MOVING_HOME;
    }

    @Override
    public void tick() {
        if (owner.isRemoved()) {
            finishTask();
            return;
        }

        switch (phase) {
            case MOVING_IN -> {
                if (target == null || target.isRemoved()) {
                    phase = PickupPhase.MOVING_HOME;
                    break;
                }

                drone.setTarget(owner.level(), target.position().add(0, 0.5, 0));

                var playerDist = drone.currentPosition.distanceTo(owner.getEyePosition());
                if (playerDist > MAX_RANGE) {
                    phase = PickupPhase.MOVING_HOME;
                    break;
                }

                var targetDist = drone.currentPosition.distanceTo(target.position().add(0, 0.5, 0));
                if (targetDist < PICKUP_RANGE) {
                    collectItem();
                }
            }
            case MOVING_HOME -> {
                if (drone.carriedItem.isEmpty()) {
                    finishTask();
                    break;
                }

                drone.setTarget(owner.level(), owner.getEyePosition().add(0, 0.7, 0));

                var playerDist = drone.currentPosition.distanceTo(owner.getEyePosition());
                if (playerDist < 1) {
                    deliverItem();
                }
            }
        }
    }

    private void collectItem() {
        var entityStack = target.getItem();

        int space;
        if (drone.carriedItem.isEmpty()) {
            space = entityStack.getMaxStackSize();
        } else if (ItemStack.isSameItemSameComponents(drone.carriedItem, entityStack)) {
            space = drone.carriedItem.getMaxStackSize() - drone.carriedItem.getCount();
        } else {
            // Different item type somehow. Abort pickup, keep what we're already carrying
            phase = PickupPhase.MOVING_HOME;
            return;
        }

        var toTake = Math.min(space, entityStack.getCount());

        if (drone.carriedItem.isEmpty()) {
            drone.carriedItem = entityStack.copyWithCount(toTake);
        } else {
            drone.carriedItem.grow(toTake);
        }

        if (toTake >= entityStack.getCount()) {
            target.discard();
        } else {
            // Partial pickup - leave the remainder in the world
            entityStack.shrink(toTake);
        }
        target = null;

        drone.carriedItemDirty = true;
        DroneController.saveCarriedItemToStack(owner, drone.carriedItem);

        // Try to consolidate nearby matching items before returning home
        if (drone.carriedItem.getMaxStackSize() > 1 && drone.carriedItem.getCount() < drone.carriedItem.getMaxStackSize()) {
            var next = findSameTypeNearDrone(drone, drone.carriedItem, owner.level(), drone.excludedItem);
            if (next.isPresent()) {
                target = next.get();
                // stay in MOVING_IN
                return;
            }
        }

        phase = PickupPhase.MOVING_HOME;
    }

    private void deliverItem() {
        var toDeliver = drone.carriedItem.copy();
        var countBefore = toDeliver.getCount();
        owner.getInventory().add(toDeliver);
        if (toDeliver.getCount() < countBefore) {
            owner.level().playSound(null, owner.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2f, ((owner.getRandom().nextFloat() - owner.getRandom().nextFloat()) * 0.7f + 1.0f) * 2.0f);
        }

        if (!toDeliver.isEmpty() && owner.level() instanceof ServerLevel serverWorld) {
            // Inventory full - drop remainder at drone position
            var pos = drone.currentPosition;
            var dropped = new ItemEntity(serverWorld, pos.x, pos.y, pos.z, toDeliver);
            dropped.setDeltaMovement(Vec3.ZERO);
            serverWorld.addFreshEntity(dropped);
            drone.excludedItem = dropped.getUUID();
            drone.excludedItemTimeout = 200;
        }

        drone.carriedItem = ItemStack.EMPTY;
        drone.carriedItemDirty = true;
        DroneController.saveCarriedItemToStack(owner, ItemStack.EMPTY);
        finishTask();
    }

    private void finishTask() {
        drone.setIdle(owner, drone);
    }

    @Override
    public void onStopped() {
        // Items are held
        // PickupSensor will resume delivery when the drone returns to idle
    }

    @Override
    public float getCurrentYaw() {
        if (phase == PickupPhase.MOVING_IN && target != null) {
            return Helpers.calculateYaw(drone.currentPosition, target.getEyePosition());
        }
        return Helpers.calculateYaw(drone.currentPosition, owner.getEyePosition());
    }

    @Override
    public int getPriority() {
        return 10;
    }

    private enum PickupPhase {
        MOVING_IN, MOVING_HOME
    }

    private static Optional<ItemEntity> findSameTypeNearDrone(DroneServerData drone, ItemStack carried,
            net.minecraft.world.level.Level world, @Nullable UUID excludedId) {
        var pos = drone.currentPosition;
        var box = new AABB(
                pos.x - CONSOLIDATION_RANGE, pos.y - CONSOLIDATION_RANGE, pos.z - CONSOLIDATION_RANGE,
                pos.x + CONSOLIDATION_RANGE, pos.y + CONSOLIDATION_RANGE, pos.z + CONSOLIDATION_RANGE);
        var candidates = world.getEntitiesOfClass(ItemEntity.class, box, entity -> {
            if (entity.hasPickUpDelay())
                return false;
            if (excludedId != null && entity.getUUID().equals(excludedId))
                return false;
            if (!ItemStack.isSameItemSameComponents(entity.getItem(), carried))
                return false;
            return Helpers.getDronePath(world, pos, entity.position().add(0, 0.5, 0)).isReachable();
        });
        if (candidates.isEmpty()) return Optional.empty();
        candidates.sort(Comparator.comparingDouble(e -> e.position().distanceTo(pos)));
        return Optional.of(candidates.getFirst());
    }

    public static Optional<ItemEntity> GetPickupTarget(Player player, @Nullable UUID excludedItemId, Vec3 dronePos) {
        var world = player.level();
        var origin = player.getEyePosition();
        var range = MAX_RANGE / 2;
        var box = new AABB(origin.x - range, origin.y - range, origin.z - range, origin.x + range, origin.y + range,
                origin.z + range);
        var items = world.getEntitiesOfClass(ItemEntity.class, box, entity -> {
            if (entity.hasPickUpDelay())
                return false;
            if (excludedItemId != null && entity.getUUID().equals(excludedItemId))
                return false;
            return Helpers.getDronePath(world, dronePos, entity.position().add(0, 0.5, 0)).isReachable();
        });
        return items.isEmpty() ? Optional.empty() : Optional.of(items.getFirst());
    }

    public static class PickupSensor implements DroneSensor {

        @Override
        public int getPriority() {
            return 10;
        }

        @Override
        public boolean sense(DroneServerData drone, Player player) {
            // If already carrying, resume delivery
            if (!drone.carriedItem.isEmpty()) {
                drone.setCurrentTask(new PickupBehaviour(player, drone));
                return true;
            }
            // Otherwise look for a new item to pick up
            var candidate = PickupBehaviour.GetPickupTarget(player, drone.excludedItem, drone.currentPosition);
            if (candidate.isPresent()) {
                drone.setCurrentTask(new PickupBehaviour(player, drone, candidate.get()));
                return true;
            }
            return false;
        }
    }

    public static boolean isValid(RecordedBlock block, HashMap<Vec3i, BlockState> frame) {
        // is valid when facing down and not blocked
        var blockMatches = block.state().is(TagContent.PICKUP_TOOLS);
        if (!blockMatches) return false;

        // ensure dir is free
        for (int i = 1; i < 8; i++) {
            if (frame.containsKey(block.localPos().below(i))) return false;
        }

        return true;
    }
}
