package rearth.fabric.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.BlockTags;
import rearth.init.BlockContent;
import rearth.init.TagContent;

import java.util.concurrent.CompletableFuture;

public class BlockTagGenerator extends FabricTagProvider<Block> {
    
    public BlockTagGenerator(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, RegistryKeys.BLOCK, registriesFuture);
    }
    
    @Override
    protected void configure(RegistryWrapper.WrapperLookup wrapperLookup) {
        
        var pickaxeBuilder = getOrCreateTagBuilder(BlockTags.PICKAXE_MINEABLE);
        
        for (var supplier : BlockContent.BLOCKS) {
            pickaxeBuilder.add(supplier.get());
        }
        
        getOrCreateTagBuilder(TagContent.THRUSTER_BLOCKS)
          .addOptionalTag(TagContent.LOW_THRUSTER)
          .addOptionalTag(TagContent.MEDIUM_THRUSTER)
          .addOptionalTag(TagContent.HIGH_THRUSTER)
          .addOptionalTag(TagContent.ULTRA_THRUSTER);
        
        getOrCreateTagBuilder(TagContent.LOW_THRUSTER)
          .addOptionalTag(BlockTags.WOODEN_TRAPDOORS)
          .add(BlockContent.WOOD_ROTOR.get());
        
        getOrCreateTagBuilder(TagContent.MEDIUM_THRUSTER)
          .add(Blocks.IRON_TRAPDOOR)
          .add(BlockContent.IRON_ROTOR.get());
        
        getOrCreateTagBuilder(TagContent.HIGH_THRUSTER)
          .add(BlockContent.ION_THRUSTER.get());
        
        getOrCreateTagBuilder(TagContent.ARROW_LAUNCHER)
          .add(Blocks.DISPENSER);
        
        getOrCreateTagBuilder(TagContent.MELEE_DAMAGE)
          .add(Blocks.MAGMA_BLOCK)
          .add(Blocks.CACTUS);
        
        getOrCreateTagBuilder(TagContent.PICKUP_TOOLS)
          .add(Blocks.LODESTONE);
        
        getOrCreateTagBuilder(TagContent.BEAM_SOURCE)
          .add(Blocks.BEACON);
        
        getOrCreateTagBuilder(TagContent.MINING_TOOLS)
          .add(BlockContent.DRILL.get());
        
    }
}
