package rearth.init;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import rearth.Drones;
import rearth.blocks.controller.ControllerBlock;
import rearth.blocks.controller.ControllerBlockItem;
import rearth.blocks.rotors.DrillBlock;
import rearth.blocks.rotors.IonThruster;
import rearth.blocks.rotors.IronRotor;
import rearth.blocks.rotors.WoodenRotor;

// todo crafting recipes, random loot spawns?

public class BlockContent {
    
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Drones.MOD_ID, Registries.BLOCK);
    
    public static final RegistrySupplier<Block> ASSEMBLER_FRAME = BLOCKS.register("frame", () -> new Block(properties("frame", BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK))));
    public static final RegistrySupplier<Block> ASSEMBLER_CONTROLLER = BLOCKS.register("controller", () -> new ControllerBlock(properties("controller", BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK))));

    public static final RegistrySupplier<Block> WOOD_ROTOR = BLOCKS.register("wood_rotor", () -> new WoodenRotor(properties("wood_rotor", BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).noOcclusion())));
    public static final RegistrySupplier<Block> IRON_ROTOR = BLOCKS.register("iron_rotor", () -> new IronRotor(properties("iron_rotor", BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion())));
    public static final RegistrySupplier<Block> ION_THRUSTER = BLOCKS.register("ion_thruster", () -> new IonThruster(properties("ion_thruster", BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion())));
    public static final RegistrySupplier<Block> DRILL = BLOCKS.register("drill", () -> new DrillBlock(properties("drill", BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion())));

    private static BlockBehaviour.Properties properties(String name, BlockBehaviour.Properties properties) {
        return properties.setId(ResourceKey.create(Registries.BLOCK, Drones.id(name)));
    }
    
    public static void registerItems() {
        
        registerItem(ASSEMBLER_FRAME, "frame");
        ItemContent.ITEMS.register(Drones.id("controller"), () -> new ControllerBlockItem(ASSEMBLER_CONTROLLER.get(), ItemContent.properties("controller").arch$tab(ItemGroups.DRONES_TAB)));
        registerItem(WOOD_ROTOR, "wood_rotor");
        registerItem(IRON_ROTOR, "iron_rotor");
        registerItem(ION_THRUSTER, "ion_thruster");
        registerItem(DRILL, "drill");
    
    }
    
    private static void registerItem(RegistrySupplier<Block> block, String name) {
        ItemContent.ITEMS.register(Drones.id(name), () -> new BlockItem(block.get(), ItemContent.properties(name).arch$tab(ItemGroups.DRONES_TAB)));
    }
    
}
