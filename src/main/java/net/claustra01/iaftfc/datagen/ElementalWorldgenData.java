package net.claustra01.iaftfc.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.claustra01.iaftfc.worldgen.ElementalPlacement;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

public final class ElementalWorldgenData implements DataProvider {
    private final PackOutput.PathProvider configuredFeatures;
    private final PackOutput.PathProvider placedFeatures;
    private final PackOutput.PathProvider biomeModifiers;
    private final PackOutput.PathProvider recipes;
    private final PackOutput.PathProvider lootTables;
    private final PackOutput.PathProvider blockTags;
    private final PackOutput.PathProvider itemTags;

    public ElementalWorldgenData(PackOutput output) {
        configuredFeatures = output.createPathProvider(PackOutput.Target.DATA_PACK, "worldgen/configured_feature");
        placedFeatures = output.createPathProvider(PackOutput.Target.DATA_PACK, "worldgen/placed_feature");
        biomeModifiers = output.createPathProvider(PackOutput.Target.DATA_PACK, "neoforge/biome_modifier");
        recipes = output.createPathProvider(PackOutput.Target.DATA_PACK, "recipe");
        lootTables = output.createPathProvider(PackOutput.Target.DATA_PACK, "loot_table");
        blockTags = output.createPathProvider(PackOutput.Target.DATA_PACK, "tags/block");
        itemTags = output.createPathProvider(PackOutput.Target.DATA_PACK, "tags/item");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        List<CompletableFuture<?>> writes = new ArrayList<>();
        for (ElementalPlacement.Element element : ElementalPlacement.Element.values()) {
            addPlant(writes, output, element);
            disableCeLily(writes, output, element.id());
        }
        addBiomeModifier(writes, output);
        addFlowerTags(writes, output);
        addStewRecipe(writes, output, "fire_stew", "fire_lily", "minecraft:blaze_rod",
            "iceandfire:fire_stew");
        addStewRecipe(writes, output, "frost_stew", "frost_lily", "minecraft:prismarine_crystals",
            "iceandfire:frost_stew");
        addStewRecipe(writes, output, "lightning_lily_mixture", "lightning_lily", "minecraft:chorus_fruit",
            "iceandfire:lightning_stew");
        return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
    }

    private void addPlant(List<CompletableFuture<?>> writes, CachedOutput output,
        ElementalPlacement.Element element) {
        String id = element.id();
        ResourceLocation block = ResourceLocation.parse("iaftfc:plant/" + id);

        JsonObject simple = new JsonObject();
        simple.addProperty("type", "minecraft:simple_block");
        JsonObject simpleConfig = new JsonObject();
        JsonObject provider = new JsonObject();
        provider.addProperty("type", "minecraft:simple_state_provider");
        provider.add("state", blockState(block.toString()));
        simpleConfig.add("to_place", provider);
        simple.add("config", simpleConfig);
        save(writes, output, configuredFeatures, "iaftfc", id, simple);

        JsonObject directPlaced = new JsonObject();
        directPlaced.addProperty("feature", "iaftfc:" + id);
        JsonArray directPlacement = new JsonArray();
        JsonObject airFilter = new JsonObject();
        airFilter.addProperty("type", "minecraft:block_predicate_filter");
        JsonObject airPredicate = new JsonObject();
        airPredicate.addProperty("type", "tfc:air_or_empty_fluid");
        airFilter.add("predicate", airPredicate);
        directPlacement.add(airFilter);
        JsonObject survivalFilter = new JsonObject();
        survivalFilter.addProperty("type", "minecraft:block_predicate_filter");
        JsonObject survival = new JsonObject();
        survival.addProperty("type", "minecraft:would_survive");
        survival.add("state", blockState(block.toString()));
        survivalFilter.add("predicate", survival);
        directPlacement.add(survivalFilter);
        directPlaced.add("placement", directPlacement);
        save(writes, output, placedFeatures, "iaftfc", id, directPlaced);

        JsonObject patch = new JsonObject();
        patch.addProperty("type", "minecraft:random_patch");
        JsonObject patchConfig = new JsonObject();
        patchConfig.addProperty("tries", 8);
        patchConfig.addProperty("xz_spread", 7);
        patchConfig.addProperty("y_spread", 1);
        patchConfig.addProperty("feature", "iaftfc:" + id);
        patch.add("config", patchConfig);
        save(writes, output, configuredFeatures, "iaftfc", id + "_patch", patch);

        JsonObject patchPlaced = new JsonObject();
        patchPlaced.addProperty("feature", "iaftfc:" + id + "_patch");
        JsonArray patchPlacement = new JsonArray();
        patchPlacement.add(type("minecraft:rarity_filter", "chance", 32));
        patchPlacement.add(type("minecraft:in_square"));
        patchPlacement.add(type("minecraft:heightmap", "heightmap", "WORLD_SURFACE_WG"));
        JsonObject climate = new JsonObject();
        climate.addProperty("type", "tfc:climate");
        climate.addProperty("min_temperature", element.minTemperature());
        climate.addProperty("max_temperature", element.maxTemperature());
        climate.addProperty("min_groundwater", element.minGroundwater());
        climate.addProperty("max_groundwater", element.maxGroundwater());
        patchPlacement.add(climate);
        patchPlaced.add("placement", patchPlacement);
        save(writes, output, placedFeatures, "iaftfc", id + "_patch", patchPlaced);

        JsonObject loot = new JsonObject();
        loot.addProperty("type", "minecraft:block");
        JsonObject pool = new JsonObject();
        pool.addProperty("name", "loot_pool");
        pool.addProperty("rolls", 1);
        JsonObject entry = new JsonObject();
        entry.addProperty("type", "minecraft:item");
        entry.addProperty("name", block.toString());
        JsonArray entryConditions = new JsonArray();
        JsonObject anyOf = new JsonObject();
        anyOf.addProperty("condition", "minecraft:any_of");
        JsonArray terms = new JsonArray();
        terms.add(toolTag("tfc:tools/sharp"));
        terms.add(toolTag("c:tools/shear"));
        anyOf.add("terms", terms);
        entryConditions.add(anyOf);
        entry.add("conditions", entryConditions);
        JsonArray entries = new JsonArray();
        entries.add(entry);
        pool.add("entries", entries);
        JsonArray poolConditions = new JsonArray();
        poolConditions.add(condition("minecraft:survives_explosion"));
        pool.add("conditions", poolConditions);
        JsonArray pools = new JsonArray();
        pools.add(pool);
        loot.add("pools", pools);
        save(writes, output, lootTables, "iaftfc", "blocks/plant/" + id, loot);
    }

    private void disableCeLily(List<CompletableFuture<?>> writes, CachedOutput output, String id) {
        JsonObject none = new JsonObject();
        none.addProperty("type", "neoforge:none");
        save(writes, output, biomeModifiers, "iceandfire", id, none);

        JsonObject configured = new JsonObject();
        configured.addProperty("type", "minecraft:no_op");
        configured.add("config", new JsonObject());
        save(writes, output, configuredFeatures, "iceandfire", id, configured);

        JsonObject placed = new JsonObject();
        placed.addProperty("feature", "iceandfire:" + id);
        placed.add("placement", new JsonArray());
        save(writes, output, placedFeatures, "iceandfire", id, placed);

        JsonObject emptyLoot = new JsonObject();
        emptyLoot.addProperty("type", "minecraft:block");
        emptyLoot.add("pools", new JsonArray());
        save(writes, output, lootTables, "iceandfire", "blocks/" + id, emptyLoot);
    }

    private void addBiomeModifier(List<CompletableFuture<?>> writes, CachedOutput output) {
        JsonObject modifier = new JsonObject();
        modifier.addProperty("type", "neoforge:add_features");
        modifier.addProperty("biomes", "#iaftfc:dragon_land");
        JsonArray features = new JsonArray();
        for (ElementalPlacement.Element element : ElementalPlacement.Element.values()) {
            features.add("iaftfc:" + element.id() + "_patch");
        }
        modifier.add("features", features);
        modifier.addProperty("step", "vegetal_decoration");
        save(writes, output, biomeModifiers, "iaftfc", "elemental_lilies", modifier);
    }

    private void addFlowerTags(List<CompletableFuture<?>> writes, CachedOutput output) {
        for (String tag : List.of("minecraft:small_flowers", "minecraft:mineable/hoe",
            "minecraft:replaceable", "tfc:consumes_tool_durability", "tfc:natural_regrowing_plants",
            "tfc:can_be_snow_piled", "item:minecraft:small_flowers", "item:tfc:plants",
            "item:tfc:compost_greens/low")) {
            boolean item = tag.startsWith("item:");
            ResourceLocation location = ResourceLocation.parse(item ? tag.substring(5) : tag);
            JsonObject root = new JsonObject();
            root.addProperty("replace", false);
            JsonArray values = new JsonArray();
            for (ElementalPlacement.Element element : ElementalPlacement.Element.values()) {
                values.add("iaftfc:plant/" + element.id());
            }
            root.add("values", values);
            save(writes, output, item ? itemTags : blockTags, location.getNamespace(), location.getPath(), root);
        }
    }

    private void addStewRecipe(List<CompletableFuture<?>> writes, CachedOutput output, String recipeId,
        String lilyId, String otherIngredient, String result) {
        JsonObject recipe = new JsonObject();
        recipe.addProperty("type", "minecraft:crafting_shapeless");
        recipe.addProperty("category", "misc");
        JsonArray ingredients = new JsonArray();
        ingredients.add(ingredient("minecraft:bowl"));
        ingredients.add(ingredient(otherIngredient));
        ingredients.add(ingredient("iaftfc:plant/" + lilyId));
        recipe.add("ingredients", ingredients);
        JsonObject resultObject = new JsonObject();
        resultObject.addProperty("id", result);
        resultObject.addProperty("count", 1);
        recipe.add("result", resultObject);
        save(writes, output, recipes, "iceandfire", recipeId, recipe);
    }

    private static JsonObject ingredient(String item) {
        JsonObject ingredient = new JsonObject();
        ingredient.addProperty("item", item);
        return ingredient;
    }

    private static JsonObject condition(String condition) {
        JsonObject object = new JsonObject();
        object.addProperty("condition", condition);
        return object;
    }

    private static JsonObject blockState(String name) {
        JsonObject state = new JsonObject();
        state.addProperty("Name", name);
        return state;
    }

    private static JsonObject toolTag(String tag) {
        JsonObject condition = new JsonObject();
        condition.addProperty("condition", "minecraft:match_tool");
        JsonObject predicate = new JsonObject();
        predicate.addProperty("items", "#" + tag);
        condition.add("predicate", predicate);
        return condition;
    }

    private static JsonObject type(String type) {
        JsonObject object = new JsonObject();
        object.addProperty("type", type);
        return object;
    }

    private static JsonObject type(String type, String key, String value) {
        JsonObject object = type(type);
        object.addProperty(key, value);
        return object;
    }

    private static JsonObject type(String type, String key, int value) {
        JsonObject object = type(type);
        object.addProperty(key, value);
        return object;
    }

    private static void save(List<CompletableFuture<?>> writes, CachedOutput output, PackOutput.PathProvider path,
        String namespace, String id, JsonObject json) {
        writes.add(DataProvider.saveStable(output, json,
            path.json(ResourceLocation.fromNamespaceAndPath(namespace, id))));
    }

    @Override
    public String getName() {
        return "IaFTFC elemental lilies";
    }
}
