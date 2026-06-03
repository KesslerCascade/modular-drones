package rearth.drone;

import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rearth.drone.behaviour.DroneBehaviour;
import rearth.drone.behaviour.PlayerSwarmBehaviour;

import java.util.UUID;

public class DroneServerData {
    
    // synced to client
    public @NotNull Vec3d currentPosition;
    public @NotNull Vec3d currentRotation;  // y is vertical, z is forward, x is right
    
    // not synced
    public @NotNull Vec3d targetPosition = Vec3d.ZERO;
    public @NotNull Vec3d currentVelocity = Vec3d.ZERO;
    public @NotNull Vec3d recoilVelocity = Vec3d.ZERO;
    private @Nullable DroneBehaviour currentTask = null;
    public int ghostTicks = 0;
    public int ghostWaitTime = 0;
    public int actionCooldown = 0;
    public int currentTaskAge = 0;
    
    // pickup inventory
    public @NotNull ItemStack carriedItem = ItemStack.EMPTY;
    public boolean carriedItemDirty = false;
    public @Nullable UUID excludedItem = null;
    public int excludedItemTimeout = 0;

    public final @NotNull DroneData droneData;
    
    public DroneServerData(DroneData droneData, ServerPlayerEntity player) {
        this.droneData = droneData;
        this.currentPosition = player.getEyePos();
        this.currentRotation = Vec3d.ZERO;
        this.currentTask = new PlayerSwarmBehaviour(this, player);
    }
    
    public @Nullable DroneBehaviour getCurrentTask() {
        return currentTask;
    }
    
    public void setCurrentTask(@Nullable DroneBehaviour currentTask) {
        if (this.currentTask != null) {
            this.currentTask.onStopped();
        }
        this.currentTask = currentTask;
        this.currentTaskAge = 0;
    }
    
}
