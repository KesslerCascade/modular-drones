package rearth.init;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import rearth.Drones;
import rearth.blocks.controller.ControllerBlockEntity;

public class BlockEntitiesContent {
    
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Drones.MOD_ID, Registries.BLOCK_ENTITY_TYPE);
    
    public static final RegistrySupplier<BlockEntityType<ControllerBlockEntity>> ASSEMBLER_CONTROLLER = BLOCK_ENTITIES.register("controller", () -> new BlockEntityType<ControllerBlockEntity>(ControllerBlockEntity::new, java.util.Set.of(BlockContent.ASSEMBLER_CONTROLLER.get())));
    
}
