package rearth.init;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.component.ComponentType;
import net.minecraft.registry.RegistryKeys;
import rearth.Drones;
import rearth.drone.DroneData;

public class ComponentContent {
    
    public static final DeferredRegister<ComponentType<?>> COMPONENT_TYPES = DeferredRegister.create(Drones.MOD_ID, RegistryKeys.DATA_COMPONENT_TYPE);
    
    public static final RegistrySupplier<ComponentType<DroneData>> DRONE_DATA_TYPE = COMPONENT_TYPES.register("drone_data", () ->
                                                                                                            ComponentType.<DroneData>builder()
                                                                                                              .codec(DroneData.CODEC)
                                                                                                              .cache()
                                                                                                              .packetCodec(DroneData.PACKET_CODEC)
                                                                                                              .build()
    );
    
    public static final RegistrySupplier<ComponentType<CarriedItemComponent>> CARRIED_ITEM_TYPE = COMPONENT_TYPES
            .register("carried_item", () -> ComponentType.<CarriedItemComponent>builder()
                    .codec(CarriedItemComponent.CODEC)
                    .packetCodec(CarriedItemComponent.PACKET_CODEC)
                    .build());

}
