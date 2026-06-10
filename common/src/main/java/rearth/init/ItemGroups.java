package rearth.init;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import rearth.Drones;

public class ItemGroups {
    
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Drones.MOD_ID, Registries.CREATIVE_MODE_TAB);
    
    public static final RegistrySupplier<CreativeModeTab> DRONES_TAB = TABS.register(
      Drones.id("main_group"),
      () -> CreativeTabRegistry.create(Component.translatable("category.drones.main_group"),
        () -> new ItemStack(ItemContent.POCKET_DRONE.get()))
    );
    
}
