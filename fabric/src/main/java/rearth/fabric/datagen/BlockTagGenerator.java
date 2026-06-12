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

        var pickaxeBuilder = builder(BlockTags.MINEABLE_WITH_PICKAXE);

        for (var supplier : BlockContent.BLOCKS) {
            pickaxeBuilder.add(supplier.get().builtInRegistryHolder().key());
        }

        builder(TagContent.THRUSTER_BLOCKS)
          .addOptionalTag(TagContent.LOW_THRUSTER)
          .addOptionalTag(TagContent.MEDIUM_THRUSTER)
          .addOptionalTag(TagContent.HIGH_THRUSTER)
          .addOptionalTag(TagContent.ULTRA_THRUSTER);

        builder(TagContent.LOW_THRUSTER)
          .addOptionalTag(BlockTags.WOODEN_TRAPDOORS)
          .add(BlockContent.WOOD_ROTOR.get().builtInRegistryHolder().key());

        builder(TagContent.MEDIUM_THRUSTER)
          .add(Blocks.IRON_TRAPDOOR.builtInRegistryHolder().key())
          .add(BlockContent.IRON_ROTOR.get().builtInRegistryHolder().key());

        builder(TagContent.HIGH_THRUSTER)
          .add(BlockContent.ION_THRUSTER.get().builtInRegistryHolder().key());

        builder(TagContent.ARROW_LAUNCHER)
          .add(Blocks.DISPENSER.builtInRegistryHolder().key());

        builder(TagContent.MELEE_DAMAGE)
          .add(Blocks.MAGMA_BLOCK.builtInRegistryHolder().key())
          .add(Blocks.CACTUS.builtInRegistryHolder().key());

        builder(TagContent.PICKUP_TOOLS)
          .add(Blocks.LODESTONE.builtInRegistryHolder().key());

        builder(TagContent.BEAM_SOURCE)
          .add(Blocks.BEACON.builtInRegistryHolder().key());

        builder(TagContent.MINING_TOOLS)
          .add(BlockContent.DRILL.get().builtInRegistryHolder().key());

    }
}
