package rearth.init;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import rearth.Drones;
import rearth.blocks.controller.ControllerBlock;
import rearth.blocks.rotors.DrillBlock;
import rearth.blocks.rotors.IonThruster;
import rearth.blocks.rotors.IronRotor;
import rearth.blocks.rotors.WoodenRotor;

// todo crafting recipes, random loot spawns?

public class BlockContent {
    
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Drones.MOD_ID, Registries.BLOCK);
    
    public static final RegistrySupplier<Block> ASSEMBLER_FRAME = BLOCKS.register("frame", () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)));
    public static final RegistrySupplier<Block> ASSEMBLER_CONTROLLER = BLOCKS.register("controller", () -> new ControllerBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)));
    
    public static final RegistrySupplier<Block> WOOD_ROTOR = BLOCKS.register("wood_rotor", () -> new WoodenRotor(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).noOcclusion()));
    public static final RegistrySupplier<Block> IRON_ROTOR = BLOCKS.register("iron_rotor", () -> new IronRotor(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion()));
    public static final RegistrySupplier<Block> ION_THRUSTER = BLOCKS.register("ion_thruster", () -> new IonThruster(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion()));
    public static final RegistrySupplier<Block> DRILL = BLOCKS.register("drill", () -> new DrillBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion()));
    
    public static void registerItems() {
        
        registerItem(ASSEMBLER_FRAME, "frame");
        registerItem(ASSEMBLER_CONTROLLER, "controller");
        registerItem(WOOD_ROTOR, "wood_rotor");
        registerItem(IRON_ROTOR, "iron_rotor");
        registerItem(ION_THRUSTER, "ion_thruster");
        registerItem(DRILL, "drill");
    
    }
    
    private static void registerItem(RegistrySupplier<Block> block, String name) {
        ItemContent.ITEMS.register(Drones.id(name), () -> new BlockItem(block.get(), new Item.Properties().arch$tab(ItemGroups.DRONES_TAB)));
    }
    
}
