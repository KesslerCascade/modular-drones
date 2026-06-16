package rearth.fabric.datagen;

import com.mojang.math.Quadrant;
import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.ConditionBuilder;
import net.minecraft.client.data.models.blockstates.MultiPartGenerator;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.renderer.block.model.Variant;
import net.minecraft.client.renderer.block.model.VariantMutator;
import net.minecraft.client.renderer.item.EmptyModel;
import net.minecraft.client.renderer.item.properties.select.DisplayContext;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.item.ItemDisplayContext;
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
        registerRotor(BlockContent.WOOD_ROTOR.get(), blockStateModelGenerator);
        registerRotor(BlockContent.IRON_ROTOR.get(), blockStateModelGenerator);
        registerIonThruster(BlockContent.ION_THRUSTER.get(), blockStateModelGenerator);
        registerHorizontalFacing(BlockContent.DRILL.get(), blockStateModelGenerator);
        registerController(BlockContent.ASSEMBLER_CONTROLLER.get(), blockStateModelGenerator);
        registerFrame(BlockContent.ASSEMBLER_FRAME.get(), blockStateModelGenerator);
    }

    public void registerRotor(Block block, BlockModelGenerators blockStateModelGenerator) {
        var topModel = ModelLocationUtils.getModelLocation(block);
        var bottomModel = ModelLocationUtils.getModelLocation(block, "_bottom");

        blockStateModelGenerator.blockStateOutput.accept(
          MultiVariantGenerator.dispatch(block)
            .with(PropertyDispatch.initial(net.minecraft.world.level.block.state.properties.BlockStateProperties.HALF)
                    .select(net.minecraft.world.level.block.state.properties.Half.TOP, new MultiVariant(WeightedList.of(new Variant(topModel))))
                    .select(net.minecraft.world.level.block.state.properties.Half.BOTTOM, new MultiVariant(WeightedList.of(new Variant(bottomModel)))))
        );
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
        var multiVariant = new MultiVariant(WeightedList.of(new Variant(model)));

        blockStateModelGenerator.blockStateOutput.accept(
          MultiVariantGenerator.dispatch(block, multiVariant)
            .with(PropertyDispatch.modify(BlockStateProperties.HORIZONTAL_FACING)
                    .select(Direction.SOUTH, (VariantMutator) variant -> variant)
                    .select(Direction.WEST, VariantMutator.Y_ROT.withValue(Quadrant.R90))
                    .select(Direction.NORTH, VariantMutator.Y_ROT.withValue(Quadrant.R180))
                    .select(Direction.EAST, VariantMutator.Y_ROT.withValue(Quadrant.R270)))
        );
    }
    
    public void registerIonThruster(Block block, BlockModelGenerators blockStateModelGenerator) {
        var model = ModelLocationUtils.getModelLocation(block);
        var armModel = Identifier.fromNamespaceAndPath("drones", "block/ion_thruster_arm");

        var baseVariant = new MultiVariant(WeightedList.of(new Variant(model)));

        blockStateModelGenerator.blockStateOutput.accept(
          MultiPartGenerator.multiPart(block)
            .with(baseVariant)
            .with(new ConditionBuilder().term(BlockStateProperties.NORTH, true),
              new MultiVariant(WeightedList.of(new Variant(armModel).with(VariantMutator.Y_ROT.withValue(Quadrant.R0)))))
            .with(new ConditionBuilder().term(BlockStateProperties.SOUTH, true),
              new MultiVariant(WeightedList.of(new Variant(armModel).with(VariantMutator.Y_ROT.withValue(Quadrant.R180)))))
            .with(new ConditionBuilder().term(BlockStateProperties.EAST, true),
              new MultiVariant(WeightedList.of(new Variant(armModel).with(VariantMutator.Y_ROT.withValue(Quadrant.R90)))))
            .with(new ConditionBuilder().term(BlockStateProperties.WEST, true),
              new MultiVariant(WeightedList.of(new Variant(armModel).with(VariantMutator.Y_ROT.withValue(Quadrant.R270)))))
        );
    }

    public void registerHorizontalFacing(Block block, BlockModelGenerators blockStateModelGenerator) {
        var model = ModelLocationUtils.getModelLocation(block);
        var multiVariant = new MultiVariant(WeightedList.of(new Variant(model)));

        blockStateModelGenerator.blockStateOutput.accept(
          MultiVariantGenerator.dispatch(block, multiVariant)
            .with(PropertyDispatch.modify(BlockStateProperties.HORIZONTAL_FACING)
                    .select(Direction.SOUTH, (VariantMutator) variant -> variant)
                    .select(Direction.WEST, VariantMutator.Y_ROT.withValue(Quadrant.R90))
                    .select(Direction.NORTH, VariantMutator.Y_ROT.withValue(Quadrant.R180))
                    .select(Direction.EAST, VariantMutator.Y_ROT.withValue(Quadrant.R270)))
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
