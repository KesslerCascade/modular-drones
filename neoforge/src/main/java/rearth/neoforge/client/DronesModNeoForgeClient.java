package rearth.neoforge.client;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import rearth.Drones;
import rearth.DronesClient;
import rearth.client.renderers.DroneRenderer;
import rearth.client.ui.render.DroneGuiPreviewRenderState;
import rearth.client.ui.render.DroneGuiPreviewRenderer;

@Mod(value = Drones.MOD_ID, dist = Dist.CLIENT)
public class DronesModNeoForgeClient {

    public DronesModNeoForgeClient(IEventBus eventBus) {
        eventBus.addListener((FMLClientSetupEvent event) -> DronesClient.init());
        eventBus.addListener((RegisterPictureInPictureRenderersEvent event) -> event.register(
          DroneGuiPreviewRenderState.class,
          bufferSource -> new DroneGuiPreviewRenderer(bufferSource, Minecraft.getInstance().getBlockEntityRenderDispatcher())
        ));
    }

    @EventBusSubscriber(modid = Drones.MOD_ID, value = Dist.CLIENT)
    public static class CustomEvents {

        @SubscribeEvent
        public static void onWorldRender(RenderLevelStageEvent.AfterTranslucentFeatures event) {
            var camera = Minecraft.getInstance().gameRenderer.getMainCamera();
            DroneRenderer.doRender(event.getPoseStack(), camera, Minecraft.getInstance().renderBuffers().bufferSource());
        }
    }
}
