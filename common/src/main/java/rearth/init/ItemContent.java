package rearth.init;

import dev.architectury.platform.Platform;
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

    /**
     * If Curios/Trinkets is installed, the drone has a dedicated accessory slot,
     * so it shouldn't also be wearable in the vanilla head slot.
     */
    public static final boolean HAS_ACCESSORY_SLOT = Platform.isModLoaded("curios") || Platform.isModLoaded("trinkets");

    public static final RegistrySupplier<Item> POCKET_DRONE = ITEMS.register("pocket_drone", () -> {
        var properties = new Item.Properties()
                            .setId(ResourceKey.create(Registries.ITEM, Drones.id("pocket_drone")))
                            .stacksTo(1);

        if (!HAS_ACCESSORY_SLOT)
            properties.component(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.HEAD).build());

        return new PocketDrone(properties);
    });

    public static Item.Properties properties(String name) {
        return new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Drones.id(name)));
    }

}
