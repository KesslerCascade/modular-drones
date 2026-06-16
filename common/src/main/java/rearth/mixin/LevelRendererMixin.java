package rearth.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rearth.client.renderers.DroneRenderer;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    @Inject(
      method = "submitFeatures(Lnet/minecraft/client/renderer/LevelRenderState;Lnet/minecraft/client/renderer/SubmitNodeCollector;Z)V",
      at = @At("TAIL")
    )
    private void drones$submitDroneFeatures(
      LevelRenderState levelRenderState, SubmitNodeCollector submitNodeCollector, boolean renderOutline,
      CallbackInfo ci
    ) {
        var camera = Minecraft.getInstance().gameRenderer.mainCamera();
        DroneRenderer.doRender(new PoseStack(), camera, submitNodeCollector);
    }
}
