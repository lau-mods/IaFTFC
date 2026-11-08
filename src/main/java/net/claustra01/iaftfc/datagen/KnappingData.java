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

public final class KnappingData implements DataProvider {
    private static final List<ScaleColor> SCALE_COLORS = List.of(
        new ScaleColor("amethyst", "amythest"),
        new ScaleColor("black", "black"),
        new ScaleColor("blue", "blue"),
        new ScaleColor("bronze", "bronze"),
        new ScaleColor("copper", "copper"),
        new ScaleColor("electric", "electric"),
        new ScaleColor("gray", "gray"),
        new ScaleColor("green", "green"),
        new ScaleColor("red", "red"),
        new ScaleColor("sapphire", "sapphire"),
        new ScaleColor("silver", "silver"),
        new ScaleColor("white", "white"));
    private static final List<ArmorPart> ARMOR_PARTS = List.of(
        new ArmorPart("helmet", List.of("#####", "#   #", "#   #"), true),
        new ArmorPart("chestplate", List.of("#   #", "#####", "#####", "#####", "#####"), false),
        new ArmorPart("leggings", List.of("#####", "#####", "## ##", "## ##", "## ##"), false),
        new ArmorPart("boots", List.of("##   ", "##   ", "##   ", "#### ", "#####"), false));
    private static final List<DragonbonePart> DRAGONBONE_PARTS = List.of(
        new DragonbonePart("axe_head", "axe", List.of(" #   ", "#### ", "#####", "#### ", " #   ")),
        new DragonbonePart("hoe_head", "hoe", List.of("     ", "#####", "   ##", "     ", "     ")),
        new DragonbonePart("pickaxe_head", "pickaxe", List.of("     ", " ### ", "#   #", "     ", "     ")),
        new DragonbonePart("shovel_head", "shovel", List.of(" ### ", " ### ", " ### ", " ### ", "  #  ")),
        new DragonbonePart("sword_blade", "sword", List.of("##   ", "###  ", " ### ", "  ## ", "    #")));
    private static final List<String> FLUTE_PATTERN = List.of(
        "#####", "# # #", "##  #", "#  ##", "#####");

    private final PackOutput.PathProvider knappingTypes;
    private final PackOutput.PathProvider recipes;
    private final PackOutput.PathProvider models;

    public KnappingData(PackOutput output) {
        knappingTypes = output.createPathProvider(PackOutput.Target.DATA_PACK, "tfc/knapping_type");
        recipes = output.createPathProvider(PackOutput.Target.DATA_PACK, "recipe");
        models = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "models/item");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        List<CompletableFuture<?>> writes = new ArrayList<>();
        for (ScaleColor color : SCALE_COLORS) {
            addScaleType(writes, output, color);
            for (ArmorPart part : ARMOR_PARTS) {
                addScaleArmor(writes, output, color, part);
            }
        }
        addDragonboneType(writes, output);
        for (DragonbonePart part : DRAGONBONE_PARTS) {
            addDragonbonePart(writes, output, part);
            addDragonboneTool(writes, output, part);
            addPartModel(writes, output, part.id());
        }
        addDragonFlute(writes, output);
        return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
    }

    private void addScaleType(List<CompletableFuture<?>> writes, CachedOutput output, ScaleColor color) {
        JsonObject type = new JsonObject();
        JsonObject input = new JsonObject();
        input.addProperty("count", 5);
        input.addProperty("item", color.scaleItem());
        type.add("input", input);
        type.addProperty("amount_to_consume", 5);
        type.addProperty("click_sound", "tfc:item.knapping.leather");
        type.addProperty("consume_after_complete", true);
        type.addProperty("has_off_texture", false);
        type.addProperty("spawns_particles", false);
        type.add("icon", stack(color.scaleItem()));
        save(writes, output, knappingTypes, "iaftfc", color.knappingType(), type);
    }

    private void addScaleArmor(List<CompletableFuture<?>> writes, CachedOutput output, ScaleColor color,
        ArmorPart part) {
        JsonObject recipe = knappingRecipe("iaftfc:" + color.knappingType(), part.pattern(), color.armor(part.id()));
        if (part.requiresDefaultOff()) {
            recipe.addProperty("default_on", false);
        }
        save(writes, output, recipes, "iceandfire", "armor_" + color.recipeId() + "_" + part.id(), recipe);
    }

    private void addDragonboneType(List<CompletableFuture<?>> writes, CachedOutput output) {
        JsonObject type = new JsonObject();
        JsonObject input = new JsonObject();
        input.addProperty("count", 2);
        input.addProperty("item", "iceandfire:dragonbone");
        type.add("input", input);
        type.addProperty("amount_to_consume", 1);
        type.addProperty("click_sound", "tfc:item.knapping.stone");
        type.addProperty("consume_after_complete", false);
        type.addProperty("has_off_texture", false);
        type.addProperty("spawns_particles", true);
        type.add("icon", stack("iceandfire:dragonbone"));
        save(writes, output, knappingTypes, "iaftfc", "dragonbone", type);
    }

    private void addDragonbonePart(List<CompletableFuture<?>> writes, CachedOutput output, DragonbonePart part) {
        JsonObject recipe = knappingRecipe("iaftfc:dragonbone", part.pattern(), "iaftfc:dragonbone/" + part.id());
        save(writes, output, recipes, "iaftfc", "knapping/dragonbone/" + part.id(), recipe);
    }

    private void addDragonboneTool(List<CompletableFuture<?>> writes, CachedOutput output, DragonbonePart part) {
        JsonObject recipe = new JsonObject();
        recipe.addProperty("type", "minecraft:crafting_shaped");
        recipe.addProperty("category", "equipment");
        recipe.add("pattern", strings(List.of("X", "S")));
        JsonObject key = new JsonObject();
        key.add("X", ingredient("iaftfc:dragonbone/" + part.id()));
        JsonObject witherBone = new JsonObject();
        witherBone.addProperty("tag", "c:bones/wither");
        key.add("S", witherBone);
        recipe.add("key", key);
        recipe.add("result", stack("iceandfire:dragonbone_" + part.tool()));
        recipe.addProperty("show_notification", true);
        save(writes, output, recipes, "iceandfire", "dragonbone_" + part.tool(), recipe);
    }

    private void addPartModel(List<CompletableFuture<?>> writes, CachedOutput output, String part) {
        JsonObject model = new JsonObject();
        model.addProperty("parent", "minecraft:item/generated");
        JsonObject textures = new JsonObject();
        textures.addProperty("layer0", "iaftfc:item/dragonbone/" + part);
        model.add("textures", textures);
        save(writes, output, models, "iaftfc", "dragonbone/" + part, model);
    }

    private void addDragonFlute(List<CompletableFuture<?>> writes, CachedOutput output) {
        JsonObject recipe = knappingRecipe("iaftfc:dragonbone", FLUTE_PATTERN, "iceandfire:dragon_flute");
        save(writes, output, recipes, "iceandfire", "dragon_flute", recipe);
    }

    private static JsonObject knappingRecipe(String type, List<String> pattern, String result) {
        JsonObject recipe = new JsonObject();
        recipe.addProperty("type", "tfc:knapping");
        recipe.addProperty("knapping_type", type);
        recipe.add("pattern", strings(pattern));
        recipe.add("result", stack(result));
        return recipe;
    }

    private static JsonArray strings(List<String> values) {
        JsonArray array = new JsonArray();
        values.forEach(array::add);
        return array;
    }

    private static JsonObject ingredient(String item) {
        JsonObject ingredient = new JsonObject();
        ingredient.addProperty("item", item);
        return ingredient;
    }

    private static JsonObject stack(String item) {
        JsonObject stack = new JsonObject();
        stack.addProperty("count", 1);
        stack.addProperty("id", item);
        return stack;
    }

    private static void save(List<CompletableFuture<?>> writes, CachedOutput output, PackOutput.PathProvider path,
        String namespace, String id, JsonObject json) {
        writes.add(DataProvider.saveStable(output, json,
            path.json(ResourceLocation.fromNamespaceAndPath(namespace, id))));
    }

    @Override
    public String getName() {
        return "IaFTFC knapping";
    }

    private record ScaleColor(String id, String recipeId) {
        String scaleItem() {
            return "iceandfire:dragonscales_" + id;
        }

        String knappingType() {
            return "dragonscales_" + id;
        }

        String armor(String part) {
            return "iceandfire:armor_" + id + "_" + part;
        }
    }

    private record ArmorPart(String id, List<String> pattern, boolean requiresDefaultOff) {
    }

    private record DragonbonePart(String id, String tool, List<String> pattern) {
    }
}
