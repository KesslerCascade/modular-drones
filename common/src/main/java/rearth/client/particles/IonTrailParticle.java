package rearth.client.particles;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import rearth.Drones;

public class IonTrailParticle extends SingleQuadParticle {

    protected IonTrailParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
        super(level, x, y, z);
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        this.hasPhysics = false;
        this.gravity = 0f;
        this.friction = 1f;
        this.lifetime = 20 + this.random.nextInt(15);
        this.quadSize = 0.05f * (this.random.nextFloat() * 0.5f + 0.5f);
        this.setColor(0.6f, 0.85f, 1f);
        this.alpha = 0.8f;
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        this.alpha = 0.8f * Math.max(0f, 1f - ((float) this.age + partialTicks) / (float) this.lifetime);

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA, com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE);
        RenderSystem.setShaderTexture(0, Drones.id("textures/particle/ion_trail.png"));

        super.render(buffer, camera, partialTicks);
    }

    @Override
    protected int getLightColor(float partialTick) {
        return net.minecraft.client.renderer.LightTexture.FULL_BRIGHT;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.CUSTOM;
    }

    @Override
    protected float getU0() {
        return 0f;
    }

    @Override
    protected float getU1() {
        return 1f;
    }

    @Override
    protected float getV0() {
        return 0f;
    }

    @Override
    protected float getV1() {
        return 1f;
    }
}
