package rearth.client.renderers;

import rearth.DronesClient;
import rearth.drone.DroneController;
import rearth.drone.DroneData;
import rearth.drone.RecordedBlock;
import rearth.init.NetworkContent.DroneMoveSyncPacket;
import rearth.util.FloodFill;
import rearth.util.Helpers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.HitboxesRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public class DroneRenderer {
    
    private static final HashMap<Player, Vec3> lastPositions = new HashMap<>();
    private static final HashMap<Player, Vec3> lastRotations = new HashMap<>();
    
    public static void doRender(PoseStack matrices, Camera camera, MultiBufferSource vertexConsumers) {
        var world = Minecraft.getInstance().level;
        if (world == null) return;

        var cameraRenderState = new CameraRenderState();
        cameraRenderState.pos = camera.getPosition();
        cameraRenderState.blockPos = camera.getBlockPosition();

        var collector = new ImmediateSubmitNodeCollector(vertexConsumers);
        
        for (var dronePlayer : world.players()) {
            
            var droneCandidate = DroneController.getDroneOfPlayer(dronePlayer);
            if (droneCandidate.isEmpty()) continue;
            
            var droneData = droneCandidate.get();
            
            var movementData = DronesClient.CURRENT_DATA.get(droneData.getDroneId());
            if (movementData == null) return;
            
            // Scale interpolation factor based on frame time
            var frameTimeTicks = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaTicks();
            var ftScale = frameTimeTicks * 3.0;
            
            var targetScale = 0.15f * droneData.getRenderScale();
            
            var lastPos = lastPositions.computeIfAbsent(dronePlayer, player -> movementData.position());
            var lastRot = lastRotations.computeIfAbsent(dronePlayer, player -> movementData.rotation());
            var newPos = movementData.position();
            var newRot = movementData.rotation();
            
            // adjust for switch from angle -180 to 180 on Y rotation axis
            var rotDistY = Math.abs(lastRot.y - newRot.y);
            var altRotDistY = Math.abs(-lastRot.y - newRot.y);
            if (altRotDistY < rotDistY && Math.abs(lastRot.y) > 90) {
                var positive = lastRot.y > 0;
                var adjustedY = positive ? lastRot.y - 360 : lastRot.y + 360;
                lastRot = new Vec3(lastRot.x, adjustedY, lastRot.z);
            }
            
            var posAlpha = (float) (1.0 - Math.pow(0.9, ftScale));
            var deltaDronePos = Helpers.lerp(lastPos, newPos, posAlpha);
            lastPositions.put(dronePlayer, deltaDronePos);

            var rotDelta = newRot.subtract(lastRot);
            var rotDist = (float) Math.max(Math.abs(rotDelta.x), Math.max(Math.abs(rotDelta.y), Math.abs(rotDelta.z)));
            // Ease factor scales linearly from 0.1 at 15 degrees to 0.15 at 45 degrees
            var rampT = Math.min(1.0f, Math.max(0.0f, (rotDist - 15.0f) / (45.0f - 15.0f)));
            var baseFactor = 0.05f + rampT * 0.1f;
            var easeStep = (float) (1.0 - Math.pow(1.0 - baseFactor, ftScale));
            // Cap at a fixed angular velocity (degrees per tick) to prevent whipping
            var maxDegreesPerTick = 18.0f;
            var capStep = rotDist > 0.001f ? (maxDegreesPerTick * (float) frameTimeTicks) / rotDist : 1.0f;
            // Use whichever is slower
            var rotFactor = Math.min(easeStep, capStep);
            var deltaDroneRot = Helpers.lerp(lastRot, newRot, rotFactor);
            lastRotations.put(dronePlayer, deltaDroneRot);
            
            matrices.pushPose();
            matrices.translate(-camera.getPosition().x, -camera.getPosition().y, -camera.getPosition().z);
            matrices.translate(deltaDronePos.x, deltaDronePos.y, deltaDronePos.z);
            matrices.mulPose(Axis.XP.rotationDegrees((float) -deltaDroneRot.x));
            matrices.mulPose(Axis.ZP.rotationDegrees((float) -deltaDroneRot.z));
            matrices.mulPose(Axis.YP.rotationDegrees((float) -deltaDroneRot.y));
            
            
            for (var blockData : droneData.getBlocks()) {
                var localOffset = blockData.localPos();
                var state = blockData.state();
                
                var scaledLocalOffset = Vec3.atLowerCornerOf(localOffset).add(-0.5f, -0.5f, -0.5f).scale(targetScale);
                
                matrices.pushPose();
                matrices.translate(scaledLocalOffset.x, scaledLocalOffset.y, scaledLocalOffset.z);
                matrices.scale(targetScale, targetScale, targetScale);

                // beacon faces up by default; rotate it around its center to face the drone's forward direction
                if (state.getBlock() == Blocks.BEACON) {
                    matrices.translate(0.5, 0.5, 0.5);
                    matrices.mulPose(Axis.XP.rotationDegrees(90));
                    matrices.translate(-0.5, -0.5, -0.5);
                }

                var light = getMaxLight(BlockPos.containing(movementData.position()), world);
                
                // render baked / animated block
                Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                  state,
                  matrices,
                  vertexConsumers,
                  light, OverlayTexture.NO_OVERLAY
                );
                
                // render optional custom entity renderer
                if (state.getBlock() instanceof EntityBlock blockEntityProvider) {
                    var blockEntity = blockEntityProvider.newBlockEntity(new BlockPos(localOffset), state);
                    if (blockEntity != null) {
                        blockEntity.setLevel(world);
                        var dispatcher = Minecraft.getInstance().getBlockEntityRenderDispatcher();
                        dispatcher.prepare(camera);
                        var renderState = dispatcher.tryExtractRenderState(blockEntity, 0, null);
                        if (renderState != null) {
                            dispatcher.submit(renderState, matrices, collector, cameraRenderState);
                        }
                    }
                }
                
                matrices.popPose();
            }
            
            // render carried item below drone center
            var carriedItem = DronesClient.CARRIED_ITEMS.get(droneData.getDroneId());
            if (carriedItem != null && !carriedItem.isEmpty()) {
                matrices.pushPose();
                matrices.translate(0, -0.5, 0);
                var itemLight = getMaxLight(BlockPos.containing(movementData.position()), world);
                var itemRenderState = new ItemStackRenderState();
                Minecraft.getInstance().getItemModelResolver().updateForTopItem(
                  itemRenderState,
                  carriedItem,
                  ItemDisplayContext.GROUND,
                  world,
                  null,
                  0
                );
                itemRenderState.submit(matrices, collector, itemLight, OverlayTexture.NO_OVERLAY, -1);
                matrices.popPose();
            }
            
            matrices.popPose();
        }
        
    }
    
    /**
     * Minimal SubmitNodeCollector adapter that immediately renders submitted geometry into an
     * existing PoseStack/MultiBufferSource pair, instead of deferring it for later draining.
     * Only the methods actually used by block entity renderers and item rendering in this mod
     * are implemented; everything else is a no-op.
     */
    public static class ImmediateSubmitNodeCollector implements SubmitNodeCollector, OrderedSubmitNodeCollector {

        private final MultiBufferSource bufferSource;

        public ImmediateSubmitNodeCollector(MultiBufferSource bufferSource) {
            this.bufferSource = bufferSource;
        }

        @Override
        public OrderedSubmitNodeCollector order(int order) {
            return this;
        }

        @Override
        public void submitHitbox(PoseStack poseStack, EntityRenderState entityRenderState, HitboxesRenderState hitboxesRenderState) {
        }

        @Override
        public void submitShadow(PoseStack poseStack, float radius, List<EntityRenderState.ShadowPiece> shadowPieces) {
        }

        @Override
        public void submitNameTag(PoseStack poseStack, @Nullable Vec3 pos, int packedLight, Component text, boolean seeThrough, int backgroundColor, double distanceSq, CameraRenderState cameraRenderState) {
        }

        @Override
        public void submitText(PoseStack poseStack, float x, float y, FormattedCharSequence text, boolean dropShadow, Font.DisplayMode displayMode, int packedLight, int backgroundColor, int color, int outlineColor) {
        }

        @Override
        public void submitFlame(PoseStack poseStack, EntityRenderState entityRenderState, Quaternionf quaternionf) {
        }

        @Override
        public void submitLeash(PoseStack poseStack, EntityRenderState.LeashState leashState) {
        }

        @Override
        public <S> void submitModel(Model<? super S> model, S state, PoseStack poseStack, RenderType renderType, int packedLight, int packedOverlay, int color, @Nullable TextureAtlasSprite sprite, int outlineColor, @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
            var vertexConsumer = bufferSource.getBuffer(renderType);
            model.renderToBuffer(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        }

        @Override
        public void submitModelPart(ModelPart modelPart, PoseStack poseStack, RenderType renderType, int packedLight, int packedOverlay, @Nullable TextureAtlasSprite sprite, boolean outline, boolean crumbling, int outlineColor, @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay, int color) {
            var vertexConsumer = bufferSource.getBuffer(renderType);
            modelPart.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        }

        @Override
        public void submitBlock(PoseStack poseStack, BlockState state, int packedLight, int packedOverlay, int outlineColor) {
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state, poseStack, bufferSource, packedLight, packedOverlay);
        }

        @Override
        public void submitMovingBlock(PoseStack poseStack, MovingBlockRenderState movingBlockRenderState) {
        }

        @Override
        public void submitBlockModel(PoseStack poseStack, RenderType renderType, BlockStateModel model, float r, float g, float b, int packedLight, int packedOverlay, int outlineColor) {
            var vertexConsumer = bufferSource.getBuffer(renderType);
            ModelBlockRenderer.renderModel(poseStack.last(), vertexConsumer, model, r, g, b, packedLight, packedOverlay);
        }

        @Override
        public void submitItem(PoseStack poseStack, ItemDisplayContext displayContext, int packedLight, int packedOverlay, int outlineColor, int[] tintLayers, List<BakedQuad> quads, RenderType renderType, ItemStackRenderState.FoilType foilType) {
            ItemRenderer.renderItem(displayContext, poseStack, bufferSource, packedLight, packedOverlay, tintLayers, quads, renderType, foilType);
        }

        @Override
        public void submitCustomGeometry(PoseStack poseStack, RenderType renderType, SubmitNodeCollector.CustomGeometryRenderer renderer) {
            var vertexConsumer = bufferSource.getBuffer(renderType);
            renderer.render(poseStack.last(), vertexConsumer);
        }

        @Override
        public void submitParticleGroup(SubmitNodeCollector.ParticleGroupRenderer particleGroupRenderer) {
        }
    }

    private static int getMaxLight(BlockPos center, Level world) {
        var bestLight = LevelRenderer.getLightColor(world, center);
        
        for (var side : FloodFill.GetNeighbors(center)) {
            var candidate = LevelRenderer.getLightColor(world, side);
            bestLight = Math.max(candidate, bestLight);
        }
        
        return bestLight;
    }
    
}
