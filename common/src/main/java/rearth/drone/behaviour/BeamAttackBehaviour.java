package rearth.drone.behaviour;

import rearth.drone.DroneServerData;
import rearth.drone.RecordedBlock;
import rearth.init.TagContent;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class BeamAttackBehaviour extends ArrowAttackBehaviour {
    
    private static final int ATTACK_DAMAGE = 10;
    public static final int BEAM_PRIORITY = 37;

    public BeamAttackBehaviour(LivingEntity target, Player owner, DroneServerData drone) {
        super(target, owner, drone, BEAM_PRIORITY);
    }
    
    @Override
    public boolean performAttack(double dist, Vec3 shotFrom) {
        var world = this.owner.level();
        
        this.target.hurt(new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.PLAYER_ATTACK)), ATTACK_DAMAGE);
        
        world.playSound(null, owner.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.5f, 1.7f);
        
        if (world instanceof ServerLevel serverWorld) {
            spawnBeamLine(this.drone.currentPosition, target.getEyePosition(), serverWorld);
        }
        
        return true;
    }
    
    private void spawnBeamLine(Vec3 from, Vec3 to, ServerLevel world) {
        var count = (int) (from.distanceTo(to) * 0.6f + 1);
        count = Math.min(count, 12);
        
        var increment = to.subtract(from).scale(1f / count);
        var particle = ParticleTypes.SONIC_BOOM;
        
        world.sendParticles(ParticleTypes.EXPLOSION, to.x, to.y, to.z, 1, 0, 0, 0, 0);
        
        int finalCount = count;
        CompletableFuture.runAsync(() -> {
            for (int i = 0; i < finalCount; i++) {
                var pos =  from.add(increment.scale(i));
                world.sendParticles(particle, pos.x(), pos.y(), pos.z(), 1, 0, 0, 0, 0);
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
    }
    
    @Override
    public int getAttackCooldown() {
        return 30;
    }

    public static class BeamAttackSensor extends ArrowAttackSensor {
        
        @Override
        public int getPriority() {
            return BEAM_PRIORITY;
        }

        @Override
        public int getTargetingRange() {
            return super.getTargetingRange() + 8;
        }

        @Override
        public boolean shootsProjectile() {
            return false;
        }

        @Override
        public void onTargetFound(DroneServerData drone, Player player, LivingEntity target, int priority) {
            drone.setCurrentTask(new BeamAttackBehaviour(target, player, drone));
        }
    }
    
    public static boolean isValid(RecordedBlock block, HashMap<Vec3i, BlockState> frame) {
        // is valid when facing forward (south?) and not blocked
        
        var blockMatches = block.state().is(TagContent.BEAM_SOURCE);
        if (!blockMatches) return false;
        
        // ensure front is free
        for (int i = 1; i < 8; i++) {
            if (frame.containsKey(block.localPos().south(i))) return false;
        }
        
        return true;
        
    }
}
