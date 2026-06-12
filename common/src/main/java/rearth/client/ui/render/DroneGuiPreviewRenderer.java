package rearth.client.ui.render;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import rearth.client.renderers.DroneRenderer;

public class DroneGuiPreviewRenderer extends PictureInPictureRenderer<DroneGuiPreviewRenderState> {

    private final BlockRenderDispatcher blockRenderDispatcher;
    private final BlockEntityRenderDispatcher blockEntityRenderDispatcher;

    public DroneGuiPreviewRenderer(MultiBufferSource.BufferSource bufferSource, BlockRenderDispatcher blockRenderDispatcher, BlockEntityRenderDispatcher blockEntityRenderDispatcher) {
        super(bufferSource);
        this.blockRenderDispatcher = blockRenderDispatcher;
        this.blockEntityRenderDispatcher = blockEntityRenderDispatcher;
    }

    @Override
    public Class<DroneGuiPreviewRenderState> getRenderStateClass() {
        return DroneGuiPreviewRenderState.class;
    }

    @Override
    protected void renderToTexture(DroneGuiPreviewRenderState state, PoseStack poseStack) {

        var minecraft = Minecraft.getInstance();
        minecraft.gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_3D);

        var droneRenderScale = state.droneData().getRenderScale();
        poseStack.scale(droneRenderScale, droneRenderScale, droneRenderScale);
        poseStack.mulPose(Axis.XP.rotationDegrees(180));

        poseStack.mulPose(Axis.XP.rotationDegrees(state.xRotation()));
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yRotation()));

        var dispatcher = this.blockEntityRenderDispatcher;
        dispatcher.prepare(minecraft.gameRenderer.getMainCamera());

        var collector = new DroneRenderer.ImmediateSubmitNodeCollector(this.bufferSource);
        var cameraRenderState = new CameraRenderState();

        for (var pair : state.droneData().getBlocks()) {
            var localPos = pair.localPos();
            var blockState = pair.state();

            poseStack.pushPose();
            poseStack.translate(-0.5 + localPos.getX(), -0.5 + localPos.getY(), -0.5 + localPos.getZ());

            this.blockRenderDispatcher.renderSingleBlock(blockState, poseStack, this.bufferSource, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);

            if (blockState.getBlock() instanceof EntityBlock blockEntityProvider) {
                var blockEntity = blockEntityProvider.newBlockEntity(new BlockPos(localPos), blockState);
                if (blockEntity != null) {
                    blockEntity.setLevel(minecraft.level);
                    var renderState = dispatcher.tryExtractRenderState(blockEntity, 0, null);
                    if (renderState != null) {
                        dispatcher.submit(renderState, poseStack, collector, cameraRenderState);
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
