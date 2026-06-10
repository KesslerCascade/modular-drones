package rearth.drone.neoforge;

import dev.architectury.platform.Platform;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import rearth.init.ComponentContent;
import rearth.init.ItemContent;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.Optional;

public class AccessorySlotsImpl {

    public static Optional<ItemStack> findDroneSlotItem(Player player) {
        if (!Platform.isModLoaded("curios"))
            return Optional.empty();

        var inventory = CuriosApi.getCuriosInventory(player);
        if (inventory.isEmpty())
            return Optional.empty();

        var stacksHandler = inventory.get().getStacksHandler("drone");
        if (stacksHandler.isEmpty())
            return Optional.empty();

        var stacks = stacksHandler.get().getStacks();
        for (var i = 0; i < stacks.getSlots(); i++) {
            var stack = stacks.getStackInSlot(i);
            if (stack.is(ItemContent.POCKET_DRONE.get())
                    && stack.has(ComponentContent.DRONE_DATA_TYPE.get())) {
                return Optional.of(stack);
            }
        }

        return Optional.empty();
    }
}
