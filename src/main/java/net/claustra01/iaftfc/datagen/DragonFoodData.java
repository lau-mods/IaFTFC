package net.claustra01.iaftfc.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

public final class DragonFoodData implements DataProvider {
    private static final List<String> ELEMENTS = List.of("fire", "ice", "lightning");
    private final PackOutput.PathProvider food;
    private final PackOutput.PathProvider recipes;

    public DragonFoodData(PackOutput output) {
        food = output.createPathProvider(PackOutput.Target.DATA_PACK, "tfc/food");
        recipes = output.createPathProvider(PackOutput.Target.DATA_PACK, "recipe");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        List<CompletableFuture<?>> writes = new ArrayList<>();
        for (String element : ELEMENTS) {
            addFlesh(writes, output, element);
            addFriedRice(writes, output, element);
        }
        addDragonMealRecipe(writes, output);
        return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
    }

    private void addFlesh(List<CompletableFuture<?>> writes, CachedOutput output, String element) {
        JsonObject value = food("iceandfire:" + element + "_dragon_flesh", 8, 0, 2, 2);
        value.addProperty("protein", 3);
        save(writes, output, food, "iaftfc", element + "_dragon_flesh", value);
    }

    private void addFriedRice(List<CompletableFuture<?>> writes, CachedOutput output, String element) {
        String id = "cooked_rice_with_" + element + "_dragon_meat";
        JsonObject value = food("iceandfire:" + id, 4, 20, 1.5, 3.5);
        value.addProperty("grain", 1.7);
        value.addProperty("vegetables", 2);
        value.addProperty("protein", 3);
        save(writes, output, food, "iaftfc", id, value);
    }

    private void addDragonMealRecipe(List<CompletableFuture<?>> writes, CachedOutput output) {
        JsonObject recipe = new JsonObject();
        recipe.addProperty("type", "minecraft:crafting_shaped");
        recipe.addProperty("category", "misc");
        JsonArray pattern = new JsonArray();
        pattern.add("BMB");
        pattern.add("MBM");
        pattern.add("BMB");
        recipe.add("pattern", pattern);

        JsonObject key = new JsonObject();
        key.add("B", ingredient("item", "minecraft:bone"));
        JsonObject meat = new JsonObject();
        meat.addProperty("type", "tfc:and");
        JsonArray children = new JsonArray();
        JsonObject tfcMeat = new JsonObject();
        tfcMeat.addProperty("type", "neoforge:difference");
        tfcMeat.add("base", ingredient("tag", "c:foods/meat"));
        tfcMeat.add("subtracted", ingredient("tag", "c:foods/fish"));
        children.add(tfcMeat);
        JsonObject notRotten = new JsonObject();
        notRotten.addProperty("type", "tfc:not_rotten");
        children.add(notRotten);
        meat.add("children", children);
        key.add("M", meat);
        recipe.add("key", key);

        JsonObject result = new JsonObject();
        result.addProperty("id", "iceandfire:dragon_meal");
        result.addProperty("count", 1);
        recipe.add("result", result);
        recipe.addProperty("show_notification", true);
        save(writes, output, recipes, "iceandfire", "dragon_meal", recipe);
    }

    private static JsonObject food(String item, int hunger, double water, double saturation, double decay) {
        JsonObject value = new JsonObject();
        value.add("ingredient", ingredient("item", item));
        value.addProperty("hunger", hunger);
        if (water != 0) {
            value.addProperty("water", water);
        }
        value.addProperty("saturation", saturation);
        value.addProperty("decay_modifier", decay);
        return value;
    }

    private static JsonObject ingredient(String key, String value) {
        JsonObject ingredient = new JsonObject();
        ingredient.addProperty(key, value);
        return ingredient;
    }

    private static void save(List<CompletableFuture<?>> writes, CachedOutput output, PackOutput.PathProvider path,
        String namespace, String id, JsonObject json) {
        writes.add(DataProvider.saveStable(output, json,
            path.json(ResourceLocation.fromNamespaceAndPath(namespace, id))));
    }

    @Override
    public String getName() {
        return "IaFTFC dragon food";
    }
}
