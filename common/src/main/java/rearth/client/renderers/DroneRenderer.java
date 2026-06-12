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
import java.util.ArrayList;
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
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.RandomSource;
import com.mojang.blaze3d.vertex.QuadInstance;
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
        cameraRenderState.pos = camera.position();
        cameraRenderState.blockPos = camera.blockPosition();

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
            matrices.translate(-camera.position().x, -camera.position().y, -camera.position().z);
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
                renderSingleBlock(state, matrices, collector, world, BlockPos.containing(movementData.position()), light, OverlayTexture.NO_OVERLAY);
                
                // render optional custom entity renderer
                if (state.getBlock() instanceof EntityBlock blockEntityProvider) {
                    var blockEntity = blockEntityProvider.newBlockEntity(new BlockPos(localOffset), state);
                    if (blockEntity != null) {
                        blockEntity.setLevel(world);
                        var dispatcher = Minecraft.getInstance().getBlockEntityRenderDispatcher();
                        dispatcher.prepare(camera.position());
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
        public void submitMovingBlock(PoseStack poseStack, MovingBlockRenderState movingBlockRenderState) {
        }

        @Override
        public void submitBreakingBlockModel(PoseStack poseStack, BlockStateModel model, long seed, int progress) {
        }

        @Override
        public void submitBlockModel(PoseStack poseStack, RenderType renderType, List<BlockStateModelPart> parts, int[] tintLayers, int packedLight, int packedOverlay, int outlineColor) {
            var vertexConsumer = bufferSource.getBuffer(renderType);
            var pose = poseStack.last();
            var quadInstance = new QuadInstance();
            quadInstance.setLightCoords(packedLight);
            quadInstance.setOverlayCoords(packedOverlay);

            for (var part : parts) {
                for (var direction : Direction.values()) {
                    for (var quad : part.getQuads(direction)) {
                        putQuad(pose, quad, quadInstance, tintLayers, vertexConsumer);
                    }
                }
                for (var quad : part.getQuads(null)) {
                    putQuad(pose, quad, quadInstance, tintLayers, vertexConsumer);
                }
            }
        }

        private static void putQuad(PoseStack.Pose pose, BakedQuad quad, QuadInstance quadInstance, int[] tintLayers, com.mojang.blaze3d.vertex.VertexConsumer buffer) {
            var tintIndex = quad.materialInfo().tintIndex();
            var hasTint = tintIndex != -1 && tintIndex < tintLayers.length;
            quadInstance.setColor(hasTint ? tintLayers[tintIndex] : -1);
            buffer.putBakedQuad(pose, quad, quadInstance);
        }

        @Override
        public void submitItem(PoseStack poseStack, ItemDisplayContext displayContext, int packedLight, int packedOverlay, int outlineColor, int[] tintLayers, List<BakedQuad> quads, ItemStackRenderState.FoilType foilType) {
            var pose = poseStack.last();
            var quadInstance = new QuadInstance();
            quadInstance.setLightCoords(packedLight);
            quadInstance.setOverlayCoords(packedOverlay);

            for (var quad : quads) {
                var material = quad.materialInfo();
                var renderType = material.itemRenderType();
                var hasTint = material.isTinted();
                var tintIndex = material.tintIndex();
                quadInstance.setColor(hasTint && tintIndex >= 0 && tintIndex < tintLayers.length ? tintLayers[tintIndex] : -1);
                bufferSource.getBuffer(renderType).putBakedQuad(pose, quad, quadInstance);
            }
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
        var bestLight = LevelRenderer.getLightCoords(world, center);

        for (var side : FloodFill.GetNeighbors(center)) {
            var candidate = LevelRenderer.getLightCoords(world, side);
            bestLight = Math.max(candidate, bestLight);
        }

        return bestLight;
    }

    public static void renderSingleBlock(BlockState state, PoseStack poseStack, ImmediateSubmitNodeCollector collector, ClientLevel world, BlockPos pos, int packedLight, int packedOverlay) {
        var model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(state);
        var parts = new ArrayList<BlockStateModelPart>();
        model.collectParts(RandomSource.create(state.getSeed(pos)), parts);
        var renderType = model.hasMaterialFlag(BakedQuad.FLAG_TRANSLUCENT) ? Sheets.translucentBlockSheet() : Sheets.cutoutBlockSheet();
        var tintLayers = computeTintLayers(world, state, pos);
        collector.submitBlockModel(poseStack, renderType, parts, tintLayers, packedLight, packedOverlay, 0);
    }

    private static int[] computeTintLayers(ClientLevel world, BlockState state, BlockPos pos) {
        var sources = Minecraft.getInstance().getBlockColors().getTintSources(state);
        var tints = new int[sources.size()];
        for (int i = 0; i < sources.size(); i++) {
            tints[i] = sources.get(i).colorInWorld(state, world, pos);
        }
        return tints;
    }

}
