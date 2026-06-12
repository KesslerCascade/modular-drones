package rearth.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import rearth.DronesClient;
import rearth.client.renderers.DroneRenderer;

public final class DronesModFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        DronesClient.init();
        WorldRenderEvents.AFTER_ENTITIES.register(DronesModFabricClient::renderWorld);
    }
    
    private static void renderWorld(WorldRenderContext worldRenderContext) {
        
        var matrices = worldRenderContext.matrices();
        var camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        var vertexConsumers = worldRenderContext.consumers();
        
        DroneRenderer.doRender(matrices, camera, vertexConsumers);
        
    }
    
    
}
