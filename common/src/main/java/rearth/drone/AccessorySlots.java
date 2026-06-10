package rearth.drone;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * Looks up the PocketDrone in the loader-specific accessory mod's "drone" slot
 * (Trinkets on Fabric, Curios on NeoForge). Returns empty if the mod isn't loaded
 * or no matching item is equipped.
 */
public class AccessorySlots {

    @ExpectPlatform
    public static Optional<ItemStack> findDroneSlotItem(Player player) {
        throw new AssertionError();
    }
}
