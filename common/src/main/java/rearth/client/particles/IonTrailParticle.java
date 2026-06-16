package rearth.client.particles;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.util.LightCoordsUtil;

public class IonTrailParticle extends SingleQuadParticle {

    // Mirrors RenderPipelines.TRANSLUCENT_PARTICLE, but with additive blending so the
    // trail glows brightly instead of being dimmed by normal alpha blending.
    private static final RenderPipeline ADDITIVE_PARTICLE_PIPELINE = RenderPipeline.builder()
      .withLocation("pipeline/drones_additive_particle")
      .withVertexShader("core/particle")
      .withFragmentShader("core/particle")
      .withBindGroupLayout(BindGroupLayouts.GLOBALS)
      .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
      .withBindGroupLayout(BindGroupLayouts.FOG)
      .withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER2)
      .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
      .withVertexBinding(0, DefaultVertexFormat.PARTICLE)
      .withPrimitiveTopology(PrimitiveTopology.QUADS)
      .build();

    private static final SingleQuadParticle.Layer ADDITIVE_LAYER = new SingleQuadParticle.Layer(
      true, TextureAtlas.LOCATION_PARTICLES, ADDITIVE_PARTICLE_PIPELINE
    );

    protected IonTrailParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
        super(level, x, y, z, xd, yd, zd, sprites.first());
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
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        this.alpha = 0.8f * Math.max(0f, 1f - (float) this.age / (float) this.lifetime);
    }

    @Override
    protected int getLightCoords(float partialTick) {
        return LightCoordsUtil.FULL_BRIGHT;
    }

    @Override
    protected SingleQuadParticle.Layer getLayer() {
        return ADDITIVE_LAYER;
    }
}
