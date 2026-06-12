package rearth.fabric.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import rearth.Drones;
import rearth.init.BlockContent;
import rearth.init.ItemContent;
import rearth.init.TagContent;

import java.util.concurrent.CompletableFuture;

public class RecipeGenerator extends FabricRecipeProvider {

    public RecipeGenerator(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    public String getName() {
        return "Recipes";
    }

    @Override
    protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput exporter) {
        return new RecipeProvider(registries, exporter) {

            private final HolderGetter<Item> items = registries.lookupOrThrow(Registries.ITEM);

            @Override
            public void buildRecipes() {

                // controller
                offerFrameRecipe(exporter, BlockContent.ASSEMBLER_CONTROLLER.get().asItem(), Ingredient.of(Items.REPEATER), Ingredient.of(Items.REDSTONE), Ingredient.of(items.getOrThrow(ItemTags.LOGS)), Ingredient.of(Items.IRON_INGOT), Ingredient.of(Items.SMOOTH_STONE), 1, "_controller");

                // frame
                offerFrameRecipe(exporter, BlockContent.ASSEMBLER_FRAME.get().asItem(), Ingredient.of(Items.SMOOTH_STONE), Ingredient.of(Items.SMOOTH_STONE), Ingredient.of(items.getOrThrow(ItemTags.LOGS)), Ingredient.of(Items.IRON_INGOT), Ingredient.of(Items.SMOOTH_STONE), 6, "_frame");

                // basic rotor
                offerRotorRecipe(exporter, BlockContent.WOOD_ROTOR.get().asItem(), Ingredient.of(items.getOrThrow(ItemTags.PLANKS)), Ingredient.of(Items.STICK), Ingredient.of(Items.COPPER_INGOT), 1, "_woodrotor");
                // iron
                offerRotorRecipe(exporter, BlockContent.IRON_ROTOR.get().asItem(), Ingredient.of(Items.IRON_INGOT), Ingredient.of(Items.STICK), Ingredient.of(Items.COPPER_INGOT), 1, "_ironrotor");
                // ion thruster
                offerRotorRecipe(exporter, BlockContent.ION_THRUSTER.get().asItem(), Ingredient.of(Items.IRON_INGOT), Ingredient.of(Items.STICK), Ingredient.of(Items.DIAMOND), 1, "_ionrotor");

                // drill
                offerDrillRecipe(exporter, BlockContent.DRILL.get().asItem(), Ingredient.of(Items.IRON_INGOT), 1, "_drill");
            }

            public void offerFrameRecipe(RecipeOutput exporter,
                                         Item output,
                                         Ingredient bottom,
                                         Ingredient botSides,
                                         Ingredient middleSides,
                                         Ingredient core,
                                         Ingredient top,
                                         int count,
                                         String suffix) {
                var builder = ShapedRecipeBuilder.shaped(items, RecipeCategory.MISC, output, count)
                                .define('s', botSides)
                                .define('c', core)
                                .define('f', top)
                                .define('b', bottom)
                                .define('m', middleSides)
                                .pattern("fff")
                                .pattern("mcm")
                                .pattern("sbs");
                builder.unlockedBy(getHasName(output), has(output)).save(exporter, ResourceKey.create(Registries.RECIPE, Drones.id("crafting/" + suffix)));
            }

            public void offerRotorRecipe(RecipeOutput exporter,
                                         Item output,
                                         Ingredient outer,
                                         Ingredient inner,
                                         Ingredient core,
                                         int count,
                                         String suffix) {
                var builder = ShapedRecipeBuilder.shaped(items, RecipeCategory.MISC, output, count)
                                .define('o', outer)
                                .define('i', inner)
                                .define('c', core)
                                .pattern("oio")
                                .pattern("ici")
                                .pattern("ooo");
                builder.unlockedBy(getHasName(output), has(output)).save(exporter, ResourceKey.create(Registries.RECIPE, Drones.id("crafting/" + suffix)));
            }

            public void offerDrillRecipe(RecipeOutput exporter,
                                         Item output,
                                         Ingredient main,
                                         int count,
                                         String suffix) {
                var builder = ShapedRecipeBuilder.shaped(items, RecipeCategory.MISC, output, count)
                                .define('m', main)
                                .pattern("m  ")
                                .pattern("mmm")
                                .pattern("m  ");
                builder.unlockedBy(getHasName(output), has(output)).save(exporter, ResourceKey.create(Registries.RECIPE, Drones.id("crafting/" + suffix)));
            }
        };
    }
}
