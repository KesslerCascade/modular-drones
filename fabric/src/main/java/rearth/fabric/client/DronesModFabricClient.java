package rearth.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import rearth.DronesClient;
import rearth.client.particles.IonTrailParticleProvider;
import rearth.init.ParticleContent;

public final class DronesModFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        DronesClient.init();
        ParticleProviderRegistry.getInstance().register(ParticleContent.ION_TRAIL.get(), IonTrailParticleProvider::new);
    }
}
