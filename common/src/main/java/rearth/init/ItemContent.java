package rearth.init;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import rearth.Drones;
import rearth.items.PocketDrone;

public class ItemContent {
    
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Drones.MOD_ID, Registries.ITEM);
    
    public static final RegistrySupplier<Item> POCKET_DRONE = ITEMS.register("pocket_drone", () ->
                                                                                               new PocketDrone(new Item.Properties()
                                                                                                                 .stacksTo(1)
                                                                                               ));
    
}
