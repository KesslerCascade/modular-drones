package rearth.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import rearth.DronesClient;
import rearth.client.renderers.DroneRenderer;

public final class DronesModFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        DronesClient.init();
        LevelRenderEvents.AFTER_TRANSLUCENT_FEATURES.register(DronesModFabricClient::renderWorld);
    }

    private static void renderWorld(LevelRenderContext context) {

        var matrices = context.poseStack();
        var camera = context.gameRenderer().getMainCamera();
        var vertexConsumers = context.bufferSource();

        DroneRenderer.doRender(matrices, camera, vertexConsumers);

    }


}
