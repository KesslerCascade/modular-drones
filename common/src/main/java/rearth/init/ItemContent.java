package rearth.init;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.Equippable;
import rearth.Drones;
import rearth.items.PocketDrone;

public class ItemContent {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Drones.MOD_ID, Registries.ITEM);

    public static final RegistrySupplier<Item> POCKET_DRONE = ITEMS.register("pocket_drone", () ->
                                                                                               new PocketDrone(new Item.Properties()
                                                                                                                 .setId(ResourceKey.create(Registries.ITEM, Drones.id("pocket_drone")))
                                                                                                                 .stacksTo(1)
                                                                                                                 .component(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.HEAD).build())
                                                                                               ));

    public static Item.Properties properties(String name) {
        return new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Drones.id(name)));
    }

}
