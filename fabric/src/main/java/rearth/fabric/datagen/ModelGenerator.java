package rearth.fabric.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.minecraft.data.models.BlockModelGenerators;
import net.minecraft.data.models.ItemModelGenerators;
import net.minecraft.data.models.model.ModelTemplates;
import net.minecraft.data.models.model.TextureMapping;
import net.minecraft.data.models.model.TextureSlot;
import net.minecraft.world.level.block.Block;
import rearth.init.BlockContent;
import rearth.init.ItemContent;

public class ModelGenerator extends FabricModelProvider {
    
    public ModelGenerator(FabricDataOutput output) {
        super(output);
    }
    
    @Override
    public void generateBlockStateModels(BlockModelGenerators blockStateModelGenerator) {
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.WOOD_ROTOR.get());
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.IRON_ROTOR.get());
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.ION_THRUSTER.get());
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.DRILL.get());
        blockStateModelGenerator.createGenericCube(BlockContent.ASSEMBLER_CONTROLLER.get());
        registerFrame(BlockContent.ASSEMBLER_FRAME.get(), blockStateModelGenerator);
    }
    
    @Override
    public void generateItemModels(ItemModelGenerators itemModelGenerator) {
        itemModelGenerator.generateFlatItem(ItemContent.POCKET_DRONE.get(), ModelTemplates.FLAT_ITEM);
    }
    
    public void registerFrame(Block block, BlockModelGenerators blockStateModelGenerator) {
        var textureMap = (new TextureMapping())
                           .put(TextureSlot.PARTICLE, TextureMapping.getBlockTexture(block, "_side"))
                           .put(TextureSlot.DOWN, TextureMapping.getBlockTexture(block, "_down"))
                           .put(TextureSlot.UP, TextureMapping.getBlockTexture(block, "_up"))
                           .put(TextureSlot.NORTH, TextureMapping.getBlockTexture(block, "_side"))
                           .put(TextureSlot.SOUTH, TextureMapping.getBlockTexture(block, "_side"))
                           .put(TextureSlot.EAST, TextureMapping.getBlockTexture(block, "_side"))
                           .put(TextureSlot.WEST, TextureMapping.getBlockTexture(block, "_side"));
        
        blockStateModelGenerator.blockStateOutput.accept(
          BlockModelGenerators.createSimpleBlock(block, ModelTemplates.CUBE.create(block, textureMap,blockStateModelGenerator.modelOutput)));
    }
}
