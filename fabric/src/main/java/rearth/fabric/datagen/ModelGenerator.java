package rearth.fabric.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.minecraft.core.Direction;
import net.minecraft.data.models.BlockModelGenerators;
import net.minecraft.data.models.ItemModelGenerators;
import net.minecraft.data.models.blockstates.Condition;
import net.minecraft.data.models.blockstates.MultiPartGenerator;
import net.minecraft.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.data.models.blockstates.PropertyDispatch;
import net.minecraft.data.models.blockstates.Variant;
import net.minecraft.data.models.blockstates.VariantProperties;
import net.minecraft.data.models.model.ModelLocationUtils;
import net.minecraft.data.models.model.ModelTemplates;
import net.minecraft.data.models.model.TextureMapping;
import net.minecraft.data.models.model.TextureSlot;
import net.minecraft.resources.ResourceLocation;
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
        registerIonThruster(BlockContent.ION_THRUSTER.get(), blockStateModelGenerator);
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
    
    public void registerIonThruster(Block block, BlockModelGenerators blockStateModelGenerator) {
        var model = ModelLocationUtils.getModelLocation(block);
        var armModel = ResourceLocation.fromNamespaceAndPath("drones", "block/ion_thruster_arm");

        blockStateModelGenerator.blockStateOutput.accept(
          MultiPartGenerator.multiPart(block)
            .with(Variant.variant().with(VariantProperties.MODEL, model))
            .with(Condition.condition().term(BlockStateProperties.NORTH, true),
              Variant.variant().with(VariantProperties.MODEL, armModel).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R0))
            .with(Condition.condition().term(BlockStateProperties.SOUTH, true),
              Variant.variant().with(VariantProperties.MODEL, armModel).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180))
            .with(Condition.condition().term(BlockStateProperties.EAST, true),
              Variant.variant().with(VariantProperties.MODEL, armModel).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90))
            .with(Condition.condition().term(BlockStateProperties.WEST, true),
              Variant.variant().with(VariantProperties.MODEL, armModel).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270))
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
        var location = ModelLocationUtils.getModelLocation(drone);

        // items equipped in the head slot are rendered above the player's head by default
        // (via CustomHeadLayer); a zero scale collapses that render to nothing, hiding it
        ModelTemplates.FLAT_ITEM.create(location, TextureMapping.layer0(drone), modelOutput, (modelLocation, textures) -> {
            var json = ModelTemplates.FLAT_ITEM.createBaseTemplate(modelLocation, textures);

            var scale = new JsonArray();
            scale.add(0);
            scale.add(0);
            scale.add(0);

            var head = new JsonObject();
            head.add("scale", scale);

            var display = new JsonObject();
            display.add("head", head);
            json.add("display", display);

            return json;
        });
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
