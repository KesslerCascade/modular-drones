package rearth.drone.behaviour;

import net.minecraft.block.BlockState;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.jetbrains.annotations.Nullable;
import rearth.drone.DroneController;
import rearth.drone.DroneServerData;
import rearth.drone.RecordedBlock;
import rearth.init.TagContent;
import rearth.util.Helpers;

import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

// enabled with a lodestone and observer
public class PickupBehaviour implements DroneBehaviour {

    private static final int MAX_RANGE = 25;
    private static final float PICKUP_RANGE = 0.75f;
    private static final int CONSOLIDATION_RANGE = 8;

    private final PlayerEntity owner;
    private final DroneServerData drone;
    private @Nullable ItemEntity target;

    private PickupPhase phase;

    // Start in MOVING_IN toward a specific target
    public PickupBehaviour(PlayerEntity owner, DroneServerData drone, ItemEntity target) {
        this.owner = owner;
        this.drone = drone;
        this.target = target;
        this.phase = PickupPhase.MOVING_IN;
    }

    // Start in MOVING_HOME to deliver already-carried item (e.g. resume after
    // combat)
    public PickupBehaviour(PlayerEntity owner, DroneServerData drone) {
        this.owner = owner;
        this.drone = drone;
        this.target = null;
        this.phase = PickupPhase.MOVING_HOME;
    }

    @Override
    public void tick() {
        switch (phase) {
            case MOVING_IN -> {
                if (target == null || target.isRemoved()) {
                    phase = PickupPhase.MOVING_HOME;
                    break;
                }

                drone.targetPosition = target.getEyePos();

                var playerDist = drone.currentPosition.distanceTo(owner.getEyePos());
                if (playerDist > MAX_RANGE) {
                    phase = PickupPhase.MOVING_HOME;
                    break;
                }

                var targetDist = drone.currentPosition.distanceTo(target.getEyePos());
                if (targetDist < PICKUP_RANGE) {
                    collectItem();
                }
            }
            case MOVING_HOME -> {
                if (drone.carriedItem.isEmpty()) {
                    finishTask();
                    break;
                }

                drone.targetPosition = owner.getEyePos().add(0, 0.7, 0);

                var playerDist = drone.currentPosition.distanceTo(owner.getEyePos());
                if (playerDist < 1) {
                    deliverItem();
                }
            }
        }
    }

    private void collectItem() {
        var entityStack = target.getStack();

        int space;
        if (drone.carriedItem.isEmpty()) {
            space = entityStack.getMaxCount();
        } else if (ItemStack.areItemsAndComponentsEqual(drone.carriedItem, entityStack)) {
            space = drone.carriedItem.getMaxCount() - drone.carriedItem.getCount();
        } else {
            // Different item type somehow. Abort pickup, keep what we're already carrying
            phase = PickupPhase.MOVING_HOME;
            return;
        }

        var toTake = Math.min(space, entityStack.getCount());

        if (drone.carriedItem.isEmpty()) {
            drone.carriedItem = entityStack.copyWithCount(toTake);
        } else {
            drone.carriedItem.increment(toTake);
        }

        if (toTake >= entityStack.getCount()) {
            target.discard();
        } else {
            // Partial pickup - leave the remainder in the world
            entityStack.decrement(toTake);
        }
        target = null;

        drone.carriedItemDirty = true;
        DroneController.saveCarriedItemToStack(owner, drone.carriedItem);

        // Try to consolidate nearby matching items before returning home
        if (drone.carriedItem.getMaxCount() > 1 && drone.carriedItem.getCount() < drone.carriedItem.getMaxCount()) {
            var next = findSameTypeNearDrone(drone, drone.carriedItem, owner.getWorld(), drone.excludedItem);
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
        owner.getInventory().insertStack(toDeliver);

        if (!toDeliver.isEmpty() && owner.getWorld() instanceof ServerWorld serverWorld) {
            // Inventory full - drop remainder at drone position
            var pos = drone.currentPosition;
            var dropped = new ItemEntity(serverWorld, pos.x, pos.y, pos.z, toDeliver);
            dropped.setVelocity(Vec3d.ZERO);
            serverWorld.spawnEntity(dropped);
            drone.excludedItem = dropped.getUuid();
            drone.excludedItemTimeout = 200;
        }

        drone.carriedItem = ItemStack.EMPTY;
        drone.carriedItemDirty = true;
        DroneController.saveCarriedItemToStack(owner, ItemStack.EMPTY);
        finishTask();
    }

    private void finishTask() {
        drone.setCurrentTask(new PlayerSwarmBehaviour(drone, owner));
    }

    @Override
    public void onStopped() {
        // Items are held
        // PickupSensor will resume delivery when the drone returns to idle
    }

    @Override
    public float getCurrentYaw() {
        if (phase == PickupPhase.MOVING_IN && target != null) {
            return Helpers.calculateYaw(drone.currentPosition, target.getEyePos());
        }
        return Helpers.calculateYaw(drone.currentPosition, owner.getEyePos());
    }

    @Override
    public int getPriority() {
        return 10;
    }

    private enum PickupPhase {
        MOVING_IN, MOVING_HOME
    }

    private static Optional<ItemEntity> findSameTypeNearDrone(DroneServerData drone, ItemStack carried,
            net.minecraft.world.World world, @Nullable UUID excludedId) {
        var pos = drone.currentPosition;
        var box = new Box(
                pos.x - CONSOLIDATION_RANGE, pos.y - CONSOLIDATION_RANGE, pos.z - CONSOLIDATION_RANGE,
                pos.x + CONSOLIDATION_RANGE, pos.y + CONSOLIDATION_RANGE, pos.z + CONSOLIDATION_RANGE);
        var candidates = world.getEntitiesByClass(ItemEntity.class, box, entity -> {
            if (entity.cannotPickup())
                return false;
            if (excludedId != null && entity.getUuid().equals(excludedId))
                return false;
            return ItemStack.areItemsAndComponentsEqual(entity.getStack(), carried);
        });
        return candidates.isEmpty() ? Optional.empty() : Optional.of(candidates.getFirst());
    }

    public static Optional<ItemEntity> GetPickupTarget(PlayerEntity player, @Nullable UUID excludedItemId) {
        var world = player.getWorld();
        var origin = player.getEyePos();
        var range = MAX_RANGE / 2;
        var box = new Box(origin.x - range, origin.y - range, origin.z - range, origin.x + range, origin.y + range,
                origin.z + range);
        var items = world.getEntitiesByClass(ItemEntity.class, box, entity -> {
            if (entity.cannotPickup())
                return false;
            if (excludedItemId != null && entity.getUuid().equals(excludedItemId))
                return false;
            return true;
        });
        return items.isEmpty() ? Optional.empty() : Optional.of(items.getFirst());
    }

    public static class PickupSensor implements DroneSensor {

        @Override
        public int getPriority() {
            return 10;
        }

        @Override
        public boolean sense(DroneServerData drone, PlayerEntity player) {
            // If already carrying, resume delivery
            if (!drone.carriedItem.isEmpty()) {
                drone.setCurrentTask(new PickupBehaviour(player, drone));
                return true;
            }
            // Otherwise look for a new item to pick up
            var candidate = PickupBehaviour.GetPickupTarget(player, drone.excludedItem);
            if (candidate.isPresent()) {
                drone.setCurrentTask(new PickupBehaviour(player, drone, candidate.get()));
                return true;
            }
            return false;
        }
    }

    public static boolean isValid(RecordedBlock block, HashMap<Vec3i, BlockState> frame) {
        // is valid when facing down and not blocked
        var blockMatches = block.state().isIn(TagContent.PICKUP_TOOLS);
        if (!blockMatches) return false;

        // ensure dir is free
        for (int i = 1; i < 8; i++) {
            if (frame.containsKey(block.localPos().down(i))) return false;
        }

        return true;
    }
}
