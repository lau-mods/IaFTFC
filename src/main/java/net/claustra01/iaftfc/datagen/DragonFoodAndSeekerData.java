package net.claustra01.iaftfc.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.neoforged.fml.ModList;

public final class DragonFoodAndSeekerData implements DataProvider {
    private final PackOutput output;

    public DragonFoodAndSeekerData(PackOutput output) {
        this.output = output;
    }

    @Override
    public String getName() {
        return "IaFTFC dragon food and seeker";
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        try {
            List<CompletableFuture<?>> writes = new ArrayList<>();
            generate(cache, writes);
            return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
        } catch (IOException exception) {
            return CompletableFuture.failedFuture(exception);
        }
    }

    private void generate(CachedOutput cache, List<CompletableFuture<?>> writes) throws IOException {
        JsonArray meat = new JsonArray();
        JsonArray registered = read("tfc", "data/c/tags/item/foods/raw_meat.json").getAsJsonArray("values");
        Set<String> excluded = Set.of("turtle", "frog_legs", "calamari", "shellfish");
        for (var entry : registered) {
            String id = entry.getAsString();
            if (!id.startsWith("tfc:food/")) continue;
            String name = id.substring("tfc:food/".length());
            if (excluded.contains(name)) continue;
            if (!Files.exists(path("tfc", "data/tfc/tfc/food/" + name + ".json"))) {
                throw new IOException("Missing TFC raw meat FoodData: " + id);
            }
            meat.add(id);
        }
        JsonObject tag = new JsonObject();
        tag.add("values", meat);
        write(cache, writes, "data/iaftfc/tags/item/dragon_food_meat.json", tag);

        JsonObject meal = read("iceandfire", "data/iceandfire/recipe/dragon_meal.json");
        meal.getAsJsonObject("key").add("B", tagIngredient("c:bones"));
        JsonObject freshMeat = new JsonObject();
        freshMeat.addProperty("type", "tfc:and");
        JsonArray children = new JsonArray();
        children.add(tagIngredient("iaftfc:dragon_food_meat"));
        JsonObject notRotten = new JsonObject();
        notRotten.addProperty("type", "tfc:not_rotten");
        children.add(notRotten);
        freshMeat.add("children", children);
        meal.getAsJsonObject("key").add("M", freshMeat);
        write(cache, writes, "data/iceandfire/recipe/dragon_meal.json", meal);

        JsonArray foods = new JsonArray();
        for (String element : List.of("fire", "ice", "lightning")) {
            String flesh = "iceandfire:" + element + "_dragon_flesh";
            foods.add(flesh);
            JsonObject fleshData = food(flesh, 0, 2.0, 0, 0, 2.0);
            write(cache, writes, "data/iaftfc/tfc/food/" + element + "_dragon_flesh.json", fleshData);
            String stew = "iceandfire:" + (element.equals("ice") ? "frost" : element) + "_stew";
            foods.add(stew);
            JsonObject stewData = food(stew, 2.0, 2.5, 5.0, 1.0, 1.5);
            write(cache, writes, "data/iaftfc/tfc/food/" + element + "_stew.json", stewData);
        }
        JsonObject size = new JsonObject();
        JsonArray ingredients = new JsonArray();
        for (var food : foods) ingredients.add(itemIngredient(food.getAsString()));
        size.add("ingredient", ingredients);
        size.addProperty("size", "small");
        size.addProperty("weight", "light");
        write(cache, writes, "data/iaftfc/tfc/item_size/dragon_food.json", size);

        JsonObject normal = read("iceandfire", "data/iceandfire/recipe/dragonseeker.json");
        normal.getAsJsonObject("key").add("x", itemIngredient("tfc:metal/ingot/steel"));
        write(cache, writes, "data/iceandfire/recipe/dragonseeker.json", normal);
        JsonObject disabled = new JsonObject();
        JsonObject falseCondition = new JsonObject();
        falseCondition.addProperty("type", "neoforge:false");
        JsonArray conditions = new JsonArray();
        conditions.add(falseCondition);
        disabled.add("neoforge:conditions", conditions);
        write(cache, writes, "data/iceandfire/recipe/epic_dragon_seeker.json", disabled);
        for (String color : List.of("red", "blue")) {
            JsonObject epic = new JsonObject();
            epic.addProperty("type", "minecraft:smithing_transform");
            epic.add("base", itemIngredient("iceandfire:dragon_seeker"));
            epic.add("template", itemIngredient("tfc:metal/ingot/" + color + "_steel"));
            epic.add("addition", itemIngredient("tfc:metal/ingot/" + color + "_steel"));
            JsonObject result = new JsonObject();
            result.addProperty("id", "iceandfire:epic_dragon_seeker");
            epic.add("result", result);
            write(cache, writes, "data/iaftfc/recipe/epic_dragon_seeker_" + color + ".json", epic);
        }
        JsonObject legendary = read("iceandfire", "data/iceandfire/recipe/legendary_dragon_seeker.json");
        legendary.getAsJsonObject("key").add("x", tagIngredient("iaftfc:dragonsteel_ingots"));
        write(cache, writes, "data/iceandfire/recipe/legendary_dragon_seeker.json", legendary);
    }

    private static JsonObject food(String id, double saturation, double protein, double water,
        double vegetables, double decay) {
        JsonObject value = new JsonObject();
        value.add("ingredient", itemIngredient(id));
        value.addProperty("hunger", 4);
        value.addProperty("saturation", saturation);
        value.addProperty("protein", protein);
        value.addProperty("water", water);
        value.addProperty("vegetables", vegetables);
        value.addProperty("decay_modifier", decay);
        return value;
    }

    private static JsonObject itemIngredient(String id) {
        JsonObject value = new JsonObject();
        value.addProperty("item", id);
        return value;
    }

    private static JsonObject tagIngredient(String id) {
        JsonObject value = new JsonObject();
        value.addProperty("tag", id);
        return value;
    }

    private static Path path(String mod, String name) {
        return ModList.get().getModFileById(mod).getFile().findResource(name);
    }

    private static JsonObject read(String mod, String name) throws IOException {
        Path file = path(mod, name);
        if (!Files.exists(file)) throw new IOException("Missing " + mod + " data: " + name);
        return JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private void write(CachedOutput cache, List<CompletableFuture<?>> writes, String path, JsonObject json) {
        writes.add(DataProvider.saveStable(cache, json, output.getOutputFolder().resolve(path)));
    }
}
