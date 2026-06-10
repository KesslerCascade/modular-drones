package rearth.fabric.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map.Entry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import rearth.DronesClient;
import rearth.client.renderers.DroneRenderer;

public final class DronesModFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        DronesClient.init();
        WorldRenderEvents.AFTER_ENTITIES.register(DronesModFabricClient::renderWorld);
        
        for (var entry : DronesClient.RENDER_LAYERS.entrySet()) {
            BlockRenderLayerMap.INSTANCE.putBlock(entry.getKey().get(), entry.getValue());
        }
    }
    
    private static void renderWorld(WorldRenderContext worldRenderContext) {
        
        var matrices = worldRenderContext.matrixStack();
        var camera = worldRenderContext.camera();
        var vertexConsumers = worldRenderContext.consumers();
        
        DroneRenderer.doRender(matrices, camera, vertexConsumers);
        
    }
    
    
}
