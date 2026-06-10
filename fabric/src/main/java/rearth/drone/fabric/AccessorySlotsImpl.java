package rearth.drone.fabric;

import dev.architectury.platform.Platform;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import rearth.init.ComponentContent;
import rearth.init.ItemContent;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class AccessorySlotsImpl {

    public static Optional<ItemStack> findDroneSlotItem(Player player) {
        if (!Platform.isModLoaded("trinkets"))
            return Optional.empty();

        var component = TrinketsApi.getTrinketComponent(player);
        if (component.isEmpty())
            return Optional.empty();

        var found = new AtomicReference<ItemStack>();
        component.get().forEach((slotReference, stack) -> {
            if (found.get() == null
                    && "drone".equals(slotReference.inventory().getSlotType().getName())
                    && stack.is(ItemContent.POCKET_DRONE.get())
                    && stack.has(ComponentContent.DRONE_DATA_TYPE.get())) {
                found.set(stack);
            }
        });

        return Optional.ofNullable(found.get());
    }
}
