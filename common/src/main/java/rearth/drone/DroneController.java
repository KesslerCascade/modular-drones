package rearth.drone;

import dev.architectury.event.EventResult;
import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.noise.SimplexNoiseSampler;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import rearth.drone.behaviour.*;
import rearth.init.CarriedItemComponent;
import rearth.init.ComponentContent;
import rearth.init.ItemContent;
import rearth.init.NetworkContent;
import rearth.util.Helpers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Optional;

public class DroneController {
    
    // tp the drone to player if it's too far away
    public static final int SNAP_RANGE = 30;
    
    private final static HashMap<Integer, DroneServerData> WORK_DATA = new HashMap<>();
    
    public static final SimplexNoiseSampler SIMPLEX = new SimplexNoiseSampler(Random.create());
    
    public static void tickPlayer(ServerPlayerEntity playerEntity) {
        
        var droneCandidate = getPlayerServerData(playerEntity);
        droneCandidate.ifPresent(serverData -> updateDrone(playerEntity, serverData));
        
    }
    
    public static void updateDrone(PlayerEntity player, DroneServerData serverData) {
        
        if (serverData.getCurrentTask() == null) {
            serverData.setIdle(player, serverData);
        }
        
        if (serverData.droneData.isGlowing()) {
            DroneLight.updateDroneLight(serverData, player.getWorld());
        }

        if (serverData.actionCooldown > 0) {
            serverData.actionCooldown--;
        }

        if (serverData.excludedItemTimeout > 0) {
            serverData.excludedItemTimeout--;
            if (serverData.excludedItemTimeout == 0) {
                serverData.excludedItem = null;
            }
        }

        if (serverData.taskCooldown > 0) {
            serverData.taskCooldown--;
        }

        updateDroneSensors(player, serverData);
        serverData.getCurrentTask().tick();
        updateDroneMovement(player, serverData);
        
        // yes this gets players in 100 dist from all worlds, but I don't care
        if (player.getWorld() instanceof ServerWorld serverWorld) {
            var nearbyPlayers = serverWorld.getPlayers(candidate -> candidate.getPos().squaredDistanceTo(player.getPos()) < 10_000);
            NetworkManager.sendToPlayers(nearbyPlayers, new NetworkContent.DroneMoveSyncPacket(serverData.currentPosition, serverData.currentRotation, serverData.droneData.getDroneId()));
            if (serverData.carriedItemDirty) {
                NetworkManager.sendToPlayers(nearbyPlayers, new NetworkContent.DroneCarriedItemPacket(
                        serverData.droneData.getDroneId(), serverData.carriedItem.copy()));
                serverData.carriedItemDirty = false;
            }
        }
        
    }
    
    /**
     * While blocked, tries to slide the drone one movement step along whichever
     * axis has the
     * smallest delta to the target (but > 0.5), checking axes in ascending delta
     * order.
     */
    private static boolean tryAxisSlideMovement(World world, DroneServerData serverData, float powerMultiplier) {
        record AxisMove(double absDelta, Vec3d candidatePos) {
        }

        var current = serverData.currentPosition;
        var target = serverData.currentTargetPosition;
        var step = powerMultiplier / 20.0;

        var axes = new ArrayList<AxisMove>();

        var dx = target.x - current.x;
        var dy = target.y - current.y;
        var dz = target.z - current.z;
        var absDx = Math.abs(dx);
        var absDy = Math.abs(dy);
        var absDz = Math.abs(dz);

        if (absDx > 0.01)
            axes.add(new AxisMove(absDx, current.add(Math.signum(dx) * Math.min(step, absDx), 0, 0)));
        if (absDy > 0.01)
            axes.add(new AxisMove(absDy, current.add(0, Math.signum(dy) * Math.min(step, absDy), 0)));
        if (absDz > 0.01)
            axes.add(new AxisMove(absDz, current.add(0, 0, Math.signum(dz) * Math.min(step, absDz))));

        axes.sort(Comparator.comparingDouble(AxisMove::absDelta));

        for (var axis : axes) {
            if (Helpers.isLineAvailable(world, current, axis.candidatePos())) {
                serverData.currentPosition = axis.candidatePos();
                return true;
            }
        }
        return false;
    }

    private static void updateDroneSensors(PlayerEntity player, DroneServerData serverData) {
        if (serverData.taskCooldown > 0)
            return;

        var currentPriority = serverData.getCurrentTask().getPriority();

        // if a sensor matches, stop the search
        for (var sensor : serverData.droneData.enabledSensors) {
            if (currentPriority >= sensor.getPriority()) break;

            if (sensor.sense(serverData, player)) {
                break;
            }

        }

    }
    
    private static void updateDroneMovement(PlayerEntity player, DroneServerData serverData) {

        // advance waypoint when the final target is now directly reachable
        if (serverData.nextTargetPosition != null) {
            if (Helpers.isLineAvailable(player.getWorld(), serverData.currentPosition, serverData.nextTargetPosition)) {
                serverData.currentTargetPosition = serverData.nextTargetPosition;
                serverData.lastTargetPosition = serverData.nextTargetPosition;
                serverData.nextTargetPosition = null;
            }
        }

        var powerMultiplier = serverData.droneData.power;

        var accelerationPower = 0.2f;
        var bankingFactor = 30 * Math.sqrt(powerMultiplier);

        var currentVelocity = serverData.currentVelocity;
        var targetOffset = serverData.currentTargetPosition.subtract(serverData.currentPosition);
        var maxOffset = powerMultiplier / 3.0;
        if (targetOffset.length() > maxOffset)
            targetOffset = targetOffset.normalize().multiply(maxOffset);
        var velocityDelta = targetOffset.subtract(currentVelocity);

        // Reset ramp only when the target position changes significantly (new task/target),
        // not on overshoot — resetting ramp there would weaken the correcting force at the worst moment.
        if (serverData.lastTargetPosition == null ||
                serverData.currentTargetPosition.distanceTo(serverData.lastTargetPosition) > 1.0) {
            serverData.accelerationRamp = DroneServerData.ACCELERATION_RAMP_STEP;
            serverData.lastTargetPosition = serverData.currentTargetPosition;
        }
        serverData.accelerationRamp = Math.min(1.0f,
                serverData.accelerationRamp + DroneServerData.ACCELERATION_RAMP_STEP);

        // When decelerating to arrive at target (moving faster than remaining distance implies),
        // bypass the ramp so full correction force is applied — prevents overshoot.
        float effectiveRamp;
        if (currentVelocity.length() > targetOffset.length()) {
            effectiveRamp = 1.0f;
        } else {
            effectiveRamp = serverData.accelerationRamp;
        }
        var effectiveDelta = velocityDelta.multiply(effectiveRamp);

        // 2 movement modes:
        // horizontal thrusters only
        // thrusters for forward and up
        // currently only mode 1 is implemented and available

        // mode 1:
        // angle the model on all axis. Model forward points to the player forward, and acceleration is achieved by tilting the body in the right direction
        // this is similar to a quadcopter
        var rotationAngle = serverData.currentRotation.y;
        var bankX = Math.clamp(effectiveDelta.z * -bankingFactor, -45, 45);
        var bankZ = Math.clamp(effectiveDelta.x * bankingFactor, -45, 45);

        var acceleration = effectiveDelta.length();
        var spawnChance = acceleration - 0.5f;

        if (player.getWorld().getRandom().nextFloat() < spawnChance && player.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(ParticleTypes.SMALL_GUST, serverData.currentPosition.x, serverData.currentPosition.y - 0.2f, serverData.currentPosition.z, 1,
              0.1f, 0.1, 0.1,
              0.1f);
        }

        if (serverData.getCurrentTask() != null) {
            rotationAngle = serverData.getCurrentTask().getCurrentYaw();
            bankX += serverData.getCurrentTask().getExtraRoll();
        }

        serverData.currentRotation = new Vec3d(bankX, rotationAngle, bankZ);

        serverData.currentVelocity = currentVelocity.add(effectiveDelta.multiply(accelerationPower));
        
        var nextPosition = serverData.currentPosition.add(serverData.currentVelocity.multiply(powerMultiplier / 20f));
        
        var positionBlocked = !Helpers.isLineAvailable(player.getWorld(), serverData.currentPosition, nextPosition);
        
        //ghost through blocks
        if (serverData.ghostTicks > 0) {
            serverData.ghostTicks--;
            serverData.currentPosition = nextPosition;
        } else if (serverData.ghostWaitTime > 0) {   // wait for ghosting
            serverData.ghostWaitTime--;
            if (serverData.ghostWaitTime == 0) {
                serverData.ghostTicks = 20;
            }
            // while waiting, try to slide along the least-offset unobstructed axis
            if (!tryAxisSlideMovement(player.getWorld(), serverData, powerMultiplier)) {
                // don't accumulate velocity, this avoids a sudden snap once ghosting kicks in
                serverData.currentVelocity = Vec3d.ZERO;
                serverData.accelerationRamp = DroneServerData.ACCELERATION_RAMP_STEP;
            }
        } else if (positionBlocked) {  // just hit an obstacle, start ghosting CD
            serverData.currentVelocity = Vec3d.ZERO;
            serverData.accelerationRamp = DroneServerData.ACCELERATION_RAMP_STEP;
            serverData.ghostWaitTime = 40;
        } else {    // normal movement
            serverData.currentPosition = nextPosition;
            serverData.ghostTicks = 0;
            serverData.ghostWaitTime = 0;
        }
        
        // apply recoil: a forced impulse set by attacks, decayed each tick, bypasses
        // banking
        if (serverData.recoilVelocity.lengthSquared() > 0.0001) {
            serverData.currentPosition = serverData.currentPosition
                    .add(serverData.recoilVelocity.multiply(powerMultiplier / 20.0));
            serverData.recoilVelocity = serverData.recoilVelocity.multiply(0.75);
            if (serverData.recoilVelocity.lengthSquared() < 0.0001)
                serverData.recoilVelocity = Vec3d.ZERO;
        }

        // tp to player if too far away
        var playerDist = serverData.currentPosition.distanceTo(player.getEyePos());
        if (playerDist > SNAP_RANGE) {
            serverData.currentPosition = player.getEyePos();
        }
        
    }
    
    public static Optional<DroneServerData> getPlayerServerData(PlayerEntity playerEntity) {
        
        if (!(playerEntity instanceof ServerPlayerEntity serverPlayer))
            return Optional.empty();
        var droneStack = findDroneItemStack(playerEntity);
        if (droneStack.isEmpty())
            return Optional.empty();
        var droneData = droneStack.get().get(ComponentContent.DRONE_DATA_TYPE.get());
        if (droneData == null)
            return Optional.empty();

        var serverData = WORK_DATA.computeIfAbsent(droneData.getDroneId(), droneId -> {
            var newData = new DroneServerData(droneData, serverPlayer);
            var saved = droneStack.get().get(ComponentContent.CARRIED_ITEM_TYPE.get());
            if (saved != null && !saved.stack().isEmpty()) {
                newData.carriedItem = saved.stack().copy();
            }
            return newData;
        });
        return Optional.of(serverData);
    }

    /**
     * Finds the live ItemStack reference for the drone item worn by the given
     * player, checking HEAD slot, accessories:drone, and accessories:head cosmetic.
     */
    public static Optional<ItemStack> findDroneItemStack(PlayerEntity playerEntity) {
        var headStack = playerEntity.getEquippedStack(EquipmentSlot.HEAD);
        if (headStack.isOf(ItemContent.POCKET_DRONE.get())
                && headStack.contains(ComponentContent.DRONE_DATA_TYPE.get())) {
            return Optional.of(headStack);
        }

        if (Platform.isModLoaded("accessories") && playerEntity.accessoriesCapability() != null
                && playerEntity.accessoriesCapability().getContainers() != null) {
            var containers = playerEntity.accessoriesCapability().getContainers();

            var droneSlot = containers.get("drone");
            if (droneSlot != null) {
                for (var pair : droneSlot.getAccessories()) {
                    var candidate = pair.getSecond();
                    if (candidate.isOf(ItemContent.POCKET_DRONE.get())
                            && candidate.contains(ComponentContent.DRONE_DATA_TYPE.get())) {
                        return Optional.of(candidate);
                    }
                }
            }

            var headCosmetic = containers.get("accessories:head");
            if (headCosmetic != null) {
                for (var pair : headCosmetic.getCosmeticAccessories()) {
                    var candidate = pair.getSecond();
                    if (candidate.isOf(ItemContent.POCKET_DRONE.get())
                            && candidate.contains(ComponentContent.DRONE_DATA_TYPE.get())) {
                        return Optional.of(candidate);
                    }
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Persists the drone's carried item into the PocketDrone ItemStack's component
     * data, so it survives server restarts. Covers HEAD slot and Accessories slots.
     */
    public static void saveCarriedItemToStack(PlayerEntity owner, ItemStack carriedItem) {
        var droneStack = findDroneItemStack(owner);
        droneStack.ifPresent(stack -> {
            if (carriedItem.isEmpty()) {
                stack.remove(ComponentContent.CARRIED_ITEM_TYPE.get());
            } else {
                stack.set(ComponentContent.CARRIED_ITEM_TYPE.get(), new CarriedItemComponent(carriedItem.copy()));
            }
        });
    }

    public static Optional<DroneData> getDroneOfPlayer(PlayerEntity playerEntity) {
        return findDroneItemStack(playerEntity)
                .map(stack -> stack.get(ComponentContent.DRONE_DATA_TYPE.get()));
    }
    
    private static void issueAttackCommend(PlayerEntity player, DroneServerData serverData, LivingEntity livingEntity) {
        var currentTask = serverData.getCurrentTask();

        if (serverData.droneData.installed.contains(DroneBehaviour.BlockFunctions.BEAM)) {
            if (currentTask != null && currentTask.getPriority() >= BeamAttackBehaviour.BEAM_PRIORITY) return;
            serverData.setCurrentTask(new BeamAttackBehaviour(livingEntity, player, serverData));
            return;
        }

        if (serverData.droneData.installed.contains(DroneBehaviour.BlockFunctions.MELEE_ATTACK)) {
            if (currentTask != null && currentTask.getPriority() >= MeleeAttackBehaviour.PLAYER_INITIATED_PRIORITY) return;
            serverData.setCurrentTask(new MeleeAttackBehaviour(livingEntity, player, serverData, MeleeAttackBehaviour.PLAYER_INITIATED_PRIORITY));
        }
    }
    
    public static EventResult onPlayerAttackEntityEvent(PlayerEntity player, World world, Entity entity, Hand hand, @Nullable EntityHitResult entityHitResult) {
        
        var droneCandidate = getPlayerServerData(player);
        if (droneCandidate.isPresent() && entity instanceof LivingEntity livingEntity)
            DroneController.issueAttackCommend(player, droneCandidate.get(), livingEntity);
        
        
        return EventResult.pass();
    }
    
    public static void onPlayerBlockBreakStart(PlayerEntity player, BlockPos blockPos) {
        
        var droneCandidate = getPlayerServerData(player);
        if (droneCandidate.isPresent() && MiningSupportBehaviour.isValidMiningTarget(player.getWorld(), blockPos)) {
            if (droneCandidate.get().droneData.installed.contains(DroneBehaviour.BlockFunctions.MINING_SUPPORT))
                droneCandidate.get().setCurrentTask(new MiningSupportBehaviour(blockPos, player, droneCandidate.get()));
        }
        
    }

    // Called on player disconnect/quit to purge stale runtime state. Persistent
    // data (carried item) is already saved to the item stack component, so it
    // is safe to drop the in-memory entry here. On the next login a fresh
    // DroneServerData will be reconstructed from the saved component.
    public static void clearPlayerData(ServerPlayerEntity player) {
        getDroneOfPlayer(player).ifPresent(droneData -> WORK_DATA.remove(droneData.getDroneId()));
    }
}
