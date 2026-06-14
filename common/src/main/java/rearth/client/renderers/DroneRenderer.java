package rearth.client.renderers;

import rearth.Drones;
import rearth.DronesClient;
import rearth.drone.DroneController;
import rearth.drone.DroneData;
import rearth.drone.RecordedBlock;
import rearth.init.NetworkContent.DroneMoveSyncPacket;
import rearth.init.ParticleContent;
import rearth.util.FloodFill;
import rearth.util.Helpers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class DroneRenderer {

    private static final HashMap<Player, Vec3> lastPositions = new HashMap<>();
    private static final HashMap<Player, Vec3> lastRotations = new HashMap<>();

    private static final RenderType ION_GLOW_RENDER_TYPE = RenderType.eyes(Drones.id("textures/block/ion_glow.png"));
    private static final float ION_GLOW_PULSE_SPEED = 2.0f;
    private static final float ION_GLOW_PULSE_DEPTH = 0.3f;
    private static final float ION_TRAIL_SPAWN_CHANCE = 1f / 8f;
    private static final float ION_TRAIL_IDLE_CHANCE_SCALE = 0.05f;
    private static final double ION_TRAIL_VELOCITY_THRESHOLD = 0.05;
    
    public static void doRender(PoseStack matrices, Camera camera, MultiBufferSource vertexConsumers) {
        var world = Minecraft.getInstance().level;
        if (world == null) return;
        
        for (var dronePlayer : world.players()) {
            
            var droneCandidate = DroneController.getDroneOfPlayer(dronePlayer);
            if (droneCandidate.isEmpty()) continue;
            
            var droneData = droneCandidate.get();
            
            var movementData = DronesClient.CURRENT_DATA.get(droneData.getDroneId());
            if (movementData == null) return;
            
            // Scale interpolation factor based on frame time
            var frameTimeTicks = Minecraft.getInstance().getTimer().getGameTimeDeltaTicks();
            var ftScale = frameTimeTicks * 3.0;
            
            var targetScale = 0.15f * droneData.getRenderScale();
            
            var lastPos = lastPositions.computeIfAbsent(dronePlayer, player -> movementData.position());
            var lastRot = lastRotations.computeIfAbsent(dronePlayer, player -> movementData.rotation());
            var newPos = movementData.position();
            var newRot = movementData.rotation();
            
            // adjust for switch from angle -180 to 180 on Y rotation axis
            var rotDistY = Math.abs(lastRot.y - newRot.y);
            if (rotDistY > 180) {
                var positive = lastRot.y > newRot.y;
                var adjustedY = positive ? lastRot.y - 360 : lastRot.y + 360;
                lastRot = new Vec3(lastRot.x, adjustedY, lastRot.z);
            }
            
            var posAlpha = (float) (1.0 - Math.pow(0.9, ftScale));
            var deltaDronePos = Helpers.lerp(lastPos, newPos, posAlpha);
            var velocity = deltaDronePos.subtract(lastPos);
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
            
            
            var ionGlowPositions = new ArrayList<Vector3f>();

            for (var blockData : droneData.getBlocks()) {
                var localOffset = blockData.localPos();
                var state = blockData.state();

                var scaledLocalOffset = Vec3.atLowerCornerOf(localOffset).add(-0.5f, -0.5f, -0.5f).scale(targetScale);

                if (droneData.getIonThrusterPositions().contains(localOffset)) {
                    var center = Vec3.atLowerCornerOf(localOffset).add(0, -0.05f, 0).scale(targetScale);
                    var centerVec = new Vector3f((float) center.x, (float) center.y, (float) center.z);
                    matrices.last().pose().transformPosition(centerVec);
                    ionGlowPositions.add(centerVec);
                }

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
                    blockEntity.setLevel(world);
                    var renderer = Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(blockEntity);
                    if (renderer != null) {
                        renderer.render(blockEntity, 0, matrices, vertexConsumers, light, OverlayTexture.NO_OVERLAY);
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
                Minecraft.getInstance().getItemRenderer().renderStatic(
                  carriedItem,
                  ItemDisplayContext.GROUND,
                  itemLight,
                  OverlayTexture.NO_OVERLAY,
                  matrices,
                  vertexConsumers,
                  world,
                  0
                );
                matrices.popPose();
            }

            // render additive glow billboards for ion thruster blocks
            if (!ionGlowPositions.isEmpty()) {
                var glowBuffer = vertexConsumers.getBuffer(ION_GLOW_RENDER_TYPE);
                var time = System.currentTimeMillis() / 1000.0;
                for (var i = 0; i < ionGlowPositions.size(); i++) {
                    renderGlowQuad(glowBuffer, ionGlowPositions.get(i), targetScale, camera, time, i);
                }
            }

            matrices.popPose();

            // spawn occasional ion trail particles when the drone is moving, or rarely even when idle
            var velocityThreshold = ION_TRAIL_VELOCITY_THRESHOLD * frameTimeTicks;
            var isMoving = velocity.lengthSqr() > velocityThreshold * velocityThreshold;
            if (!droneData.getIonThrusterPositions().isEmpty()) {
                var chanceScale = isMoving ? 1f : ION_TRAIL_IDLE_CHANCE_SCALE;
                spawnIonTrailParticle(world, droneData, movementData, deltaDroneRot, velocity, targetScale, frameTimeTicks, chanceScale, isMoving);
            }
        }
        
    }
    
    private static void renderGlowQuad(VertexConsumer buffer, Vector3f center, float size, Camera camera, double time, int thrusterIndex) {
        var rotation = camera.rotation();
        var half = size * 0.4f;

        var v1 = new Vector3f(half, -half, 0).rotate(rotation).add(center);
        var v2 = new Vector3f(half, half, 0).rotate(rotation).add(center);
        var v3 = new Vector3f(-half, half, 0).rotate(rotation).add(center);
        var v4 = new Vector3f(-half, -half, 0).rotate(rotation).add(center);

        var light = LightTexture.FULL_BRIGHT;

        // offset each thruster's pulse phase so they don't all pulsate in sync
        var phase = thrusterIndex * 1.7;
        var t = time * ION_GLOW_PULSE_SPEED + phase;
        // layer a few sine waves at different speeds/phases plus a fast jittery component for a noisy flicker
        var wave = 0.55f * (float) Math.sin(t)
          + 0.25f * (float) Math.sin(t * 2.7 + 1.3)
          + 0.2f * (float) Math.sin(t * 9.1 + thrusterIndex * 5.2);
        var pulse = 1f - ION_GLOW_PULSE_DEPTH * Math.clamp(0.5f + 0.5f * wave, 0f, 1f);
        var brightness = (int) (255 * pulse);

        buffer.addVertex(v1.x, v1.y, v1.z).setColor(brightness, brightness, brightness, 255).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 1, 0);
        buffer.addVertex(v2.x, v2.y, v2.z).setColor(brightness, brightness, brightness, 255).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 1, 0);
        buffer.addVertex(v3.x, v3.y, v3.z).setColor(brightness, brightness, brightness, 255).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 1, 0);
        buffer.addVertex(v4.x, v4.y, v4.z).setColor(brightness, brightness, brightness, 255).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 1, 0);
    }

    private static void spawnIonTrailParticle(ClientLevel world, DroneData droneData, DroneMoveSyncPacket movementData, Vec3 deltaDroneRot, Vec3 velocity, float targetScale, float frameTimeTicks, float chanceScale, boolean isMoving) {
        var random = world.getRandom();
        if (random.nextFloat() > ION_TRAIL_SPAWN_CHANCE * frameTimeTicks * chanceScale) return;

        var thrusters = droneData.getIonThrusterPositions();
        var thrusterLocal = thrusters.get(random.nextInt(thrusters.size()));
        var localOffset = Vec3.atLowerCornerOf(thrusterLocal).scale(targetScale);

        // rotate the local thruster offset by the drone's current rotation to get its world-relative offset
        var rotatedOffset = new Vector3f((float) localOffset.x, (float) localOffset.y, (float) localOffset.z);
        var rotationMatrix = new PoseStack();
        rotationMatrix.mulPose(Axis.XP.rotationDegrees((float) -deltaDroneRot.x));
        rotationMatrix.mulPose(Axis.ZP.rotationDegrees((float) -deltaDroneRot.z));
        rotationMatrix.mulPose(Axis.YP.rotationDegrees((float) -deltaDroneRot.y));
        rotationMatrix.last().pose().transformPosition(rotatedOffset);

        var spawnPos = movementData.position().add(rotatedOffset.x, rotatedOffset.y, rotatedOffset.z);

        double vx, vy, vz;
        if (isMoving) {
            var velocityDir = velocity.lengthSqr() > 1e-8 ? velocity.normalize() : Vec3.ZERO;
            var driftSpeed = 0.03;
            vx = -velocityDir.x * driftSpeed + (random.nextDouble() - 0.5) * 0.02;
            vy = -velocityDir.y * driftSpeed + (random.nextDouble() - 0.5) * 0.02;
            vz = -velocityDir.z * driftSpeed + (random.nextDouble() - 0.5) * 0.02;
        } else {
            // drone is hovering in place - exhaust drifts mostly downward, like thrust supporting its weight
            var driftSpeed = 0.03;
            vx = (random.nextDouble() - 0.5) * 0.02;
            vy = -driftSpeed + (random.nextDouble() - 0.5) * 0.01;
            vz = (random.nextDouble() - 0.5) * 0.02;
        }

        world.addParticle(ParticleContent.ION_TRAIL.get(), spawnPos.x, spawnPos.y, spawnPos.z, vx, vy, vz);
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
