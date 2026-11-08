package net.claustra01.iaftfc.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

public final class DragonToolRecipeData implements DataProvider {
    private final PackOutput.PathProvider recipes;

    public DragonToolRecipeData(PackOutput output) {
        recipes = output.createPathProvider(PackOutput.Target.DATA_PACK, "recipe");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        List<CompletableFuture<?>> writes = new ArrayList<>();
        save(writes, output, "dragon_horn", shaped(List.of("  B", " BB", "IB "), keys(
            "B", item("iceandfire:dragonbone"), "I", tag("c:rods/wooden")), "iceandfire:dragon_horn"));
        save(writes, output, "dragon_stick", shaped(List.of("S", "T", "T"), keys(
            "S", tag("iceandfire:dragon_skulls"), "T", tag("c:rods/wooden")), "iceandfire:dragon_stick"));
        save(writes, output, "dragonseeker", shaped(List.of("a", "x", "x"), keys(
            "a", tag("iceandfire:dragon_skulls"), "x", item("tfc:metal/ingot/steel")),
            "iceandfire:dragon_seeker"));

        save(writes, output, "epic_dragon_seeker", shapeless(List.of(
            item("iceandfire:dragon_seeker"), tag("c:ingots/red_steel"), tag("c:ingots/blue_steel")),
            "iceandfire:epic_dragon_seeker"));
        JsonArray dragonsteel = new JsonArray();
        dragonsteel.add(item("iaftfc:metal/ingot/dragonsteel_fire"));
        dragonsteel.add(item("iaftfc:metal/ingot/dragonsteel_ice"));
        dragonsteel.add(item("iaftfc:metal/ingot/dragonsteel_lightning"));
        save(writes, output, "legendary_dragon_seeker", shaped(List.of("a", "x", "x"), keys(
            "a", tag("iceandfire:dragon_skulls"), "x", dragonsteel), "iceandfire:legendary_dragon_seeker"));

        addCrystal(writes, output, "fire", "ruby");
        addCrystal(writes, output, "ice", "sapphire");
        addCrystal(writes, output, "lightning", "topaz");
        return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
    }

    private void addCrystal(List<CompletableFuture<?>> writes, CachedOutput output, String element, String gem) {
        save(writes, output, "summoning_crystal_" + element, shapeless(List.of(
            item("iceandfire:" + element + "_dragon_blood"), item("tfc:gem/" + gem),
            tag("c:gems/diamond"), tag("c:gems/emerald")),
            "iceandfire:summoning_crystal_" + element));
    }

    private static JsonObject shaped(List<String> pattern, JsonObject key, String resultId) {
        JsonObject recipe = new JsonObject();
        recipe.addProperty("type", "minecraft:crafting_shaped");
        recipe.addProperty("category", "misc");
        JsonArray rows = new JsonArray();
        pattern.forEach(rows::add);
        recipe.add("pattern", rows);
        recipe.add("key", key);
        recipe.add("result", stack(resultId));
        recipe.addProperty("show_notification", true);
        return recipe;
    }

    private static JsonObject shapeless(List<JsonObject> ingredients, String resultId) {
        JsonObject recipe = new JsonObject();
        recipe.addProperty("type", "minecraft:crafting_shapeless");
        recipe.addProperty("category", "misc");
        JsonArray values = new JsonArray();
        ingredients.forEach(values::add);
        recipe.add("ingredients", values);
        recipe.add("result", stack(resultId));
        return recipe;
    }

    private static JsonObject keys(Object... values) {
        JsonObject keys = new JsonObject();
        for (int index = 0; index < values.length; index += 2) {
            keys.add((String) values[index], (JsonElement) values[index + 1]);
        }
        return keys;
    }

    private static JsonObject item(String id) {
        JsonObject ingredient = new JsonObject();
        ingredient.addProperty("item", id);
        return ingredient;
    }

    private static JsonObject tag(String id) {
        JsonObject ingredient = new JsonObject();
        ingredient.addProperty("tag", id);
        return ingredient;
    }

    private static JsonObject stack(String id) {
        JsonObject stack = new JsonObject();
        stack.addProperty("id", id);
        stack.addProperty("count", 1);
        return stack;
    }

    private void save(List<CompletableFuture<?>> writes, CachedOutput output, String id, JsonObject json) {
        writes.add(DataProvider.saveStable(output, json,
            recipes.json(ResourceLocation.fromNamespaceAndPath("iceandfire", id))));
    }

    @Override
    public String getName() {
        return "IaFTFC dragon tool recipes";
    }
}
