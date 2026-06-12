package rearth.fabric.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootSubProvider;
import net.minecraft.core.HolderLookup;
import rearth.init.BlockContent;
import dev.architectury.registry.registries.RegistrySupplier;
import java.util.concurrent.CompletableFuture;

public class BlockLootGenerator extends FabricBlockLootSubProvider {

    public BlockLootGenerator(FabricPackOutput dataOutput, CompletableFuture<HolderLookup.Provider> registryLookup) {
        super(dataOutput, registryLookup);
    }
    
    @Override
    public void generate() {
        for (var blockSupplier : BlockContent.BLOCKS) {
            dropSelf(blockSupplier.get());
        }
    }
}
