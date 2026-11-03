package net.claustra01.iaftfc.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.claustra01.iaftfc.metal.Dragonsteel;
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

public final class DragonsteelData implements DataProvider {
    private static final String[] ANVIL_PARTS = {
        "sheet", "rod", "sword_blade", "pickaxe_head", "axe_head", "shovel_head", "hoe_head",
        "unfinished_helmet", "unfinished_chestplate", "unfinished_greaves", "unfinished_boots"
    };
    private static final String[] WELDING_PARTS = {
        "double_ingot", "double_sheet", "helmet", "chestplate", "greaves", "boots"
    };
    private final PackOutput output;

    public DragonsteelData(PackOutput output) {
        this.output = output;
    }

    @Override
    public String getName() {
        return "IaFTFC dragonsteel";
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
        JsonArray ingots = new JsonArray();
        JsonArray anvils = new JsonArray();
        JsonArray pickaxeBlocks = new JsonArray();
        JsonArray platedBlocks = new JsonArray();
        JsonArray slabs = new JsonArray();
        JsonArray stairs = new JsonArray();
        JsonArray molten = new JsonArray();
        JsonArray meleeWeapons = new JsonArray();
        for (String metal : Dragonsteel.METALS) {
            String fluid = "iaftfc:metal/" + metal;
            String ingot = item("ingot", metal);
            ingots.add(ingot);
            anvils.add(item("anvil", metal));
            pickaxeBlocks.add(item("anvil", metal));
            pickaxeBlocks.add(item("block", metal));
            platedBlocks.add(item("block", metal));
            for (String shape : new String[] {"slab", "stairs"}) {
                String id = item("block", metal) + "_" + shape;
                pickaxeBlocks.add(id);
                (shape.equals("slab") ? slabs : stairs).add(id);
                blockShape(cache, writes, metal, shape);
            }
            molten.add(fluid);
            meleeWeapons.add("iceandfire:" + metal + "_sword");
            meleeWeapons.add("iceandfire:" + metal + "_axe");
            for (String part : new String[] {"ingot", "double_ingot", "sheet", "double_sheet", "rod"}) {
                String tag = switch (part) {
                    case "ingot" -> "ingots";
                    case "double_ingot" -> "double_ingots";
                    case "sheet" -> "sheets";
                    case "double_sheet" -> "double_sheets";
                    default -> "rods";
                };
                write(cache, writes, "data/c/tags/item/" + tag + "/" + metal + ".json",
                    tag(item(part, metal)));
            }
            write(cache, writes, "data/c/tags/item/storage_blocks/" + metal + ".json",
                tag(item("block", metal)));
            write(cache, writes, "data/c/tags/block/storage_blocks/" + metal + ".json",
                tag(item("block", metal)));
            write(cache, writes, "data/c/tags/fluid/molten_" + metal + ".json", tag(fluid));
            JsonObject fluidHeat = new JsonObject();
            fluidHeat.addProperty("fluid", fluid);
            fluidHeat.addProperty("melt_temperature", 2000);
            fluidHeat.addProperty("specific_heat_capacity", 0.008571429);
            write(cache, writes, "data/iaftfc/tfc/fluid_heat/" + metal + ".json", fluidHeat);
            for (String part : Dragonsteel.PARTS) {
                int amount = amount(part);
                JsonObject heat = new JsonObject();
                heat.addProperty("forging_temperature", 1200);
                heat.addProperty("welding_temperature", 1600);
                heat.addProperty("heat_capacity", 2.857143 * amount / 100);
                JsonObject ingredient = new JsonObject();
                ingredient.addProperty("item", item(part, metal));
                heat.add("ingredient", ingredient);
                write(cache, writes, "data/iaftfc/tfc/item_heat/" + metal + "/" + part + ".json", heat);
                heating(cache, writes, metal, part, item(part, metal), amount);
                JsonObject model = new JsonObject();
                model.addProperty("parent", "item/generated");
                JsonObject textures = new JsonObject();
                textures.addProperty("layer0", "iaftfc:item/metal/" + part + "/" + metal);
                model.add("textures", textures);
                write(cache, writes, "assets/iaftfc/models/item/metal/" + part + "/" + metal + ".json", model);
            }
            for (String part : new String[] {"block", "anvil"}) {
                String lootPath = "data/tfc/loot_table/blocks/metal/" + part + "/red_steel.json";
                JsonObject loot = JsonParser.parseString(readMod("tfc", lootPath).toString()
                    .replace("tfc:metal/", "iaftfc:metal/").replace("red_steel", metal)).getAsJsonObject();
                write(cache, writes, "data/iaftfc/loot_table/blocks/metal/" + part + "/" + metal + ".json", loot);
                JsonObject heat = new JsonObject();
                heat.addProperty("forging_temperature", 1200);
                heat.addProperty("welding_temperature", 1600);
                heat.addProperty("heat_capacity", 2.857143 * amount(part) / 100);
                JsonObject ingredient = new JsonObject();
                ingredient.addProperty("item", item(part, metal));
                heat.add("ingredient", ingredient);
                write(cache, writes, "data/iaftfc/tfc/item_heat/" + metal + "/" + part + ".json", heat);
                heating(cache, writes, metal, part, item(part, metal), amount(part));
            }
            JsonObject blockModel = new JsonObject();
            blockModel.addProperty("parent", "block/cube_all");
            JsonObject blockTextures = new JsonObject();
            blockTextures.addProperty("all", "iaftfc:block/metal/block/" + metal);
            blockModel.add("textures", blockTextures);
            write(cache, writes, "assets/iaftfc/models/block/metal/block/" + metal + ".json", blockModel);
            JsonObject anvilModel = new JsonObject();
            anvilModel.addProperty("parent", "tfc:block/anvil");
            JsonObject anvilTextures = new JsonObject();
            anvilTextures.addProperty("all", "iaftfc:block/metal/smooth/" + metal);
            anvilTextures.addProperty("particle", "iaftfc:block/metal/smooth/" + metal);
            anvilModel.add("textures", anvilTextures);
            write(cache, writes, "assets/iaftfc/models/block/metal/anvil/" + metal + ".json", anvilModel);
            for (String part : new String[] {"block", "anvil"}) {
                JsonObject model = new JsonObject();
                model.addProperty("parent", "iaftfc:block/metal/" + part + "/" + metal);
                write(cache, writes, "assets/iaftfc/models/item/metal/" + part + "/" + metal + ".json", model);
            }
            JsonObject blockState = new JsonObject();
            JsonObject blockVariants = new JsonObject();
            JsonObject blockVariant = new JsonObject();
            blockVariant.addProperty("model", "iaftfc:block/metal/block/" + metal);
            blockVariants.add("", blockVariant);
            blockState.add("variants", blockVariants);
            write(cache, writes, "assets/iaftfc/blockstates/metal/block/" + metal + ".json", blockState);
            JsonObject anvilState = new JsonObject();
            JsonObject anvilVariants = new JsonObject();
            String[] facing = {"north", "east", "south", "west"};
            for (int index = 0; index < facing.length; index++) {
                JsonObject variant = new JsonObject();
                variant.addProperty("model", "iaftfc:block/metal/anvil/" + metal);
                if (index < 3) {
                    variant.addProperty("y", 90 * (index + 1));
                }
                anvilVariants.add("facing=" + facing[index], variant);
            }
            anvilState.add("variants", anvilVariants);
            write(cache, writes, "assets/iaftfc/blockstates/metal/anvil/" + metal + ".json", anvilState);
            JsonObject fluidModel = new JsonObject();
            JsonObject fluidTextures = new JsonObject();
            fluidTextures.addProperty("particle", "tfc:block/molten_still");
            fluidModel.add("textures", fluidTextures);
            write(cache, writes, "assets/iaftfc/models/block/fluid/metal/" + metal + ".json", fluidModel);
            JsonObject fluidState = new JsonObject();
            JsonObject fluidVariants = new JsonObject();
            JsonObject fluidVariant = new JsonObject();
            fluidVariant.addProperty("model", "iaftfc:block/fluid/metal/" + metal);
            fluidVariants.add("", fluidVariant);
            fluidState.add("variants", fluidVariants);
            write(cache, writes, "assets/iaftfc/blockstates/fluid/metal/" + metal + ".json", fluidState);
            JsonObject bucketModel = new JsonObject();
            bucketModel.addProperty("loader", "neoforge:fluid_container");
            bucketModel.addProperty("parent", "neoforge:item/bucket");
            bucketModel.addProperty("fluid", fluid);
            write(cache, writes, "assets/iaftfc/models/item/bucket/metal/" + metal + ".json", bucketModel);
            for (String part : ANVIL_PARTS) {
                copyTfcRecipe(cache, writes, metal, "anvil", part);
            }
            for (String part : WELDING_PARTS) {
                copyTfcRecipe(cache, writes, metal, "welding", part);
            }
            for (String part : new String[] {"helmet", "chestplate", "leggings", "boots"}) {
                JsonObject heat = new JsonObject();
                heat.addProperty("forging_temperature", 1200);
                heat.addProperty("welding_temperature", 1600);
                heat.addProperty("heat_capacity", 2.857143 * switch (part) {
                    case "chestplate" -> 8;
                    case "boots" -> 4;
                    default -> 6;
                });
                JsonObject ingredient = new JsonObject();
                ingredient.addProperty("item", "iceandfire:" + metal + "_" + part);
                heat.add("ingredient", ingredient);
                write(cache, writes, "data/iaftfc/tfc/item_heat/" + metal + "/" + part + ".json", heat);
            }
            for (String part : new String[] {"sword", "pickaxe", "axe", "shovel", "hoe"}) {
                String head = part.equals("sword") ? "sword_blade" : part + "_head";
                JsonObject assembly = readMod("tfc", "data/tfc/recipe/crafting/metal/" + part + "/red_steel.json");
                assembly.getAsJsonObject("key").getAsJsonObject("X")
                    .addProperty("item", item(head, metal));
                assembly.getAsJsonObject("result").getAsJsonObject("stack")
                    .addProperty("id", "iceandfire:" + metal + "_" + part);
                write(cache, writes, "data/iaftfc/recipe/crafting/metal/" + part + "/" + metal + ".json", assembly);
                heating(cache, writes, metal, part, "iceandfire:" + metal + "_" + part, part.equals("sword") ? 200 : 100);
                JsonObject heat = new JsonObject();
                heat.addProperty("forging_temperature", 1200);
                heat.addProperty("welding_temperature", 1600);
                heat.addProperty("heat_capacity", 2.857143 * (part.equals("sword") ? 2 : 1));
                JsonObject ingredient = new JsonObject();
                ingredient.addProperty("item", "iceandfire:" + metal + "_" + part);
                heat.add("ingredient", ingredient);
                write(cache, writes, "data/iaftfc/tfc/item_heat/" + metal + "/" + part + ".json", heat);
            }
            for (String part : new String[] {"helmet", "chestplate", "leggings", "boots"}) {
                heating(cache, writes, metal, part, "iceandfire:" + metal + "_" + part, switch (part) {
                    case "chestplate" -> 800;
                    case "boots" -> 400;
                    default -> 600;
                });
            }
            copyTfcRecipe(cache, writes, metal, "crafting", "anvil");
            copyTfcRecipe(cache, writes, metal, "crafting", "block");
            JsonObject casting = new JsonObject();
            casting.addProperty("type", "tfc:casting");
            casting.addProperty("break_chance", 0.1);
            JsonObject castFluid = new JsonObject();
            castFluid.addProperty("amount", 100);
            castFluid.addProperty("fluid", fluid);
            casting.add("fluid", castFluid);
            JsonObject mold = new JsonObject();
            mold.addProperty("item", "tfc:ceramic/ingot_mold");
            casting.add("mold", mold);
            casting.add("result", result(ingot));
            write(cache, writes, "data/iaftfc/recipe/casting/" + metal + "_ingot.json", casting);
            JsonObject fireCasting = casting.deepCopy();
            fireCasting.addProperty("break_chance", 0.01);
            fireCasting.getAsJsonObject("mold").addProperty("item", "tfc:ceramic/fire_ingot_mold");
            write(cache, writes, "data/iaftfc/recipe/casting/" + metal + "_fire_ingot.json", fireCasting);
        }
        for (int index = 0; index < Dragonsteel.METALS.length; index++) {
            String element = new String[] {"fire", "ice", "lightning"}[index];
            String steel = new String[] {"red_steel", "blue_steel", "black_steel"}[index];
            String forgePath = "data/iceandfire/recipe/dragonforge/dragonsteel_" + element + "_ingot.json";
            JsonObject forge = readMod("iceandfire", forgePath);
            forge.getAsJsonObject("input").addProperty("item", "tfc:metal/ingot/" + steel);
            forge.getAsJsonObject("result").addProperty("id", item("ingot", Dragonsteel.METALS[index]));
            write(cache, writes, forgePath, forge);
            for (String part : new String[] {"brick", "input"}) {
                String path = "data/iceandfire/recipe/dragonforge_" + element + "_" + part + ".json";
                JsonObject recipe = readMod("iceandfire", path);
                JsonObject key = recipe.getAsJsonObject("key").getAsJsonObject(part.equals("brick") ? "B" : "I");
                key.remove("item");
                key.remove("tag");
                key.addProperty("tag", part.equals("brick") ? "minecraft:stone_bricks" : "c:ingots/wrought_iron");
                write(cache, writes, path, recipe);
            }
            for (String suffix : new String[] {"sword", "pickaxe", "axe", "shovel", "hoe",
                "helmet", "chestplate", "leggings", "boots", "block_to_dragonsteel_" + element + "_ingot",
                "ingot_to_dragonsteel_" + element + "_block"}) {
                String path = "data/iceandfire/recipe/dragonsteel_" + element + "_" + suffix + ".json";
                readMod("iceandfire", path);
                JsonObject disabled = new JsonObject();
                JsonArray conditions = new JsonArray();
                JsonObject condition = new JsonObject();
                condition.addProperty("type", "neoforge:false");
                conditions.add(condition);
                disabled.add("neoforge:conditions", conditions);
                write(cache, writes, path, disabled);
            }
        }
        for (String tag : new String[] {"ingots", "double_ingots", "sheets", "double_sheets", "rods"}) {
            JsonArray values = new JsonArray();
            for (String metal : Dragonsteel.METALS) {
                values.add("#c:" + tag + "/" + metal);
            }
            write(cache, writes, "data/c/tags/item/" + tag + ".json", tag(values));
        }
        write(cache, writes, "data/iaftfc/tags/item/dragonsteel_ingots.json", tag(
            item("ingot", Dragonsteel.METALS[0]), item("ingot", Dragonsteel.METALS[1]),
            item("ingot", Dragonsteel.METALS[2])));
        for (String kind : new String[] {"item", "block"}) {
            write(cache, writes, "data/minecraft/tags/" + kind + "/slabs.json", tag(slabs));
            write(cache, writes, "data/minecraft/tags/" + kind + "/stairs.json", tag(stairs));
        }
        write(cache, writes, "data/tfc/tags/item/anvils.json", tag(anvils));
        write(cache, writes, "data/tfc/tags/block/anvils.json", tag(anvils));
        write(cache, writes, "data/minecraft/tags/block/mineable/pickaxe.json", tag(pickaxeBlocks));
        write(cache, writes, "data/tfc/tags/item/metal_plated_blocks.json", tag(platedBlocks));
        write(cache, writes, "data/tfc/tags/fluid/molten_metals.json", tag(molten));
        write(cache, writes, "data/c/tags/item/tools/melee_weapon.json", tag(meleeWeapons));
    }

    private void blockShape(CachedOutput cache, List<CompletableFuture<?>> writes,
        String metal, String shape) throws IOException {
        for (String directory : new String[] {"assets/tfc/blockstates/metal/block/",
            "assets/tfc/models/item/metal/block/", "data/tfc/recipe/crafting/metal/block/",
            "data/tfc/loot_table/blocks/metal/block/"}) {
            copyBlockShape(cache, writes, directory, metal, shape);
        }
        for (String suffix : shape.equals("slab")
            ? new String[] {"slab", "slab_top"} : new String[] {"stairs", "stairs_inner", "stairs_outer"}) {
            copyBlockShape(cache, writes, "assets/tfc/models/block/metal/block/", metal, suffix);
        }
        int amount = shape.equals("slab") ? 50 : 75;
        String id = item("block", metal) + "_" + shape;
        JsonObject heat = new JsonObject();
        JsonObject ingredient = new JsonObject();
        ingredient.addProperty("item", id);
        heat.add("ingredient", ingredient);
        heat.addProperty("heat_capacity", 2.857143 * amount / 100);
        heat.addProperty("forging_temperature", 1200);
        heat.addProperty("welding_temperature", 1600);
        write(cache, writes, "data/iaftfc/tfc/item_heat/" + metal + "/" + shape + ".json", heat);
        JsonObject heating = readMod("tfc", "data/tfc/recipe/heating/metal/block/red_steel_" + shape + ".json");
        heating.getAsJsonObject("ingredient").addProperty("item", id);
        heating.getAsJsonObject("result_fluid").addProperty("id", "iaftfc:metal/" + metal);
        heating.addProperty("temperature", 2000);
        write(cache, writes, "data/iaftfc/recipe/heating/metal/block/" + metal + "_" + shape + ".json", heating);
    }

    private void copyBlockShape(CachedOutput cache, List<CompletableFuture<?>> writes,
        String directory, String metal, String suffix) throws IOException {
        String path = directory + "red_steel_" + suffix + ".json";
        JsonObject data = JsonParser.parseString(readMod("tfc", path).toString()
            .replace("tfc:metal/", "iaftfc:metal/")
            .replace("tfc:block/metal/", "iaftfc:block/metal/")
            .replace("red_steel", metal)).getAsJsonObject();
        write(cache, writes, path.replace("/tfc/", "/iaftfc/").replace("red_steel", metal), data);
    }

    private void heating(CachedOutput cache, List<CompletableFuture<?>> writes,
        String metal, String part, String itemId, int amount) {
        JsonObject recipe = new JsonObject();
        recipe.addProperty("type", "tfc:heating");
        JsonObject ingredient = new JsonObject();
        ingredient.addProperty("item", itemId);
        recipe.add("ingredient", ingredient);
        JsonObject fluid = new JsonObject();
        fluid.addProperty("amount", amount);
        fluid.addProperty("id", "iaftfc:metal/" + metal);
        recipe.add("result_fluid", fluid);
        recipe.addProperty("temperature", 2000);
        String path = itemId.startsWith("iceandfire:") ? "equipment/" + metal + "/" + part
            : "metal/" + part + "/" + metal;
        write(cache, writes, "data/iaftfc/recipe/heating/" + path + ".json", recipe);
    }

    private static JsonObject readMod(String mod, String path) throws IOException {
        Path source = ModList.get().getModFileById(mod).getFile().findResource(path);
        return JsonParser.parseString(Files.readString(source, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private void copyTfcRecipe(CachedOutput cache, List<CompletableFuture<?>> writes,
        String metal, String type, String part) throws IOException {
        String path = "data/tfc/recipe/" + type + "/metal/" + part + "/red_steel.json";
        JsonObject recipe = JsonParser.parseString(readMod("tfc", path).toString()
            .replace("tfc:metal/", "iaftfc:metal/")
            .replace("red_steel", metal)).getAsJsonObject();
        if (recipe.has("tier")) {
            recipe.addProperty("tier", recipe.get("tier").getAsInt() + 1);
        }
        if (type.equals("welding") && !part.equals("double_ingot") && !part.equals("double_sheet")) {
            String armorPart = part.equals("greaves") ? "leggings" : part;
            recipe.add("result", result("iceandfire:" + metal + "_" + armorPart));
        }
        write(cache, writes, "data/iaftfc/recipe/" + type + "/metal/" + part + "/" + metal + ".json", recipe);
    }

    private static int amount(String part) {
        return switch (part) {
            case "rod" -> 50;
            case "double_ingot", "sheet", "sword_blade", "unfinished_boots" -> 200;
            case "double_sheet", "unfinished_helmet", "unfinished_chestplate", "unfinished_greaves" -> 400;
            case "anvil" -> 1400;
            default -> 100;
        };
    }

    private static String item(String part, String metal) {
        return "iaftfc:metal/" + part + "/" + metal;
    }

    private static JsonObject result(String id) {
        JsonObject result = new JsonObject();
        result.addProperty("count", 1);
        result.addProperty("id", id);
        return result;
    }

    private static JsonObject tag(String... values) {
        JsonArray array = new JsonArray();
        for (String value : values) {
            array.add(value);
        }
        return tag(array);
    }

    private static JsonObject tag(JsonArray values) {
        JsonObject tag = new JsonObject();
        tag.addProperty("replace", false);
        tag.add("values", values);
        return tag;
    }

    private void write(CachedOutput cache, List<CompletableFuture<?>> writes, String path, JsonObject json) {
        writes.add(DataProvider.saveStable(cache, json, output.getOutputFolder().resolve(path)));
    }
}
