package rearth.drone.behaviour;

import net.minecraft.world.entity.player.Player;
import rearth.drone.DroneServerData;

public interface DroneSensor {
    
    int getPriority();
    boolean sense(DroneServerData drone, Player player);    // returns true if something has been found
    
}
