package rearth.client.particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.core.particles.SimpleParticleType;

public class IonTrailParticleProvider implements ParticleProvider<SimpleParticleType> {

    @Override
    public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
        return new IonTrailParticle(level, x, y, z, xSpeed, ySpeed, zSpeed);
    }
}
