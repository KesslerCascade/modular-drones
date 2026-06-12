package rearth.neoforge.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.neoforged.neoforge.client.gui.PictureInPictureRendererRegistration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import rearth.client.ui.render.DroneGuiPreviewRenderState;
import rearth.client.ui.render.DroneGuiPreviewRenderer;

import java.util.ArrayList;
import java.util.List;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @ModifyArg(
      method = "<init>",
      at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/client/gui/render/GuiRenderer;<init>(Lnet/minecraft/client/gui/render/state/GuiRenderState;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher;Ljava/util/List;)V"
      ),
      index = 4
    )
    private List<PictureInPictureRendererRegistration<?>> drones$addPreviewRenderer(List<PictureInPictureRendererRegistration<?>> renderers) {
        var extended = new ArrayList<>(renderers);
        extended.add(new PictureInPictureRendererRegistration<>(
          DroneGuiPreviewRenderState.class,
          bufferSource -> new DroneGuiPreviewRenderer(bufferSource, Minecraft.getInstance().getBlockRenderer(), Minecraft.getInstance().getBlockEntityRenderDispatcher())
        ));
        return extended;
    }
}
