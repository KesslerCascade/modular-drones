package rearth.fabric.datagen;

import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.client.renderer.item.EmptyModel;
import net.minecraft.client.renderer.item.properties.select.DisplayContext;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.Block;
import rearth.init.BlockContent;
import rearth.init.ItemContent;

public class ModelGenerator extends FabricModelProvider {
    
    public ModelGenerator(FabricPackOutput output) {
        super(output);
    }

    @Override
    public void generateBlockStateModels(BlockModelGenerators blockStateModelGenerator) {
        registerNonTemplateModelBlock(BlockContent.WOOD_ROTOR.get(), blockStateModelGenerator);
        registerNonTemplateModelBlock(BlockContent.IRON_ROTOR.get(), blockStateModelGenerator);
        registerNonTemplateModelBlock(BlockContent.ION_THRUSTER.get(), blockStateModelGenerator);
        registerNonTemplateModelBlock(BlockContent.DRILL.get(), blockStateModelGenerator);
        registerCube(BlockContent.ASSEMBLER_CONTROLLER.get(), blockStateModelGenerator);
        registerFrame(BlockContent.ASSEMBLER_FRAME.get(), blockStateModelGenerator);
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
    
    public void registerNonTemplateModelBlock(Block block, BlockModelGenerators blockStateModelGenerator) {
        var modelLocation = ModelLocationUtils.getModelLocation(block);
        var multiVariant = new MultiVariant(WeightedList.of(new Variant(modelLocation)));
        blockStateModelGenerator.blockStateOutput.accept(
          MultiVariantGenerator.dispatch(block, multiVariant));
    }

    public void registerCube(Block block, BlockModelGenerators blockStateModelGenerator) {
        var modelLocation = ModelTemplates.CUBE_ALL.create(block, TextureMapping.cube(block), blockStateModelGenerator.modelOutput);
        var multiVariant = new MultiVariant(WeightedList.of(new Variant(modelLocation)));
        blockStateModelGenerator.blockStateOutput.accept(
          MultiVariantGenerator.dispatch(block, multiVariant));
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
