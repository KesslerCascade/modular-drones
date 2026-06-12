package rearth.fabric.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
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
    private List<PictureInPictureRenderer<? extends PictureInPictureRenderState>> drones$addPreviewRenderer(List<PictureInPictureRenderer<? extends PictureInPictureRenderState>> renderers) {
        var minecraft = Minecraft.getInstance();
        var bufferSource = (MultiBufferSource.BufferSource) minecraft.renderBuffers().bufferSource();

        var extended = new ArrayList<>(renderers);
        extended.add(new DroneGuiPreviewRenderer(bufferSource, minecraft.getBlockRenderer(), minecraft.getBlockEntityRenderDispatcher()));
        return extended;
    }
}
