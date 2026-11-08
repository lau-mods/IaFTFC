package net.claustra01.iaftfc.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import net.claustra01.iaftfc.metal.Dragonsteel;
import net.dries007.tfc.util.Metal;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

public final class DragonsteelData implements DataProvider {
    private static final List<Part> PARTS = List.of(
        new Part(Metal.ItemType.INGOT, 100),
        new Part(Metal.ItemType.DOUBLE_INGOT, 200),
        new Part(Metal.ItemType.SHEET, 200),
        new Part(Metal.ItemType.DOUBLE_SHEET, 400),
        new Part(Metal.ItemType.ROD, 50),
        new Part(Metal.ItemType.AXE_HEAD, 100),
        new Part(Metal.ItemType.HOE_HEAD, 100),
        new Part(Metal.ItemType.PICKAXE_HEAD, 100),
        new Part(Metal.ItemType.SHOVEL_HEAD, 100),
        new Part(Metal.ItemType.SWORD_BLADE, 200),
        new Part(Metal.ItemType.UNFINISHED_HELMET, 400),
        new Part(Metal.ItemType.UNFINISHED_CHESTPLATE, 400),
        new Part(Metal.ItemType.UNFINISHED_GREAVES, 400),
        new Part(Metal.ItemType.UNFINISHED_BOOTS, 200));
    private static final List<FinishedPart> FINISHED_PARTS = List.of(
        new FinishedPart("axe", 100),
        new FinishedPart("hoe", 100),
        new FinishedPart("pickaxe", 100),
        new FinishedPart("shovel", 100),
        new FinishedPart("sword", 200),
        new FinishedPart("helmet", 600),
        new FinishedPart("chestplate", 800),
        new FinishedPart("leggings", 600),
        new FinishedPart("boots", 400));
    private static final List<Tool> TOOLS = List.of(
        new Tool("axe", Metal.ItemType.AXE_HEAD, List.of("punch_last", "hit_second_last", "upset_third_last")),
        new Tool("hoe", Metal.ItemType.HOE_HEAD, List.of("punch_last", "hit_not_last", "bend_not_last")),
        new Tool("pickaxe", Metal.ItemType.PICKAXE_HEAD, List.of("punch_last", "draw_not_last", "bend_not_last")),
        new Tool("shovel", Metal.ItemType.SHOVEL_HEAD, List.of("punch_last", "hit_not_last")),
        new Tool("sword", Metal.ItemType.SWORD_BLADE, List.of("hit_last", "bend_second_last", "bend_third_last")));
    private static final List<Armor> ARMOR = List.of(
        new Armor("helmet", Metal.ItemType.UNFINISHED_HELMET, "sheets"),
        new Armor("chestplate", Metal.ItemType.UNFINISHED_CHESTPLATE, "double_sheets"),
        new Armor("leggings", Metal.ItemType.UNFINISHED_GREAVES, "sheets"),
        new Armor("boots", Metal.ItemType.UNFINISHED_BOOTS, "sheets"));

    private final PackOutput.PathProvider recipes;
    private final PackOutput.PathProvider itemHeat;
    private final PackOutput.PathProvider fluidHeat;
    private final PackOutput.PathProvider itemTags;
    private final PackOutput.PathProvider blockTags;
    private final PackOutput.PathProvider fluidTags;
    private final PackOutput.PathProvider lootTables;
    private final PackOutput.PathProvider blockstates;
    private final PackOutput.PathProvider blockModels;
    private final PackOutput.PathProvider itemModels;

    public DragonsteelData(PackOutput output) {
        recipes = output.createPathProvider(PackOutput.Target.DATA_PACK, "recipe");
        itemHeat = output.createPathProvider(PackOutput.Target.DATA_PACK, "tfc/item_heat");
        fluidHeat = output.createPathProvider(PackOutput.Target.DATA_PACK, "tfc/fluid_heat");
        itemTags = output.createPathProvider(PackOutput.Target.DATA_PACK, "tags/item");
        blockTags = output.createPathProvider(PackOutput.Target.DATA_PACK, "tags/block");
        fluidTags = output.createPathProvider(PackOutput.Target.DATA_PACK, "tags/fluid");
        lootTables = output.createPathProvider(PackOutput.Target.DATA_PACK, "loot_table/blocks");
        blockstates = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "blockstates");
        blockModels = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "models/block");
        itemModels = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "models/item");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        List<CompletableFuture<?>> writes = new ArrayList<>();
        for (Dragonsteel metal : Dragonsteel.values()) {
            addMetalData(writes, output, metal);
            addRecipes(writes, output, metal);
            addTags(writes, output, metal);
            addAssets(writes, output, metal);
        }
        addAggregateTags(writes, output);
        addForgeBricks(writes, output);
        return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
    }

    private void addMetalData(List<CompletableFuture<?>> writes, CachedOutput output, Dragonsteel metal) {
        JsonObject fluid = new JsonObject();
        fluid.addProperty("fluid", molten(metal));
        fluid.addProperty("melt_temperature", Dragonsteel.MELT_TEMPERATURE);
        fluid.addProperty("specific_heat_capacity", 0.008571429F);
        save(writes, output, fluidHeat, "iaftfc", metal.getSerializedName(), fluid);

        for (Part part : PARTS) {
            JsonObject heat = heat(part.units());
            heat.add("ingredient", ingredientForPart(metal, part.type()));
            save(writes, output, itemHeat, "iaftfc",
                metal.getSerializedName() + "/" + part.id(), heat);
        }
        for (FinishedPart part : FINISHED_PARTS) {
            JsonObject heat = heat(part.units());
            heat.add("ingredient", item(ceFinished(metal, part.id())));
            save(writes, output, itemHeat, "iaftfc",
                metal.getSerializedName() + "/finished_" + part.id(), heat);
        }
        addBlockHeat(writes, output, metal, Metal.BlockType.BLOCK, 100);
        addBlockHeat(writes, output, metal, Metal.BlockType.BLOCK_SLAB, 50);
        addBlockHeat(writes, output, metal, Metal.BlockType.BLOCK_STAIRS, 75);
        addBlockHeat(writes, output, metal, Metal.BlockType.ANVIL, 1400);
    }

    private void addBlockHeat(List<CompletableFuture<?>> writes, CachedOutput output, Dragonsteel metal,
        Metal.BlockType type, int units) {
        JsonObject heat = heat(units);
        heat.add("ingredient", item(blockId(metal, type)));
        save(writes, output, itemHeat, "iaftfc",
            metal.getSerializedName() + "/" + type.name().toLowerCase(Locale.ROOT), heat);
    }

    private void addRecipes(List<CompletableFuture<?>> writes, CachedOutput output, Dragonsteel metal) {
        addDragonForgeRecipe(writes, output, metal);
        addCastingRecipes(writes, output, metal);
        for (Part part : PARTS) {
            addHeatingRecipe(writes, output, metal, part.id(), ingredientForPart(metal, part.type()), part.units());
        }
        for (FinishedPart part : FINISHED_PARTS) {
            addHeatingRecipe(writes, output, metal, "finished_" + part.id(), item(ceFinished(metal, part.id())),
                part.units());
        }
        addHeatingRecipe(writes, output, metal, "block", item(blockId(metal, Metal.BlockType.BLOCK)), 100);
        addHeatingRecipe(writes, output, metal, "block_slab", item(blockId(metal, Metal.BlockType.BLOCK_SLAB)), 50);
        addHeatingRecipe(writes, output, metal, "block_stairs", item(blockId(metal, Metal.BlockType.BLOCK_STAIRS)), 75);
        addHeatingRecipe(writes, output, metal, "anvil", item(blockId(metal, Metal.BlockType.ANVIL)), 1400);

        addWelding(writes, output, ResourceLocation.fromNamespaceAndPath("iaftfc", "welding/" + metal.getSerializedName() + "/double_ingot"), tag(commonTag("ingots", metal)),
            tag(commonTag("ingots", metal)), itemId(metal, Metal.ItemType.DOUBLE_INGOT), 6, null);
        addWelding(writes, output, ResourceLocation.fromNamespaceAndPath("iaftfc", "welding/" + metal.getSerializedName() + "/double_sheet"), tag(commonTag("sheets", metal)),
            tag(commonTag("sheets", metal)), itemId(metal, Metal.ItemType.DOUBLE_SHEET), 7, null);

        addAnvil(writes, output, metal, "sheet", tag(commonTag("double_ingots", metal)),
            itemId(metal, Metal.ItemType.SHEET), List.of("hit_last", "hit_second_last", "hit_third_last"), true);
        addAnvil(writes, output, metal, "rod", tag(commonTag("ingots", metal)),
            itemId(metal, Metal.ItemType.ROD), List.of("hit_last", "hit_second_last", "hit_third_last"), false, 2);
        for (Tool tool : TOOLS) {
            addAnvil(writes, output, metal, tool.id(),
                tag(commonTag(tool.type() == Metal.ItemType.SWORD_BLADE ? "double_ingots" : "ingots", metal)),
                itemId(metal, tool.type()), tool.rules(), true);
            addToolAssembly(writes, output, metal, tool);
        }

        addAnvil(writes, output, metal, "unfinished_helmet", tag(commonTag("double_sheets", metal)),
            itemId(metal, Metal.ItemType.UNFINISHED_HELMET),
            List.of("hit_last", "bend_second_last", "bend_third_last"), true);
        addAnvil(writes, output, metal, "unfinished_chestplate", tag(commonTag("double_sheets", metal)),
            itemId(metal, Metal.ItemType.UNFINISHED_CHESTPLATE),
            List.of("hit_last", "hit_second_last", "upset_third_last"), true);
        addAnvil(writes, output, metal, "unfinished_greaves", tag(commonTag("double_sheets", metal)),
            itemId(metal, Metal.ItemType.UNFINISHED_GREAVES),
            List.of("hit_any", "draw_any", "bend_any"), true);
        addAnvil(writes, output, metal, "unfinished_boots", tag(commonTag("sheets", metal)),
            itemId(metal, Metal.ItemType.UNFINISHED_BOOTS),
            List.of("bend_last", "bend_second_last", "shrink_third_last"), true);

        for (Armor armor : ARMOR) {
            addWelding(writes, output, ResourceLocation.fromNamespaceAndPath("iceandfire", metal.getSerializedName() + "_" + armor.id()), item(itemId(metal, armor.unfinished())),
                tag(commonTag(armor.sheetTag(), metal)), ceFinished(metal, armor.id()), 7, "copy_best");
        }
        addBlockRecipes(writes, output, metal);
    }

    private void addDragonForgeRecipe(List<CompletableFuture<?>> writes, CachedOutput output, Dragonsteel metal) {
        JsonObject recipe = new JsonObject();
        recipe.addProperty("type", "iceandfire:dragonforge");
        recipe.addProperty("dragonType", element(metal));
        recipe.addProperty("cookTime", 1000);
        recipe.add("input", item(switch (metal) {
            case FIRE -> "tfc:metal/ingot/red_steel";
            case ICE -> "tfc:metal/ingot/blue_steel";
            case LIGHTNING -> "tfc:metal/ingot/black_steel";
        }));
        recipe.add("blood", item("iceandfire:" + element(metal) + "_dragon_blood"));
        recipe.add("result", stack(itemId(metal, Metal.ItemType.INGOT)));
        save(writes, output, recipes, "iceandfire", "dragonforge/" + metal.getSerializedName() + "_ingot", recipe);
    }

    private void addCastingRecipes(List<CompletableFuture<?>> writes, CachedOutput output, Dragonsteel metal) {
        addCasting(writes, output, metal, "ingot", "tfc:ceramic/ingot_mold", 0.1F);
        addCasting(writes, output, metal, "fire_ingot", "tfc:ceramic/fire_ingot_mold", 0.01F);
    }

    private void addCasting(List<CompletableFuture<?>> writes, CachedOutput output, Dragonsteel metal,
        String suffix, String mold, float breakChance) {
        JsonObject recipe = new JsonObject();
        recipe.addProperty("type", "tfc:casting");
        recipe.addProperty("break_chance", breakChance);
        JsonObject fluid = new JsonObject();
        fluid.addProperty("amount", 100);
        fluid.addProperty("fluid", molten(metal));
        recipe.add("fluid", fluid);
        recipe.add("mold", item(mold));
        recipe.add("result", stack(itemId(metal, Metal.ItemType.INGOT)));
        save(writes, output, recipes, "iaftfc", "casting/" + metal.getSerializedName() + "_" + suffix, recipe);
    }

    private void addHeatingRecipe(List<CompletableFuture<?>> writes, CachedOutput output, Dragonsteel metal,
        String id, JsonObject ingredient, int units) {
        JsonObject recipe = new JsonObject();
        recipe.addProperty("type", "tfc:heating");
        recipe.add("ingredient", ingredient);
        JsonObject result = new JsonObject();
        result.addProperty("amount", units);
        result.addProperty("id", molten(metal));
        recipe.add("result_fluid", result);
        recipe.addProperty("temperature", Dragonsteel.MELT_TEMPERATURE);
        save(writes, output, recipes, "iaftfc", "heating/" + metal.getSerializedName() + "/" + id, recipe);
    }

    private void addWelding(List<CompletableFuture<?>> writes, CachedOutput output, ResourceLocation id,
        JsonObject first, JsonObject second, String result, int tier, String bonus) {
        JsonObject recipe = new JsonObject();
        recipe.addProperty("type", "tfc:welding");
        recipe.add("first_input", first);
        recipe.add("second_input", second);
        recipe.addProperty("tier", tier);
        recipe.add("result", stack(result));
        if (bonus != null) {
            recipe.addProperty("bonus", bonus);
        }
        save(writes, output, recipes, id.getNamespace(), id.getPath(), recipe);
    }

    private void addAnvil(List<CompletableFuture<?>> writes, CachedOutput output, Dragonsteel metal,
        String id, JsonObject ingredient, String result, List<String> rules, boolean bonus) {
        addAnvil(writes, output, metal, id, ingredient, result, rules, bonus, 1);
    }

    private void addAnvil(List<CompletableFuture<?>> writes, CachedOutput output, Dragonsteel metal,
        String id, JsonObject ingredient, String result, List<String> rules, boolean bonus, int count) {
        JsonObject recipe = new JsonObject();
        recipe.addProperty("type", "tfc:anvil");
        recipe.add("ingredient", ingredient);
        recipe.addProperty("tier", Dragonsteel.ANVIL_TIER);
        recipe.add("rules", strings(rules));
        if (bonus) {
            recipe.addProperty("apply_bonus", true);
        }
        recipe.add("result", stack(result, count));
        save(writes, output, recipes, "iaftfc", "anvil/" + metal.getSerializedName() + "/" + id, recipe);
    }

    private void addToolAssembly(List<CompletableFuture<?>> writes, CachedOutput output, Dragonsteel metal,
        Tool tool) {
        JsonObject recipe = new JsonObject();
        recipe.addProperty("type", "tfc:advanced_shaped_crafting");
        recipe.add("pattern", strings(List.of("X", "S")));
        JsonObject key = new JsonObject();
        key.add("X", item(itemId(metal, tool.type())));
        key.add("S", tag("c:rods/wooden"));
        recipe.add("key", key);
        JsonObject provider = new JsonObject();
        provider.add("stack", stack(ceFinished(metal, tool.id())));
        JsonArray modifiers = new JsonArray();
        JsonObject modifier = new JsonObject();
        modifier.addProperty("type", "tfc:copy_forging_bonus");
        modifiers.add(modifier);
        provider.add("modifiers", modifiers);
        recipe.add("result", provider);
        save(writes, output, recipes, "iceandfire", metal.getSerializedName() + "_" + tool.id(), recipe);
    }

    private void addBlockRecipes(List<CompletableFuture<?>> writes, CachedOutput output, Dragonsteel metal) {
        JsonObject plated = new JsonObject();
        plated.addProperty("type", "tfc:advanced_shaped_crafting");
        plated.add("pattern", strings(List.of(" SH", "SWS", " S ")));
        JsonObject key = new JsonObject();
        key.add("S", tag(commonTag("sheets", metal)));
        key.add("H", tag("c:tools/hammer"));
        key.add("W", tag("minecraft:planks"));
        plated.add("key", key);
        plated.addProperty("input_column", 2);
        plated.add("result", stack(blockId(metal, Metal.BlockType.BLOCK), 8));
        JsonObject remainder = new JsonObject();
        JsonArray modifiers = new JsonArray();
        JsonObject damage = new JsonObject();
        damage.addProperty("type", "tfc:damage_crafting_remainder");
        modifiers.add(damage);
        remainder.add("modifiers", modifiers);
        plated.add("remainder", remainder);
        save(writes, output, recipes, "iaftfc", "crafting/metal/block/" + metal.getSerializedName(), plated);

        addShapedBlock(writes, output, metal, "slab", List.of("BBB"), 6);
        addShapedBlock(writes, output, metal, "stairs", List.of("B  ", "BB ", "BBB"), 8);
        addShapedAnvil(writes, output, metal);
    }

    private void addShapedBlock(List<CompletableFuture<?>> writes, CachedOutput output, Dragonsteel metal,
        String shape, List<String> pattern, int count) {
        JsonObject recipe = new JsonObject();
        recipe.addProperty("type", "minecraft:crafting_shaped");
        recipe.addProperty("category", "building");
        recipe.add("pattern", strings(pattern));
        JsonObject key = new JsonObject();
        key.add("B", tag(commonTag("storage_blocks", metal)));
        recipe.add("key", key);
        recipe.add("result", stack(blockId(metal,
            shape.equals("slab") ? Metal.BlockType.BLOCK_SLAB : Metal.BlockType.BLOCK_STAIRS), count));
        save(writes, output, recipes, "iaftfc",
            "crafting/metal/block/" + metal.getSerializedName() + "_" + shape, recipe);
    }

    private void addShapedAnvil(List<CompletableFuture<?>> writes, CachedOutput output, Dragonsteel metal) {
        JsonObject recipe = new JsonObject();
        recipe.addProperty("type", "minecraft:crafting_shaped");
        recipe.addProperty("category", "misc");
        recipe.add("pattern", strings(List.of("###", " # ", "###")));
        JsonObject key = new JsonObject();
        key.add("#", tag(commonTag("double_ingots", metal)));
        recipe.add("key", key);
        recipe.add("result", stack(blockId(metal, Metal.BlockType.ANVIL)));
        save(writes, output, recipes, "iaftfc", "crafting/metal/anvil/" + metal.getSerializedName(), recipe);
    }

    private void addForgeBricks(List<CompletableFuture<?>> writes, CachedOutput output) {
        for (String element : List.of("fire", "ice", "lightning")) {
            JsonObject recipe = new JsonObject();
            recipe.addProperty("type", "minecraft:crafting_shaped");
            recipe.addProperty("category", "building");
            recipe.add("pattern", strings(List.of("SBS", "BSB", "SBS")));
            JsonObject key = new JsonObject();
            key.add("B", item("tfc:fire_bricks"));
            key.add("S", tag("iceandfire:storage_blocks/scales/dragon/" + element));
            recipe.add("key", key);
            recipe.add("result", stack("iceandfire:dragonforge_" + element + "_brick", 4));
            recipe.addProperty("show_notification", true);
            save(writes, output, recipes, "iceandfire", "dragonforge_" + element + "_brick", recipe);
        }
    }

    private void addTags(List<CompletableFuture<?>> writes, CachedOutput output, Dragonsteel metal) {
        addTag(writes, output, itemTags, "c", "ingots/" + metal.getSerializedName(),
            itemId(metal, Metal.ItemType.INGOT));
        addTag(writes, output, itemTags, "c", "double_ingots/" + metal.getSerializedName(),
            itemId(metal, Metal.ItemType.DOUBLE_INGOT));
        addTag(writes, output, itemTags, "c", "sheets/" + metal.getSerializedName(),
            itemId(metal, Metal.ItemType.SHEET));
        addTag(writes, output, itemTags, "c", "double_sheets/" + metal.getSerializedName(),
            itemId(metal, Metal.ItemType.DOUBLE_SHEET));
        addTag(writes, output, itemTags, "c", "rods/" + metal.getSerializedName(),
            itemId(metal, Metal.ItemType.ROD));
        addTag(writes, output, itemTags, "c", "storage_blocks/" + metal.getSerializedName(),
            blockId(metal, Metal.BlockType.BLOCK));
        addTag(writes, output, blockTags, "c", "storage_blocks/" + metal.getSerializedName(),
            blockId(metal, Metal.BlockType.BLOCK));
        addTag(writes, output, fluidTags, "c", "molten_" + metal.getSerializedName(),
            molten(metal), "tfc:metal/flowing_" + metal.getSerializedName());
    }

    private void addAggregateTags(List<CompletableFuture<?>> writes, CachedOutput output) {
        List<String> molten = new ArrayList<>();
        List<String> commonMolten = new ArrayList<>();
        List<String> anvils = new ArrayList<>();
        List<String> platedBlocks = new ArrayList<>();
        List<String> mineable = new ArrayList<>();
        for (Dragonsteel metal : Dragonsteel.values()) {
            molten.add(molten(metal));
            commonMolten.add(molten(metal));
            commonMolten.add("tfc:metal/flowing_" + metal.getSerializedName());
            anvils.add(blockId(metal, Metal.BlockType.ANVIL));
            platedBlocks.add(blockId(metal, Metal.BlockType.BLOCK));
            for (Metal.BlockType type : Dragonsteel.BLOCK_TYPES) {
                mineable.add(blockId(metal, type));
            }
        }
        addTag(writes, output, fluidTags, "tfc", "molten_metals", molten.toArray(String[]::new));
        addTag(writes, output, fluidTags, "c", "molten_metal", commonMolten.toArray(String[]::new));
        for (String form : List.of("ingots", "double_ingots", "sheets", "double_sheets", "rods")) {
            List<String> metalTags = new ArrayList<>();
            for (Dragonsteel metal : Dragonsteel.values()) {
                metalTags.add("#c:" + form + "/" + metal.getSerializedName());
            }
            addTag(writes, output, itemTags, "c", form, metalTags.toArray(String[]::new));
        }
        addTag(writes, output, blockTags, "tfc", "anvils", anvils.toArray(String[]::new));
        addTag(writes, output, itemTags, "tfc", "anvils", anvils.toArray(String[]::new));
        addTag(writes, output, itemTags, "tfc", "metal_plated_blocks", platedBlocks.toArray(String[]::new));
        addTag(writes, output, blockTags, "minecraft", "mineable/pickaxe", mineable.toArray(String[]::new));
    }

    private void addAssets(List<CompletableFuture<?>> writes, CachedOutput output, Dragonsteel metal) {
        for (Part part : PARTS) {
            JsonObject model = new JsonObject();
            model.addProperty("parent", "minecraft:item/generated");
            JsonObject textures = new JsonObject();
            textures.addProperty("layer0", "iaftfc:item/metal/" + part.id() + "/" + metal.getSerializedName());
            model.add("textures", textures);
            save(writes, output, itemModels, "iaftfc", "metal/" + part.id() + "/" + metal.getSerializedName(), model);
        }
        JsonObject bucket = new JsonObject();
        bucket.addProperty("loader", "tfc:fluid_container");
        bucket.addProperty("parent", "neoforge:item/default");
        JsonObject bucketTextures = new JsonObject();
        bucketTextures.addProperty("base", "minecraft:item/bucket");
        bucketTextures.addProperty("fluid", "tfc:item/metal/bucket/overlay");
        bucket.add("textures", bucketTextures);
        save(writes, output, itemModels, "iaftfc", "metal/bucket/" + metal.getSerializedName(), bucket);

        addBlockModel(writes, output, metal);
        addBlockLoot(writes, output, metal);
    }

    private void addBlockModel(List<CompletableFuture<?>> writes, CachedOutput output, Dragonsteel metal) {
        String name = metal.getSerializedName();
        String texture = "iaftfc:block/metal/block/" + name;
        save(writes, output, blockModels, "iaftfc", "metal/block/" + name,
            model("minecraft:block/cube_all", "all", texture));
        save(writes, output, blockModels, "iaftfc", "metal/block/" + name + "_slab",
            model("minecraft:block/slab", texture));
        save(writes, output, blockModels, "iaftfc", "metal/block/" + name + "_slab_top",
            model("minecraft:block/slab_top", texture));
        save(writes, output, blockModels, "iaftfc", "metal/block/" + name + "_stairs",
            model("minecraft:block/stairs", texture));
        save(writes, output, blockModels, "iaftfc", "metal/block/" + name + "_stairs_inner",
            model("minecraft:block/inner_stairs", texture));
        save(writes, output, blockModels, "iaftfc", "metal/block/" + name + "_stairs_outer",
            model("minecraft:block/outer_stairs", texture));
        JsonObject anvil = model("tfc:block/anvil", "all", "iaftfc:block/metal/smooth/" + name);
        anvil.getAsJsonObject("textures").addProperty("particle", "iaftfc:block/metal/smooth/" + name);
        save(writes, output, blockModels, "iaftfc", "metal/anvil/" + name, anvil);

        save(writes, output, itemModels, "iaftfc", "metal/block/" + name,
            parent("iaftfc:block/metal/block/" + name));
        save(writes, output, itemModels, "iaftfc", "metal/block/" + name + "_slab",
            parent("iaftfc:block/metal/block/" + name + "_slab"));
        save(writes, output, itemModels, "iaftfc", "metal/block/" + name + "_stairs",
            parent("iaftfc:block/metal/block/" + name + "_stairs"));
        save(writes, output, itemModels, "iaftfc", "metal/anvil/" + name,
            parent("iaftfc:block/metal/anvil/" + name));

        JsonObject block = new JsonObject();
        JsonObject blockVariants = new JsonObject();
        blockVariants.add("", variant("iaftfc:block/metal/block/" + name, 0, 0));
        block.add("variants", blockVariants);
        save(writes, output, blockstates, "iaftfc", "metal/block/" + name, block);
        save(writes, output, blockstates, "iaftfc", "metal/block/" + name + "_slab", slabState(name));
        save(writes, output, blockstates, "iaftfc", "metal/block/" + name + "_stairs", stairsState(name));
        save(writes, output, blockstates, "iaftfc", "metal/anvil/" + name, anvilState(name));
    }

    private void addBlockLoot(List<CompletableFuture<?>> writes, CachedOutput output, Dragonsteel metal) {
        for (Metal.BlockType type : Dragonsteel.BLOCK_TYPES) {
            String id = blockId(metal, type);
            JsonObject entry = new JsonObject();
            entry.addProperty("type", "minecraft:item");
            entry.addProperty("name", id);
            if (type == Metal.BlockType.BLOCK_SLAB) {
                JsonObject function = new JsonObject();
                function.addProperty("function", "minecraft:set_count");
                function.addProperty("count", 2);
                function.addProperty("add", false);
                JsonObject condition = new JsonObject();
                condition.addProperty("condition", "minecraft:block_state_property");
                condition.addProperty("block", id);
                JsonObject properties = new JsonObject();
                properties.addProperty("type", "double");
                condition.add("properties", properties);
                JsonArray conditions = new JsonArray();
                conditions.add(condition);
                function.add("conditions", conditions);
                JsonArray functions = new JsonArray();
                functions.add(function);
                entry.add("functions", functions);
            }
            JsonObject table = new JsonObject();
            table.addProperty("type", "minecraft:block");
            JsonObject pool = new JsonObject();
            pool.addProperty("name", "loot_pool");
            pool.addProperty("rolls", 1);
            JsonArray entries = new JsonArray();
            entries.add(entry);
            pool.add("entries", entries);
            JsonObject survives = new JsonObject();
            survives.addProperty("condition", "minecraft:survives_explosion");
            JsonArray conditions = new JsonArray();
            conditions.add(survives);
            pool.add("conditions", conditions);
            JsonArray pools = new JsonArray();
            pools.add(pool);
            table.add("pools", pools);
            save(writes, output, lootTables, "iaftfc", id.substring("iaftfc:".length()), table);
        }
    }

    private static JsonObject heat(int units) {
        JsonObject heat = new JsonObject();
        heat.addProperty("heat_capacity", units / 35F);
        heat.addProperty("forging_temperature", Dragonsteel.FORGING_TEMPERATURE);
        heat.addProperty("welding_temperature", Dragonsteel.WELDING_TEMPERATURE);
        return heat;
    }

    private static JsonObject ingredientForPart(Dragonsteel metal, Metal.ItemType type) {
        return switch (type) {
            case INGOT -> tag(commonTag("ingots", metal));
            case DOUBLE_INGOT -> tag(commonTag("double_ingots", metal));
            case SHEET -> tag(commonTag("sheets", metal));
            case DOUBLE_SHEET -> tag(commonTag("double_sheets", metal));
            case ROD -> tag(commonTag("rods", metal));
            default -> item(itemId(metal, type));
        };
    }

    private static JsonObject model(String parent, String key, String texture) {
        JsonObject model = new JsonObject();
        model.addProperty("parent", parent);
        JsonObject textures = new JsonObject();
        textures.addProperty(key, texture);
        model.add("textures", textures);
        return model;
    }

    private static JsonObject model(String parent, String texture) {
        JsonObject model = new JsonObject();
        model.addProperty("parent", parent);
        JsonObject textures = new JsonObject();
        textures.addProperty("bottom", texture);
        textures.addProperty("top", texture);
        textures.addProperty("side", texture);
        model.add("textures", textures);
        return model;
    }

    private static JsonObject parent(String parent) {
        JsonObject model = new JsonObject();
        model.addProperty("parent", parent);
        return model;
    }

    private static JsonObject slabState(String name) {
        JsonObject state = new JsonObject();
        JsonObject variants = new JsonObject();
        variants.add("type=bottom", variant("iaftfc:block/metal/block/" + name + "_slab", 0, 0));
        variants.add("type=top", variant("iaftfc:block/metal/block/" + name + "_slab_top", 0, 0));
        variants.add("type=double", variant("iaftfc:block/metal/block/" + name, 0, 0));
        state.add("variants", variants);
        return state;
    }

    private static JsonObject stairsState(String name) {
        JsonObject state = new JsonObject();
        JsonObject variants = new JsonObject();
        for (String half : List.of("bottom", "top")) {
            for (String shape : List.of("straight", "outer_right", "outer_left", "inner_right", "inner_left")) {
                for (String facing : List.of("east", "west", "south", "north")) {
                    int y = stairsRotation(half, shape, facing);
                    String suffix = shape.startsWith("outer") ? "_outer" : shape.startsWith("inner") ? "_inner" : "";
                    variants.add("facing=" + facing + ",half=" + half + ",shape=" + shape,
                        variant("iaftfc:block/metal/block/" + name + "_stairs" + suffix,
                            half.equals("top") ? 180 : 0, y));
                }
            }
        }
        state.add("variants", variants);
        return state;
    }

    private static int stairsRotation(String half, String shape, String facing) {
        int[] straight = {0, 180, 90, 270};
        int[] left = {270, 90, 0, 180};
        int[] topRight = {90, 270, 180, 0};
        int index = List.of("east", "west", "south", "north").indexOf(facing);
        if (half.equals("bottom")) {
            return (shape.endsWith("left") ? left : straight)[index];
        }
        return (shape.endsWith("right") ? topRight : straight)[index];
    }

    private static JsonObject anvilState(String name) {
        JsonObject state = new JsonObject();
        JsonObject variants = new JsonObject();
        variants.add("facing=north", variant("iaftfc:block/metal/anvil/" + name, 0, 90));
        variants.add("facing=east", variant("iaftfc:block/metal/anvil/" + name, 0, 180));
        variants.add("facing=south", variant("iaftfc:block/metal/anvil/" + name, 0, 270));
        variants.add("facing=west", variant("iaftfc:block/metal/anvil/" + name, 0, 0));
        state.add("variants", variants);
        return state;
    }

    private static JsonObject variant(String model, int x, int y) {
        JsonObject variant = new JsonObject();
        variant.addProperty("model", model);
        if (x != 0) {
            variant.addProperty("x", x);
        }
        if (y != 0) {
            variant.addProperty("y", y);
        }
        if (x != 0 || y != 0) {
            variant.addProperty("uvlock", true);
        }
        return variant;
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
        return stack(id, 1);
    }

    private static JsonObject stack(String id, int count) {
        JsonObject stack = new JsonObject();
        stack.addProperty("count", count);
        stack.addProperty("id", id);
        return stack;
    }

    private static JsonArray strings(List<String> strings) {
        JsonArray array = new JsonArray();
        strings.forEach(array::add);
        return array;
    }

    private void addTag(List<CompletableFuture<?>> writes, CachedOutput output, PackOutput.PathProvider provider,
        String namespace, String path, String... values) {
        JsonObject tag = new JsonObject();
        tag.addProperty("replace", false);
        JsonArray entries = new JsonArray();
        for (String value : values) {
            entries.add(value);
        }
        tag.add("values", entries);
        save(writes, output, provider, namespace, path, tag);
    }

    private static String itemId(Dragonsteel metal, Metal.ItemType type) {
        return "iaftfc:metal/" + type.name().toLowerCase(Locale.ROOT) + "/" + metal.getSerializedName();
    }

    private static String blockId(Dragonsteel metal, Metal.BlockType type) {
        return "iaftfc:" + type.createName(metal);
    }

    private static String commonTag(String form, Dragonsteel metal) {
        return "c:" + form + "/" + metal.getSerializedName();
    }

    private static String molten(Dragonsteel metal) {
        return "tfc:metal/" + metal.getSerializedName();
    }

    private static String ceFinished(Dragonsteel metal, String part) {
        return "iceandfire:" + metal.getSerializedName() + "_" + part;
    }

    private static String element(Dragonsteel metal) {
        return metal.name().toLowerCase(Locale.ROOT);
    }

    private static void save(List<CompletableFuture<?>> writes, CachedOutput output,
        PackOutput.PathProvider provider, String namespace, String id, JsonObject json) {
        writes.add(DataProvider.saveStable(output, json,
            provider.json(ResourceLocation.fromNamespaceAndPath(namespace, id))));
    }

    @Override
    public String getName() {
        return "IaFTFC Dragonsteel";
    }

    private record Part(Metal.ItemType type, int units) {
        String id() {
            return type.name().toLowerCase(Locale.ROOT);
        }
    }

    private record FinishedPart(String id, int units) {
    }

    private record Tool(String id, Metal.ItemType type, List<String> rules) {
    }

    private record Armor(String id, Metal.ItemType unfinished, String sheetTag) {
    }
}
