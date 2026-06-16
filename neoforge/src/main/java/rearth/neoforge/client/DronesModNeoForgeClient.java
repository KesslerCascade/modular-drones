package rearth.neoforge.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import rearth.Drones;
import rearth.DronesClient;
import rearth.client.particles.IonTrailParticleProvider;
import rearth.init.ParticleContent;

@Mod(value = Drones.MOD_ID, dist = Dist.CLIENT)
public class DronesModNeoForgeClient {

    public DronesModNeoForgeClient(IEventBus eventBus) {
        eventBus.addListener((FMLClientSetupEvent event) -> DronesClient.init());
        eventBus.addListener((RegisterParticleProvidersEvent event) -> event.registerSpriteSet(ParticleContent.ION_TRAIL.get(), IonTrailParticleProvider::new));
    }
}
