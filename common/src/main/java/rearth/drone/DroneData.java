package rearth.drone;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.BlockState;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.Vec3i;
import org.jetbrains.annotations.NotNull;
import rearth.Drones;
import rearth.drone.behaviour.*;
import rearth.init.TagContent;
import rearth.util.Helpers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;

import static rearth.blocks.controller.ControllerBlockEntity.*;

public class DroneData {
    
    // synced to client / persisted
    private final List<RecordedBlock> blocks;
    private final int size;
    private final int droneId;
    private final Vec3i assemblerOffset;
    
    // calculated on both sides
    public final EnumSet<DroneBehaviour.BlockFunctions> installed;
    public final float power;
    public final List<DroneSensor> enabledSensors;
    
    public DroneData(
      @NotNull List<RecordedBlock> blocks, int id, Vec3i assemblerOffset) {
        
        this.blocks = blocks;
        this.droneId = id;
        this.assemblerOffset = assemblerOffset;
        
        // calculate drone data
        var weight = 0f;
        var thrust = 0f;
        var abilities = new ArrayList<DroneBehaviour.BlockFunctions>();
        abilities.add(DroneBehaviour.BlockFunctions.FLIGHT);
        
        var droneFrame = new HashMap<Vec3i, BlockState>();
        blocks.forEach(block -> droneFrame.put(block.localPos(), block.state()));
        
        var minX = 0;
        var minZ = 0;
        var maxX = 0;
        var maxZ = 0;
        
        for (var recordedBlock : blocks) {
            
            var state = recordedBlock.state();
            if (!state.isIn(TagContent.THRUSTER_BLOCKS) && !state.isAir() && state.blocksMovement())
                weight += 2;
            
            thrust += getThrust(recordedBlock, droneFrame);
            
            if (ArrowAttackBehaviour.isValid(recordedBlock, droneFrame)) {
                abilities.add(DroneBehaviour.BlockFunctions.ARROW_LAUNCHER);
            }
            if (MeleeAttackBehaviour.isValid(recordedBlock, droneFrame)) {
                abilities.add(DroneBehaviour.BlockFunctions.MELEE_ATTACK);
            }
            if (MiningSupportBehaviour.isValid(recordedBlock, droneFrame)) {
                abilities.add(DroneBehaviour.BlockFunctions.MINING_SUPPORT);
            }
            if (PickupBehaviour.isValid(recordedBlock, droneFrame)) {
                abilities.add(DroneBehaviour.BlockFunctions.PICKUP);
            }
            if (BeamAttackBehaviour.isValid(recordedBlock, droneFrame)) {
                abilities.add(DroneBehaviour.BlockFunctions.BEAM);
            }
//            if (state.isIn(TagContent.AXE_TOOLS)) {
//                abilities.add(DroneBehaviour.BlockFunctions.SAW);
//            }
            
            if (state.getLuminance() > 5) {
                abilities.add(DroneBehaviour.BlockFunctions.LIGHT);
            }
            
            var localPos = recordedBlock.localPos();
            if (localPos.getX() < minX)
                minX = localPos.getX();
            if (localPos.getZ() < minZ)
                minZ = localPos.getZ();
            if (localPos.getX() > maxX)
                maxX = localPos.getX();
            if (localPos.getZ() > maxZ)
                maxZ = localPos.getZ();
            
        }
        
        var sizeX = maxX - minX + 1;
        var sizeZ = maxZ - minZ + 1;
        this.size = Math.max(sizeX, sizeZ);
        
        var thrusterRatio = thrust / Math.max(weight, 1);
        
        if (thrusterRatio < 1)
            thrusterRatio = (float) Math.sqrt(thrusterRatio);
        
        if (thrusterRatio > 6)
            thrusterRatio = (float) Math.sqrt(thrusterRatio) + 3.55f;
        
        this.power = thrusterRatio;
        this.installed = EnumSet.copyOf(abilities);
        this.enabledSensors = getInstalledSensors(installed);
    }
    
    public List<RecordedBlock> getBlocks() {
        return blocks;
    }
    
    public int getSize() {
        return size;
    }
    
    public float getRenderScale() {
        var defaultSize = 6f;
        return defaultSize / size;
    }
    
    public boolean isGlowing() {
        return installed.contains(DroneBehaviour.BlockFunctions.LIGHT);
    }
    
    public boolean isValid() {
        return power > 0.01f && !this.getBlocks().isEmpty();
    }
    
    
    public int getDroneId() {
        return droneId;
    }
    
    public Vec3i getAssemblerOffset() {
        return assemblerOffset;
    }
    
    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        
        DroneData droneData = (DroneData) o;
        return droneId == droneData.droneId;
    }
    
    @Override
    public int hashCode() {
        return droneId;
    }
    
    private static List<DroneSensor> getInstalledSensors(EnumSet<DroneBehaviour.BlockFunctions> functions) {
        var sensors = new ArrayList<DroneSensor>();
        
        if (functions.contains(DroneBehaviour.BlockFunctions.BEAM))
            sensors.add(new BeamAttackBehaviour.BeamAttackSensor());
        if (functions.contains(DroneBehaviour.BlockFunctions.ARROW_LAUNCHER))
            sensors.add(new ArrowAttackBehaviour.ArrowAttackSensor());
        if (functions.contains(DroneBehaviour.BlockFunctions.MELEE_ATTACK))
            sensors.add(new MeleeAttackBehaviour.MeleeAttackSensor());
        if (functions.contains(DroneBehaviour.BlockFunctions.PICKUP))
            sensors.add(new PickupBehaviour.PickupSensor());

        sensors.sort(Comparator.comparingInt(DroneSensor::getPriority).reversed());
        return sensors;
    }
    
    private static float getThrust(RecordedBlock block, HashMap<Vec3i, BlockState> frame) {
        
        // thrusters need to have 2 empty blocks below
        
        var isThruster = block.state().isIn(TagContent.THRUSTER_BLOCKS);
        if (!isThruster) return 0;
        
        var blocked = frame.containsKey(block.localPos().down()) || frame.containsKey(block.localPos().down(2));
        if (blocked) return 0;
        
        var state = block.state();
        if (state.isIn(TagContent.LOW_THRUSTER)) {
            return LOW_THRUSTER_POWER;
        }
        if (state.isIn(TagContent.MEDIUM_THRUSTER)) {
            return MEDIUM_THRUSTER_POWER;
        }
        if (state.isIn(TagContent.HIGH_THRUSTER)) {
            return HIGH_THRUSTER_POWER;
        }
        if (state.isIn(TagContent.ULTRA_THRUSTER)) {
            return ULTRA_THRUSTER_POWER;
        }
        
        Drones.LOGGER.warn("Found thruster with thruster tag, but without a thrust strength tag: " + state);
        
        return 0;
        
    }
    
    public static PacketCodec<ByteBuf, DroneData> PACKET_CODEC = PacketCodec.tuple(
      RecordedBlock.PACKET_CODEC.collect(PacketCodecs.toList()), DroneData::getBlocks,
      PacketCodecs.INTEGER, DroneData::getDroneId,
      Helpers.VEC3I_PACKET_CODEC, DroneData::getAssemblerOffset,
      DroneData::new
    );
    
    public static Codec<DroneData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      RecordedBlock.CODEC.listOf().fieldOf("blocks").forGetter(DroneData::getBlocks),
      Codecs.POSITIVE_INT.fieldOf("id").forGetter(DroneData::getDroneId),
      Vec3i.CODEC.fieldOf("offset").forGetter(DroneData::getAssemblerOffset)
      ).apply(instance, DroneData::new));
}
