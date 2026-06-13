package rearth.fabric.datagen;

import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.renderer.block.model.Variant;
import net.minecraft.client.renderer.item.EmptyModel;
import net.minecraft.client.renderer.item.properties.select.DisplayContext;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import rearth.init.BlockContent;
import rearth.init.ItemContent;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class ModelGenerator extends FabricModelProvider {

    private BiConsumer<ResourceLocation, Supplier<JsonElement>> modelOutput;

    public ModelGenerator(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void generateBlockStateModels(BlockModelGenerators blockStateModelGenerator) {
        modelOutput = blockStateModelGenerator.modelOutput;
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
        var drone = ItemContent.POCKET_DRONE.get();

        var modelLocation = ModelTemplates.FLAT_ITEM.create(drone, TextureMapping.layer0(drone), itemModelGenerator.modelOutput);
        var droneModel = ItemModelUtils.plainModel(modelLocation);

        // Items placed in the head slot render their item model on the player's head unless the model
        // resolves to nothing for that display context, so hide the drone there.
        itemModelGenerator.itemModelOutput.accept(drone, ItemModelUtils.select(new DisplayContext(), droneModel,
          ItemModelUtils.when(ItemDisplayContext.HEAD, new EmptyModel.Unbaked())));
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
        
        var modelLocation = ModelTemplates.CUBE.create(block, textureMap, blockStateModelGenerator.modelOutput);
        var multiVariant = new MultiVariant(WeightedList.of(new Variant(modelLocation)));
        blockStateModelGenerator.blockStateOutput.accept(
          MultiVariantGenerator.dispatch(block, multiVariant));
    }
}
