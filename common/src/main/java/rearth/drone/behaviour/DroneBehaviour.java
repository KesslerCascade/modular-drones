package rearth.drone.behaviour;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public interface DroneBehaviour {
    
    void tick();
    float getCurrentYaw();
    int getPriority();  // higher = better, scale 0 - 100
    
    default void onStopped() {};
    default float getExtraRoll() {return 0f;}
    
    enum BlockFunctions {
        ARROW_LAUNCHER, LIGHT, MELEE_ATTACK, MINING_SUPPORT, PICKUP, SAW, FLIGHT, BEAM
    }
    
    static Item getItem(BlockFunctions function) {
        
        return switch (function) {
            case ARROW_LAUNCHER -> Items.BOW;
            case LIGHT -> Items.LANTERN;
            case MELEE_ATTACK -> Items.GOLDEN_SWORD;
            case MINING_SUPPORT -> Items.GOLDEN_PICKAXE;
            case PICKUP -> Items.HOPPER;
            case SAW -> Items.SPRUCE_SAPLING;
            case FLIGHT -> Items.ELYTRA;
            case BEAM -> Items.BEACON;
        };
        
    }
    
}
