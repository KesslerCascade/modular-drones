package rearth.fabric.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.GameRenderer;
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
        // 26.2: GuiRenderer.<init>(GuiRenderState, FeatureRenderDispatcher, List)
        target = "Lnet/minecraft/client/gui/render/GuiRenderer;<init>(Lnet/minecraft/client/renderer/state/gui/GuiRenderState;Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher;Ljava/util/List;)V"
      ),
      index = 2
    )
    private List<PictureInPictureRenderer<? extends PictureInPictureRenderState>> drones$addPreviewRenderer(List<PictureInPictureRenderer<? extends PictureInPictureRenderState>> renderers) {
        var extended = new ArrayList<>(renderers);
        extended.add(new DroneGuiPreviewRenderer(Minecraft.getInstance().getBlockEntityRenderDispatcher()));
        return extended;
    }
}
