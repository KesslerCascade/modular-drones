package rearth.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import rearth.DronesClient;
import rearth.client.particles.IonTrailParticleProvider;
import rearth.client.renderers.DroneRenderer;
import rearth.init.ParticleContent;

public final class DronesModFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        DronesClient.init();
        WorldRenderEvents.AFTER_ENTITIES.register(DronesModFabricClient::renderWorld);

        for (var entry : DronesClient.RENDER_LAYERS.entrySet()) {
            BlockRenderLayerMap.INSTANCE.putBlock(entry.getKey().get(), entry.getValue());
        }

        ParticleFactoryRegistry.getInstance().register(ParticleContent.ION_TRAIL.get(), new IonTrailParticleProvider());
    }
    
    private static void renderWorld(WorldRenderContext worldRenderContext) {
        
        var matrices = worldRenderContext.matrices();
        var camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        var vertexConsumers = worldRenderContext.consumers();
        
        DroneRenderer.doRender(matrices, camera, vertexConsumers);
        
    }
    
    
}
