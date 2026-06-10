package rearth.fabric.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;
import net.minecraft.core.HolderLookup;
import rearth.init.BlockContent;
import dev.architectury.registry.registries.RegistrySupplier;
import java.util.concurrent.CompletableFuture;

public class BlockLootGenerator extends FabricBlockLootTableProvider {
    
    public BlockLootGenerator(FabricDataOutput dataOutput, CompletableFuture<HolderLookup.Provider> registryLookup) {
        super(dataOutput, registryLookup);
    }
    
    @Override
    public void generate() {
        for (var blockSupplier : BlockContent.BLOCKS) {
            dropSelf(blockSupplier.get());
        }
    }
}
