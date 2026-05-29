package rearth.drone.behaviour;

import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import rearth.drone.DroneServerData;
import rearth.drone.RecordedBlock;
import rearth.init.TagContent;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

public class BeamAttackBehaviour extends ArrowAttackBehaviour {
    
    private static final int ATTACK_DAMAGE = 10;
    
    public BeamAttackBehaviour(LivingEntity target, PlayerEntity owner, DroneServerData drone) {
        super(target, owner, drone);
    }
    
    @Override
    public void performAttack(double dist, Vec3d shotFrom) {
        var world = this.owner.getWorld();
        
        this.target.damage(new DamageSource(world.getRegistryManager().get(RegistryKeys.DAMAGE_TYPE).entryOf(DamageTypes.PLAYER_ATTACK)), ATTACK_DAMAGE);
        
        world.playSound(null, owner.getBlockPos(), SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.PLAYERS, 0.5f, 1.7f);
        
        if (world instanceof ServerWorld serverWorld) {
            spawnBeamLine(this.drone.currentPosition, target.getEyePos(), serverWorld);
        }
    }
    
    private void spawnBeamLine(Vec3d from, Vec3d to, ServerWorld world) {
        var count = (int) (from.distanceTo(to) * 0.6f + 1);
        count = Math.min(count, 12);
        
        var increment = to.subtract(from).multiply(1f / count);
        var particle = ParticleTypes.SONIC_BOOM;
        
        world.spawnParticles(ParticleTypes.EXPLOSION, to.x, to.y, to.z, 1, 0, 0, 0, 0);
        
        int finalCount = count;
        CompletableFuture.runAsync(() -> {
            for (int i = 0; i < finalCount; i++) {
                var pos =  from.add(increment.multiply(i));
                world.spawnParticles(particle, pos.getX(), pos.getY(), pos.getZ(), 1, 0, 0, 0, 0);
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
            return super.getPriority() + 2;
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
        public void onTargetFound(DroneServerData drone, PlayerEntity player, LivingEntity target) {
            drone.setCurrentTask(new BeamAttackBehaviour(target, player, drone));
        }
    }
    
    public static boolean isValid(RecordedBlock block, HashMap<Vec3i, BlockState> frame) {
        // is valid when facing forward (south?) and not blocked
        
        var blockMatches = block.state().isIn(TagContent.BEAM_SOURCE);
        if (!blockMatches) return false;
        
        // ensure front is free
        for (int i = 1; i < 8; i++) {
            if (frame.containsKey(block.localPos().south(i))) return false;
        }
        
        return true;
        
    }
}
