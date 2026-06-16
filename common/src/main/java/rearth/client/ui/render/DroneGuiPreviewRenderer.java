package rearth.client.ui.render;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import rearth.client.renderers.DroneRenderer;

public class DroneGuiPreviewRenderer extends PictureInPictureRenderer<DroneGuiPreviewRenderState> {

    private static final int FULL_BRIGHT = 0xF000F0;

    private final BlockEntityRenderDispatcher blockEntityRenderDispatcher;

    public DroneGuiPreviewRenderer(BlockEntityRenderDispatcher blockEntityRenderDispatcher) {
        this.blockEntityRenderDispatcher = blockEntityRenderDispatcher;
    }

    @Override
    public Class<DroneGuiPreviewRenderState> getRenderStateClass() {
        return DroneGuiPreviewRenderState.class;
    }

    @Override
    protected void renderToTexture(DroneGuiPreviewRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector) {

        var minecraft = Minecraft.getInstance();
        minecraft.gameRenderer.lighting().setupFor(Lighting.Entry.ITEMS_3D);

        var droneRenderScale = state.droneData().getRenderScale();
        poseStack.scale(droneRenderScale, droneRenderScale, droneRenderScale);
        poseStack.mulPose(Axis.XP.rotationDegrees(180));

        poseStack.mulPose(Axis.XP.rotationDegrees(state.xRotation()));
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yRotation()));

        var dispatcher = this.blockEntityRenderDispatcher;
        dispatcher.prepare(minecraft.gameRenderer.mainCamera().position());

        var cameraRenderState = new CameraRenderState();

        for (var pair : state.droneData().getBlocks()) {
            var localPos = pair.localPos();
            var blockState = pair.state();

            poseStack.pushPose();
            poseStack.translate(-0.5 + localPos.getX(), -0.5 + localPos.getY(), -0.5 + localPos.getZ());

            DroneRenderer.renderSingleBlock(blockState, poseStack, submitNodeCollector, minecraft.level, new BlockPos(localPos), FULL_BRIGHT, OverlayTexture.NO_OVERLAY);

            if (blockState.getBlock() instanceof EntityBlock blockEntityProvider) {
                var blockEntity = blockEntityProvider.newBlockEntity(new BlockPos(localPos), blockState);
                if (blockEntity != null) {
                    blockEntity.setLevel(minecraft.level);
                    var renderState = dispatcher.tryExtractRenderState(blockEntity, 0, null);
                    if (renderState != null) {
                        dispatcher.submit(renderState, poseStack, submitNodeCollector, cameraRenderState);
                    }
                }
            }

            poseStack.popPose();
        }
    }

    @Override
    protected float getTranslateY(int height, int guiScale) {
        return (float) height / 2.0F;
    }

    @Override
    protected String getTextureLabel() {
        return "drone preview";
    }
}
