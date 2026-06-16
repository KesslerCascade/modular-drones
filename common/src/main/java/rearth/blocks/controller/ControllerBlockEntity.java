package rearth.blocks.controller;

import io.netty.buffer.ByteBuf;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;
import rearth.Drones;
import rearth.drone.DroneData;
import rearth.drone.RecordedBlock;
import rearth.init.BlockContent;
import rearth.init.BlockEntitiesContent;
import rearth.init.ComponentContent;
import rearth.init.ItemContent;
import rearth.util.FloodFill;
import rearth.util.RotationUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class ControllerBlockEntity extends BlockEntity {
    
    public static final float LOW_THRUSTER_POWER = 10f;
    public static final float MEDIUM_THRUSTER_POWER = 25f;
    public static final float HIGH_THRUSTER_POWER = 40f;
    public static final float ULTRA_THRUSTER_POWER = 60f;
    
    private String lastDroneName = "";

    public ControllerBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.ASSEMBLER_CONTROLLER.get(), pos, state);
    }

    public String getLastDroneName() {
        return lastDroneName;
    }

    @Override
    protected void saveAdditional(ValueOutput tag) {
        super.saveAdditional(tag);
        tag.putString("last_drone_name", lastDroneName);
    }

    @Override
    protected void loadAdditional(ValueInput tag) {
        super.loadAdditional(tag);
        lastDroneName = tag.getStringOr("last_drone_name", "");
    }
    
    public List<BlockPos> getPlatformBlocks() {
        
        var frameStart = getPlatformStart();
        
        if (frameStart.isEmpty()) return List.of();
        
        var frameBlocks = FloodFill.Run(level, frameStart.get(), (pos, candidate) -> candidate.is(BlockContent.ASSEMBLER_FRAME), checkPos -> true, 200, false);
        
        if (frameBlocks.isEmpty()) return List.of();
        
        return frameBlocks;
    }
    
    public @Nullable DroneData getCurrentDroneData() {
        var frameBlocks = getPlatformBlocks();
        
        if (frameBlocks.isEmpty()) return null;
        
        BlockPos droneStart = null;
        for (var frameBlock : frameBlocks) {
            var frameAbove = frameBlock.above();
            var candidateState = level.getBlockState(frameAbove);
            if (isValidDroneBlock(level, frameAbove, candidateState)) {
                droneStart = frameAbove;
                break;
            }
        }

        if (droneStart == null) return null;

        var droneBlocks = FloodFill.Run(level, droneStart, (pos, state) -> isValidDroneBlock(level, pos, state), this::isAboveOwnFrame, 1000, true);
        var droneCenter = findCenterOfMass(droneBlocks);
        System.out.println("drone: " + droneBlocks);

        var controllerFacing = getBlockState().getValue(ControllerBlock.FACING);
        var toCanonical = RotationUtil.rotationFromTo(controllerFacing, Direction.SOUTH);

        var blockData = new ArrayList<RecordedBlock>();
        for (var blockPos : droneBlocks) {
            var blockState = level.getBlockState(blockPos);
            var localPos = RotationUtil.rotate(blockPos.subtract(droneCenter), toCanonical);
            var rotatedState = blockState.rotate(toCanonical);
            var data = new RecordedBlock(rotatedState, localPos);
            blockData.add(data);
        }
        
        var droneId = level.getRandom().nextInt(Integer.MAX_VALUE);
        var droneOffset = droneCenter.subtract(worldPosition);
        
        return new DroneData(blockData, droneId, droneOffset);
        // DroneController.PLAYER_DRONES.put(player.getName(), droneData);
        
    }
    
    private boolean isAboveOwnFrame(BlockPos pos) {
        
        var maxRange = 20;
        for (int i = 1; i <= maxRange; i++) {
            var testPos = pos.below(i);
            if (level.getBlockState(testPos).is(BlockContent.ASSEMBLER_FRAME)) return true;
        }
        
        return false;
        
    }
    
    private static BlockPos findCenterOfMass(List<BlockPos> positions) {
        if (positions.isEmpty()) {
            Drones.LOGGER.warn("tried to find COM for empty drone");
            return BlockPos.ZERO;
        }
        
        var dataX = 0d;
        var dataY = 0d;
        var dataZ = 0d;

        for (var pos : positions) {
            var center = pos.getCenter();
            dataX += center.x;
            dataY += center.y;
            dataZ += center.z;
        }

        var realCOM = new Vec3(dataX / positions.size(), dataY / positions.size(), dataZ / positions.size());
        return BlockPos.containing(realCOM);
    }
    
    private Optional<BlockPos> getPlatformStart() {
        for (var neighbor : FloodFill.GetHorizontalNeighbors(worldPosition)) {
            if (level.getBlockState(neighbor).is(BlockContent.ASSEMBLER_FRAME)) return Optional.of(neighbor);
        }
        
        return Optional.empty();
    }
    
    public boolean loadDroneToWorld(DroneData data, Player player, String droneName) {

        if (getCurrentDroneData() != null) {
            player.displayClientMessage(Component.translatable("drone.message.platform_occupied"), true);
            return false;
        }

        var controllerFacing = getBlockState().getValue(ControllerBlock.FACING);
        var fromCanonical = RotationUtil.rotationFromTo(Direction.SOUTH, controllerFacing);

        var rotatedBlocks = new ArrayList<RecordedBlock>();
        for (var droneBlockData : data.getBlocks()) {
            var rotatedLocalPos = RotationUtil.rotate(droneBlockData.localPos(), fromCanonical);
            var rotatedState = droneBlockData.state().rotate(fromCanonical);
            rotatedBlocks.add(new RecordedBlock(rotatedState, rotatedLocalPos));
        }

        var platformBlocks = getPlatformBlocks();
        if (platformBlocks.isEmpty()) {
            player.displayClientMessage(Component.translatable("drone.message.platform_too_small"), true);
            return false;
        }

        // the recorded local positions are relative to the drone's center of mass, which can have a negative
        // Y component for tall drones. Shift the placement origin up so the lowest block still lands on the platform.
        var minLocalY = 0;
        for (var rotatedBlock : rotatedBlocks) {
            minLocalY = Math.min(minLocalY, rotatedBlock.localPos().getY());
        }
        var placementUpShift = -minLocalY;

        var candidates = new ArrayList<BlockPos>();
        for (var frameBlock : platformBlocks) {
            var above = frameBlock.above();
            if (isAboveOwnFrame(above)) candidates.add(above);
        }
        candidates.sort(Comparator.comparingDouble(pos -> pos.distSqr(worldPosition)));

        BlockPos chosenCenter = null;
        for (var candidate : candidates) {
            var candidateCenter = candidate.above(placementUpShift);
            var fits = true;
            for (var rotatedBlock : rotatedBlocks) {
                var worldPos = candidateCenter.offset(rotatedBlock.localPos());
                if (!isAboveOwnFrame(worldPos) || !level.getBlockState(worldPos).isAir()) {
                    fits = false;
                    break;
                }
            }
            if (fits) {
                chosenCenter = candidateCenter;
                break;
            }
        }

        if (chosenCenter == null) {
            player.displayClientMessage(Component.translatable("drone.message.platform_too_small"), true);
            return false;
        }

        lastDroneName = droneName;
        setChanged();

        level.playSound(null, worldPosition, SoundEvents.SHROOMLIGHT_PLACE, SoundSource.BLOCKS, 1f, 1f);

        for (var rotatedBlock : rotatedBlocks) {
            var worldPos = chosenCenter.offset(rotatedBlock.localPos());
            level.setBlockAndUpdate(worldPos, rotatedBlock.state());

            if (level instanceof ServerLevel serverWorld) {
                var spawnAt = worldPos.getCenter();
                serverWorld.sendParticles(ParticleTypes.GUST, spawnAt.x, spawnAt.y, spawnAt.z, 1, 0, 0.1f, 0, 0.5f);
            }
        }

        return true;

    }
    
    private static boolean isValidDroneBlock(Level level, BlockPos pos, BlockState state) {
        return !state.isAir() && !state.liquid() && !state.is(BlockContent.ASSEMBLER_FRAME) && !state.is(BlockContent.ASSEMBLER_CONTROLLER)
          && state.getDestroySpeed(level, pos) >= 0;
    }
    
    // this is called on the server, after the player has clicked the "assemble" button
    public void assembleDrone(Player player, String name) {
        
        Drones.LOGGER.info("Assembling drone for: {}, drone name: {}", player.getName(), name);
        
        var droneData = getCurrentDroneData();
        if (droneData == null) {
            Drones.LOGGER.warn("Player tried to create empty/invalid drone");
            return;
        }
        
        var createdStack = new ItemStack(ItemContent.POCKET_DRONE);
        createdStack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        createdStack.set(ComponentContent.DRONE_DATA_TYPE.get(), droneData);

        lastDroneName = name;
        setChanged();
        
        var itemEntity = new ItemEntity(level, player.getX(), player.getY(), player.getZ(), createdStack);
        level.addFreshEntity(itemEntity);
        
        // remove blocks
        var controllerFacing = getBlockState().getValue(ControllerBlock.FACING);
        var fromCanonical = RotationUtil.rotationFromTo(Direction.SOUTH, controllerFacing);
        for (var droneBlock : droneData.getBlocks()) {
            var rotatedLocalPos = RotationUtil.rotate(droneBlock.localPos(), fromCanonical);
            var worldPos = this.worldPosition.offset(droneData.getAssemblerOffset()).offset(rotatedLocalPos);
            level.setBlockAndUpdate(worldPos, Blocks.AIR.defaultBlockState());
            
            if (level instanceof ServerLevel serverWorld) {
                var spawnAt = worldPos.getCenter();
                serverWorld.sendParticles(ParticleTypes.GUST, spawnAt.x, spawnAt.y, spawnAt.z, 1, 0, 0.1f, 0, 0.5f);
            }
            
        }
        
    }
    
    // C2S packet, contains the given name and controller pos
    public record AssembleDronePacket(String name, BlockPos controllerPos) implements CustomPacketPayload {
        
        public static final CustomPacketPayload.Type<AssembleDronePacket> PAYLOAD_ID = new CustomPacketPayload.Type<>(Drones.id("assemble"));
        
        public static final StreamCodec<ByteBuf, AssembleDronePacket> PACKET_CODEC = StreamCodec.composite(
          ByteBufCodecs.STRING_UTF8,
          AssembleDronePacket::name,
          BlockPos.STREAM_CODEC,
          AssembleDronePacket::controllerPos,
          AssembleDronePacket::new
        );
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return PAYLOAD_ID;
        }
    }
}
