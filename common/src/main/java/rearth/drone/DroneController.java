package rearth.drone;

import dev.architectury.event.EventResult;
import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
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
import rearth.init.ComponentContent;
import rearth.init.ItemContent;
import rearth.init.NetworkContent;
import rearth.util.Helpers;

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
            serverData.setCurrentTask(new PlayerSwarmBehaviour(serverData, player));
        }
        
        if (serverData.droneData.isGlowing()) {
            DroneLight.updateDroneLight(serverData, player.getWorld());
        }

        if (serverData.actionCooldown > 0) {
            serverData.actionCooldown--;
        }

        updateDroneSensors(player, serverData);
        serverData.getCurrentTask().tick();
        updateDroneMovement(player, serverData);
        
        // yes this gets players in 100 dist from all worlds, but I don't care
        if (player.getWorld() instanceof ServerWorld serverWorld) {
            var nearbyPlayers = serverWorld.getPlayers(candidate -> candidate.getPos().squaredDistanceTo(player.getPos()) < 10_000);
            NetworkManager.sendToPlayers(nearbyPlayers, new NetworkContent.DroneMoveSyncPacket(serverData.currentPosition, serverData.currentRotation, serverData.droneData.getDroneId()));
        }
        
    }
    
    private static void updateDroneSensors(PlayerEntity player, DroneServerData serverData) {
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
        
        var powerMultiplier = serverData.droneData.power;
        
        var accelerationPower = 0.2f;
        var bankingFactor = 30 * Math.sqrt(powerMultiplier);
        
        var currentVelocity = serverData.currentVelocity;
        var targetOffset = serverData.targetPosition.subtract(serverData.currentPosition);
        var velocityDelta = targetOffset.subtract(currentVelocity);
        
        // 2 movement modes:
        // horizontal thrusters only
        // thrusters for forward and up
        // currently only mode 1 is implemented and available
        
        // mode 1:
        // angle the model on all axis. Model forward points to the player forward, and acceleration is achieved by tilting the body in the right direction
        // this is similar to a quadcopter
        var rotationAngle = serverData.currentRotation.y;
        var bankX = Math.clamp(velocityDelta.z * -bankingFactor, -45, 45);
        var bankZ = Math.clamp(velocityDelta.x * bankingFactor, -45, 45);
        
        var acceleration = velocityDelta.length();
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
        
        serverData.currentVelocity = currentVelocity.add(velocityDelta.multiply(accelerationPower));
        
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
        } else if (positionBlocked) {  // just hit an obstacle, start ghosting CD
            serverData.currentVelocity = Vec3d.ZERO;
            serverData.ghostWaitTime = 14;
            
            if (player.getWorld() instanceof ServerWorld serverWorld) {
                var middle = serverData.currentPosition;
                serverWorld.spawnParticles(ParticleTypes.PORTAL, middle.x, middle.y, middle.z, 15, 0, 0, 0, 0.2f);
            }
        } else {    // normal movement
            serverData.currentPosition = nextPosition;
            serverData.ghostTicks = 0;
            serverData.ghostWaitTime = 0;
        }
        
        // tp to player if too far away
        var playerDist = serverData.currentPosition.distanceTo(player.getEyePos());
        if (playerDist > SNAP_RANGE) {
            serverData.currentPosition = player.getEyePos();
        }
        
    }
    
    public static Optional<DroneServerData> getPlayerServerData(PlayerEntity playerEntity) {
        
        var droneCandidate = getDroneOfPlayer(playerEntity);
        if (playerEntity instanceof ServerPlayerEntity serverPlayer && droneCandidate.isPresent()) {
            var serverData = WORK_DATA.computeIfAbsent(droneCandidate.get().getDroneId(), droneId -> new DroneServerData(droneCandidate.get(), serverPlayer));
            return Optional.of(serverData);
        }
        
        return Optional.empty();
    }
    
    public static Optional<DroneData> getDroneOfPlayer(PlayerEntity playerEntity) {
        
        var droneCandidate = playerEntity.getEquippedStack(EquipmentSlot.HEAD);
        if (droneCandidate.isOf(ItemContent.POCKET_DRONE.get()) && droneCandidate.contains(ComponentContent.DRONE_DATA_TYPE.get())) {
            var droneData = droneCandidate.get(ComponentContent.DRONE_DATA_TYPE.get());
            if (droneData == null) return Optional.empty();
            
            return Optional.of(droneData);
            
        }
        
        if (Platform.isModLoaded("accessories") && playerEntity.accessoriesCapability() != null && playerEntity.accessoriesCapability().getContainers() != null) {
            var headCosmetic = playerEntity.accessoriesCapability().getContainers().get("accessories:head");
            
            if (headCosmetic == null)
                return Optional.empty();
            
            for (var pair : headCosmetic.getCosmeticAccessories()) {
                var candidate = pair.getSecond();
                if (candidate.isOf(ItemContent.POCKET_DRONE.get()) && candidate.contains(ComponentContent.DRONE_DATA_TYPE.get())) {
                    
                    var droneData = candidate.get(ComponentContent.DRONE_DATA_TYPE.get());
                    if (droneData == null) return Optional.empty();
                    return Optional.of(droneData);
                    
                }
            }
        }
        
        return Optional.empty();
    }
    
    private static void issueAttackCommend(PlayerEntity player, DroneServerData serverData, LivingEntity livingEntity) {
        
        if (serverData.droneData.installed.contains(DroneBehaviour.BlockFunctions.BEAM)) {
            serverData.setCurrentTask(new BeamAttackBehaviour(livingEntity, player, serverData));
            return;
        }
        
        if (serverData.droneData.installed.contains(DroneBehaviour.BlockFunctions.MELEE_ATTACK))
            serverData.setCurrentTask(new MeleeAttackBehaviour(livingEntity, player, serverData));
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
}
