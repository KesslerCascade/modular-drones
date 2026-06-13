package rearth.fabric.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.minecraft.core.Direction;
import net.minecraft.data.models.BlockModelGenerators;
import net.minecraft.data.models.ItemModelGenerators;
import net.minecraft.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.data.models.blockstates.PropertyDispatch;
import net.minecraft.data.models.blockstates.Variant;
import net.minecraft.data.models.blockstates.VariantProperties;
import net.minecraft.data.models.model.ModelLocationUtils;
import net.minecraft.data.models.model.ModelTemplates;
import net.minecraft.data.models.model.TextureMapping;
import net.minecraft.data.models.model.TextureSlot;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
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
        registerHorizontalFacing(BlockContent.DRILL.get(), blockStateModelGenerator);
        registerController(BlockContent.ASSEMBLER_CONTROLLER.get(), blockStateModelGenerator);
        registerFrame(BlockContent.ASSEMBLER_FRAME.get(), blockStateModelGenerator);
    }

    public void registerController(Block block, BlockModelGenerators blockStateModelGenerator) {
        var textureMap = (new TextureMapping())
                           .put(TextureSlot.PARTICLE, TextureMapping.getBlockTexture(block, "_particle"))
                           .put(TextureSlot.DOWN, TextureMapping.getBlockTexture(block, "_down"))
                           .put(TextureSlot.UP, TextureMapping.getBlockTexture(block, "_up"))
                           .put(TextureSlot.NORTH, TextureMapping.getBlockTexture(block, "_north"))
                           .put(TextureSlot.SOUTH, TextureMapping.getBlockTexture(block, "_south"))
                           .put(TextureSlot.EAST, TextureMapping.getBlockTexture(block, "_east"))
                           .put(TextureSlot.WEST, TextureMapping.getBlockTexture(block, "_west"));

        var model = ModelTemplates.CUBE.create(block, textureMap, blockStateModelGenerator.modelOutput);

        blockStateModelGenerator.blockStateOutput.accept(
          MultiVariantGenerator.multiVariant(block, Variant.variant().with(VariantProperties.MODEL, model))
            .with(PropertyDispatch.property(BlockStateProperties.HORIZONTAL_FACING)
                    .select(Direction.SOUTH, Variant.variant())
                    .select(Direction.WEST, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90))
                    .select(Direction.NORTH, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180))
                    .select(Direction.EAST, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)))
        );
    }
    
    public void registerHorizontalFacing(Block block, BlockModelGenerators blockStateModelGenerator) {
        var model = ModelLocationUtils.getModelLocation(block);

        blockStateModelGenerator.blockStateOutput.accept(
          MultiVariantGenerator.multiVariant(block, Variant.variant().with(VariantProperties.MODEL, model))
            .with(PropertyDispatch.property(BlockStateProperties.HORIZONTAL_FACING)
                    .select(Direction.SOUTH, Variant.variant())
                    .select(Direction.WEST, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90))
                    .select(Direction.NORTH, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180))
                    .select(Direction.EAST, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)))
        );
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
