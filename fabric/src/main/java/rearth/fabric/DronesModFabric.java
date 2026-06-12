package rearth.fabric;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.world.InteractionResult;
import rearth.Drones;
import rearth.drone.DroneController;

public final class DronesModFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        
        Drones.init();
        
        AttackBlockCallback.EVENT.register(((playerEntity, world, hand, blockPos, direction) -> {
            if (!world.isClientSide())
                DroneController.onPlayerBlockBreakStart(playerEntity, blockPos);
            return InteractionResult.PASS;
        }));
    }
}
