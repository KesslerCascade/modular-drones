package rearth.neoforge;

import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import rearth.Drones;
import rearth.drone.DroneController;

@Mod(Drones.MOD_ID)
public final class DronesModNeoForge {
    public DronesModNeoForge() {
        // Run our common setup.
        Drones.init();
    }
    
    
    
    // No idea why this needs to be another class, but oh well.
    @EventBusSubscriber(modid = Drones.MOD_ID)
    static class CustomEvents {
    
        @SubscribeEvent
        public static void onPlayerStartBreakingBlock(PlayerInteractEvent.LeftClickBlock event) {
            var world = event.getEntity().level();
            if (!world.isClientSide())
                DroneController.onPlayerBlockBreakStart(event.getEntity(), event.getPos());
        }
    
    }
    
}
