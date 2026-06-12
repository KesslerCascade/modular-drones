package rearth.drone.fabric;

import dev.architectury.platform.Platform;
import eu.pb4.trinkets.api.TrinketsApi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import rearth.init.ComponentContent;
import rearth.init.ItemContent;

import java.util.Optional;

public class AccessorySlotsImpl {

    public static Optional<ItemStack> findDroneSlotItem(Player player) {
        if (!Platform.isModLoaded("trinkets"))
            return Optional.empty();

        var attachment = TrinketsApi.getAttachment(player);

        return attachment.getAllEquipped().stream()
          .filter(entry -> "drone".equals(entry.getA().slotType().name())
            && entry.getB().is(ItemContent.POCKET_DRONE.get())
            && entry.getB().has(ComponentContent.DRONE_DATA_TYPE.get()))
          .map(entry -> entry.getB())
          .findFirst();
    }
}
