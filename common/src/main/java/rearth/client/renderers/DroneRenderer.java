package rearth.client.renderers;

import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import rearth.DronesClient;
import rearth.drone.DroneController;
import rearth.util.FloodFill;
import rearth.util.Helpers;

import java.util.HashMap;

public class DroneRenderer {
    
    private static final HashMap<PlayerEntity, Vec3d> lastPositions = new HashMap<>();
    private static final HashMap<PlayerEntity, Vec3d> lastRotations = new HashMap<>();
    
    public static void doRender(MatrixStack matrices, Camera camera, VertexConsumerProvider vertexConsumers) {
        var world = MinecraftClient.getInstance().world;
        if (world == null) return;
        
        for (var dronePlayer : world.getPlayers()) {
            
            var droneCandidate = DroneController.getDroneOfPlayer(dronePlayer);
            if (droneCandidate.isEmpty()) continue;
            
            var droneData = droneCandidate.get();
            
            var movementData = DronesClient.CURRENT_DATA.get(droneData.getDroneId());
            if (movementData == null) return;
            
            // Scale interpolation factor based on frame time
            var frameTimeTicks = MinecraftClient.getInstance().getRenderTickCounter().getLastFrameDuration();
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
                lastRot = new Vec3d(lastRot.x, adjustedY, lastRot.z);
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
            
            matrices.push();
            matrices.translate(-camera.getPos().x, -camera.getPos().y, -camera.getPos().z);
            matrices.translate(deltaDronePos.x, deltaDronePos.y, deltaDronePos.z);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((float) -deltaDroneRot.x));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) -deltaDroneRot.z));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) -deltaDroneRot.y));
            
            
            for (var blockData : droneData.getBlocks()) {
                var localOffset = blockData.localPos();
                var state = blockData.state();
                
                var scaledLocalOffset = Vec3d.of(localOffset).add(-0.5f, -0.5f, -0.5f).multiply(targetScale);
                
                matrices.push();
                matrices.translate(scaledLocalOffset.x, scaledLocalOffset.y, scaledLocalOffset.z);
                matrices.scale(targetScale, targetScale, targetScale);

                // beacon faces up by default; rotate it around its center to face the drone's forward direction
                if (state.getBlock() == Blocks.BEACON) {
                    matrices.translate(0.5, 0.5, 0.5);
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));
                    matrices.translate(-0.5, -0.5, -0.5);
                }

                var light = getMaxLight(BlockPos.ofFloored(movementData.position()), world);
                
                // render baked / animated block
                MinecraftClient.getInstance().getBlockRenderManager().renderBlockAsEntity(
                  state,
                  matrices,
                  vertexConsumers,
                  light, OverlayTexture.DEFAULT_UV
                );
                
                // render optional custom entity renderer
                if (state.getBlock() instanceof BlockEntityProvider blockEntityProvider) {
                    var blockEntity = blockEntityProvider.createBlockEntity(new BlockPos(localOffset), state);
                    blockEntity.setWorld(world);
                    var renderer = MinecraftClient.getInstance().getBlockEntityRenderDispatcher().get(blockEntity);
                    if (renderer != null) {
                        renderer.render(blockEntity, 0, matrices, vertexConsumers, light, OverlayTexture.DEFAULT_UV);
                    }
                }
                
                matrices.pop();
            }
            
            // render carried item below drone center
            var carriedItem = DronesClient.CARRIED_ITEMS.get(droneData.getDroneId());
            if (carriedItem != null && !carriedItem.isEmpty()) {
                matrices.push();
                matrices.translate(0, -0.5, 0);
                var itemLight = getMaxLight(BlockPos.ofFloored(movementData.position()), world);
                MinecraftClient.getInstance().getItemRenderer().renderItem(
                  carriedItem,
                  ModelTransformationMode.GROUND,
                  itemLight,
                  OverlayTexture.DEFAULT_UV,
                  matrices,
                  vertexConsumers,
                  world,
                  0
                );
                matrices.pop();
            }
            
            matrices.pop();
        }
        
    }
    
    private static int getMaxLight(BlockPos center, World world) {
        var bestLight = WorldRenderer.getLightmapCoordinates(world, center);
        
        for (var side : FloodFill.GetNeighbors(center)) {
            var candidate = WorldRenderer.getLightmapCoordinates(world, side);
            bestLight = Math.max(candidate, bestLight);
        }
        
        return bestLight;
    }
    
}
