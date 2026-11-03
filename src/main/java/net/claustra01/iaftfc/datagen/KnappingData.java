package net.claustra01.iaftfc.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.claustra01.iaftfc.knapping.DragonboneParts;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.neoforged.fml.ModList;

public final class KnappingData implements DataProvider {
    private static final String[] COLORS = {
        "red", "green", "bronze", "gray", "blue", "white", "sapphire", "silver",
        "electric", "amethyst", "copper", "black"
    };
    private static final String[] ARMOR = {"helmet", "chestplate", "leggings", "boots"};
    private static final String[][] ARMOR_PATTERNS = {
        {"#####", "#   #", "#   #", "     ", "     "},
        {"#   #", "#####", "#####", "#####", "#####"},
        {"#####", "#####", "## ##", "## ##", "## ##"},
        {"##   ", "##   ", "##   ", "#### ", "#####"}
    };
    private static final String[][] TOOL_PATTERNS = {
        {"  ###", "   ##", "#   #", "##  #", "#### "},
        {"#####", "#   #", " ### ", "#####", "     "},
        {" #   ", "#### ", "#####", "#### ", " #   "},
        {"###  ", "###  ", "###  ", "###  ", " #   "},
        {"#####", "   ##", "     ", "     ", "     "}
    };
    private final PackOutput output;

    public KnappingData(PackOutput output) {
        this.output = output;
    }

    @Override
    public String getName() {
        return "IaFTFC knapping";
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
        JsonObject rock = readMod("tfc", "data/tfc/tfc/knapping_type/rock.json");
        rock.add("input", sizedItem("iceandfire:dragonbone"));
        rock.addProperty("amount_to_consume", 1);
        rock.add("icon", item("iceandfire:dragonbone"));
        write(cache, writes, "data/iaftfc/tfc/knapping_type/dragonbone.json", rock);
        JsonObject leather = readMod("tfc", "data/tfc/tfc/knapping_type/leather.json");
        JsonArray scales = new JsonArray();
        for (String color : COLORS) {
            scales.add("iceandfire:dragonscales_" + color);
        }
        JsonObject tag = new JsonObject();
        tag.add("values", scales);
        write(cache, writes, "data/iaftfc/tags/item/dragon_scales.json", tag);
        JsonObject scaleInput = new JsonObject();
        scaleInput.addProperty("count", 1);
        scaleInput.addProperty("tag", "iaftfc:dragon_scales");
        leather.add("input", scaleInput);
        leather.add("icon", item("iceandfire:dragonscales_red"));
        write(cache, writes, "data/iaftfc/tfc/knapping_type/dragonscale.json", leather);

        for (int index = 0; index < DragonboneParts.TOOLS.length; index++) {
            String tool = DragonboneParts.TOOLS[index];
            String part = DragonboneParts.PARTS[index];
            String id = "iaftfc:dragonbone/" + part;
            String reference = switch (tool) {
                case "sword" -> "ceramic/unfired_sword_blade_mold";
                case "pickaxe" -> "ceramic/unfired_pickaxe_head_mold";
                default -> "stone/" + part + "/metamorphic";
            };
            JsonObject tfc = readMod("tfc", "data/tfc/recipe/knapping/" + reference + ".json");
            JsonObject recipe = knapping("iaftfc:dragonbone", "iceandfire:dragonbone", TOOL_PATTERNS[index], id);
            for (String flag : new String[] {"default_on", "mirror", "rotate"}) {
                if (tfc.has(flag)) {
                    recipe.add(flag, tfc.get(flag));
                }
            }
            write(cache, writes, "data/iaftfc/recipe/knapping/dragonbone/" + part + ".json",
                recipe);
            JsonObject assembly = new JsonObject();
            assembly.addProperty("type", "minecraft:crafting_shaped");
            JsonArray assemblyPattern = new JsonArray();
            assemblyPattern.add("X");
            assemblyPattern.add("S");
            assembly.add("pattern", assemblyPattern);
            JsonObject key = new JsonObject();
            key.add("X", ingredient(id));
            JsonObject rod = new JsonObject();
            rod.addProperty("tag", "c:rods/wooden");
            key.add("S", rod);
            assembly.add("key", key);
            assembly.add("result", item("iceandfire:dragonbone_" + tool));
            write(cache, writes, "data/iaftfc/recipe/crafting/dragonbone/" + tool + ".json", assembly);
            disable(cache, writes, "dragonbone_" + tool);
            JsonObject model = new JsonObject();
            model.addProperty("parent", "item/generated");
            JsonObject texture = new JsonObject();
            texture.addProperty("layer0", "iaftfc:item/dragonbone/" + part);
            model.add("textures", texture);
            write(cache, writes, "assets/iaftfc/models/item/dragonbone/" + part + ".json", model);
        }
        for (int part = 0; part < ARMOR.length; part++) {
            JsonObject tfc = readMod("tfc", "data/tfc/recipe/knapping/leather_" + ARMOR[part] + ".json");
            JsonArray expected = pattern(ARMOR_PATTERNS[part]);
            JsonArray original = tfc.getAsJsonArray("pattern");
            for (int row = 0; row < original.size(); row++) {
                if (!original.get(row).getAsString().equals(expected.get(row).getAsString())) {
                    throw new IOException("TFC leather pattern differs: " + ARMOR[part]);
                }
            }
            for (String color : COLORS) {
                String input = "iceandfire:dragonscales_" + color;
                String result = "iceandfire:armor_" + color + "_" + ARMOR[part];
                JsonObject recipe = knapping("iaftfc:dragonscale", input, ARMOR_PATTERNS[part], result);
                if (tfc.has("default_on")) {
                    recipe.add("default_on", tfc.get("default_on"));
                }
                write(cache, writes, "data/iaftfc/recipe/knapping/dragonscale/" + color + "/"
                    + ARMOR[part] + ".json", recipe);
                disable(cache, writes, "armor_" + (color.equals("amethyst") ? "amythest" : color)
                    + "_" + ARMOR[part]);
            }
        }
    }

    private static JsonObject knapping(String type, String input, String[] rows, String output) {
        JsonObject recipe = new JsonObject();
        recipe.addProperty("type", "tfc:knapping");
        recipe.addProperty("knapping_type", type);
        recipe.add("ingredient", ingredient(input));
        recipe.add("pattern", pattern(rows));
        recipe.add("result", item(output));
        return recipe;
    }

    private static JsonArray pattern(String[] rows) {
        JsonArray pattern = new JsonArray();
        for (String row : rows) {
            pattern.add(row);
        }
        return pattern;
    }

    private static JsonObject item(String id) {
        JsonObject item = new JsonObject();
        item.addProperty("id", id);
        return item;
    }

    private static JsonObject ingredient(String id) {
        JsonObject ingredient = new JsonObject();
        ingredient.addProperty("item", id);
        return ingredient;
    }

    private static JsonObject sizedItem(String id) {
        JsonObject item = new JsonObject();
        item.addProperty("count", 1);
        item.addProperty("item", id);
        return item;
    }

    private void disable(CachedOutput cache, List<CompletableFuture<?>> writes, String name) throws IOException {
        String path = "data/iceandfire/recipe/" + name + ".json";
        readMod("iceandfire", path);
        JsonObject disabled = new JsonObject();
        JsonArray conditions = new JsonArray();
        JsonObject condition = new JsonObject();
        condition.addProperty("type", "neoforge:false");
        conditions.add(condition);
        disabled.add("neoforge:conditions", conditions);
        write(cache, writes, path, disabled);
    }

    private static JsonObject readMod(String mod, String path) throws IOException {
        Path source = ModList.get().getModFileById(mod).getFile().findResource(path);
        return JsonParser.parseString(Files.readString(source, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private void write(CachedOutput cache, List<CompletableFuture<?>> writes, String path, JsonObject json) {
        writes.add(DataProvider.saveStable(cache, json, output.getOutputFolder().resolve(path)));
    }
}
