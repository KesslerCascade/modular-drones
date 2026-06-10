package rearth.drone.behaviour;

import rearth.drone.DroneServerData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class DroneLight {
    
    private static final Map<GlobalPos, Long> activeLights = new HashMap<>();
    private static final Map<DroneServerData, BlockPos> droneLights = new HashMap<>();
    
    // to ensure lights are always cleaned up
    public static void removeOldLights(Level world) {
        
        var removed = new HashSet<GlobalPos>();
        
        for (var pair : activeLights.entrySet()) {
            if (pair.getValue() < world.getGameTime() && pair.getKey().dimension().equals(world.dimension())) {
                removeDroneLight(pair.getKey().pos(), world);
                removed.add(pair.getKey());
            }
        }
        
        removed.forEach(activeLights::remove);
    }
    
    public static void updateDroneLight(DroneServerData drone, Level world) {
        
        var targetPos = BlockPos.containing(drone.currentPosition);
        var lastPos = droneLights.get(drone);
        if (lastPos != null) {
            
            if (lastPos.equals(targetPos)) {    // update timestamp
                activeLights.put(GlobalPos.of(world.dimension(), targetPos), world.getGameTime() + 20);
            } else {    // or remove the light if we moved
                activeLights.remove(GlobalPos.of(world.dimension(), lastPos));
                removeDroneLight(lastPos, world);
                createDroneLight(drone, targetPos, world);
            }
            
        } else {
            createDroneLight(drone, targetPos, world);
        }
        
    }
    
    private static void removeDroneLight(BlockPos pos, Level world) {
        var existingState = world.getBlockState(pos);
        if (!existingState.is(Blocks.LIGHT)) return;
        
        world.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
    }
    
    private static void createDroneLight(DroneServerData data, BlockPos pos, Level world) {
        
        var existingState = world.getBlockState(pos);
        if (!existingState.isAir()) return;
        
        world.setBlockAndUpdate(pos, Blocks.LIGHT.defaultBlockState());
        
        droneLights.put(data, pos);
        activeLights.put(GlobalPos.of(world.dimension(), pos), world.getGameTime() + 20);
        
    }
    
}
