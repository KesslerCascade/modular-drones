package rearth.fabric.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import rearth.init.BlockContent;
import rearth.init.TagContent;
import dev.architectury.registry.registries.RegistrySupplier;
import java.util.concurrent.CompletableFuture;

public class BlockTagGenerator extends FabricTagProvider<Block> {
    
    public BlockTagGenerator(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, Registries.BLOCK, registriesFuture);
    }
    
    @Override
    protected void addTags(HolderLookup.Provider wrapperLookup) {
        
        var pickaxeBuilder = getOrCreateTagBuilder(BlockTags.MINEABLE_WITH_PICKAXE);
        
        for (var supplier : BlockContent.BLOCKS) {
            pickaxeBuilder.add(supplier.get());
        }
        
        getOrCreateTagBuilder(TagContent.THRUSTER_BLOCKS)
          .addOptionalTag(TagContent.LOW_THRUSTER)
          .addOptionalTag(TagContent.MEDIUM_THRUSTER)
          .addOptionalTag(TagContent.HIGH_THRUSTER)
          .addOptionalTag(TagContent.ULTRA_THRUSTER);
        
        getOrCreateTagBuilder(TagContent.LOW_THRUSTER)
          .add(BlockContent.WOOD_ROTOR.get());
        
        getOrCreateTagBuilder(TagContent.MEDIUM_THRUSTER)
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
