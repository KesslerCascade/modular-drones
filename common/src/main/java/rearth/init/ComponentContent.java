package rearth.init;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import rearth.Drones;
import rearth.drone.DroneData;

public class ComponentContent {
    
    public static final DeferredRegister<DataComponentType<?>> COMPONENT_TYPES = DeferredRegister.create(Drones.MOD_ID, Registries.DATA_COMPONENT_TYPE);
    
    public static final RegistrySupplier<DataComponentType<DroneData>> DRONE_DATA_TYPE = COMPONENT_TYPES.register("drone_data", () ->
                                                                                                            DataComponentType.<DroneData>builder()
                                                                                                              .persistent(DroneData.CODEC)
                                                                                                              .cacheEncoding()
                                                                                                              .networkSynchronized(DroneData.PACKET_CODEC)
                                                                                                              .build()
    );
    
    public static final RegistrySupplier<DataComponentType<CarriedItemComponent>> CARRIED_ITEM_TYPE = COMPONENT_TYPES
            .register("carried_item", () -> DataComponentType.<CarriedItemComponent>builder()
                    .persistent(CarriedItemComponent.CODEC)
                    .networkSynchronized(CarriedItemComponent.PACKET_CODEC)
                    .build());

}
