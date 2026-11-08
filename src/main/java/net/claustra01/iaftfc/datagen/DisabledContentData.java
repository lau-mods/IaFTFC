package net.claustra01.iaftfc.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;

public final class DisabledContentData implements DataProvider {
    public static final List<String> DISABLED_ITEMS = List.of(
        "silver_ore", "deepslate_silver_ore", "raw_silver", "raw_silver_block",
        "silver_ingot", "silver_nugget", "silver_block", "silver_sword", "silver_pickaxe",
        "silver_axe", "silver_shovel", "silver_hoe", "armor_silver_metal_helmet",
        "armor_silver_metal_chestplate", "armor_silver_metal_leggings", "armor_silver_metal_boots",
        "dragonarmor_silver_head", "dragonarmor_silver_neck", "dragonarmor_silver_body",
        "dragonarmor_silver_tail", "sapphire_ore", "sapphire_gem", "sapphire_block",
        "dragonsteel_fire_ingot", "dragonsteel_fire_block", "dragonsteel_ice_ingot",
        "dragonsteel_ice_block", "dragonsteel_lightning_ingot", "dragonsteel_lightning_block");

    private static final List<String> DISABLED_RECIPES = List.of(
        "armor_silver_metal_boots", "armor_silver_metal_chestplate", "armor_silver_metal_helmet",
        "armor_silver_metal_leggings", "furnace/deepslate_silver_ingot",
        "furnace/deepslate_silver_ingot_blasting", "furnace/sapphire", "furnace/sapphire_blasting",
        "furnace/silver_ingot", "furnace/silver_ingot_blasting", "furnace/silver_nugget",
        "furnace/silver_nugget_blasting", "raw_silver_block_to_raw_silver",
        "raw_silver_to_raw_silver_block", "sapphire_block_to_sapphire_gem",
        "sapphire_gem_to_sapphire_block", "silver_axe", "silver_block_to_silver_ingot",
        "silver_hoe", "silver_ingot_from_blasting_raw_silver",
        "silver_ingot_from_smelting_raw_silver", "silver_ingot_to_silver_block",
        "silver_ingot_to_silver_nugget", "silver_nugget_to_silver_ingot", "silver_pickaxe",
        "silver_shovel", "silver_sword", "dragonsteel_fire_block_to_dragonsteel_fire_ingot",
        "dragonsteel_fire_ingot_to_dragonsteel_fire_block", "dragonsteel_ice_block_to_dragonsteel_ice_ingot",
        "dragonsteel_ice_ingot_to_dragonsteel_ice_block",
        "dragonsteel_lightning_block_to_dragonsteel_lightning_ingot",
        "dragonsteel_lightning_ingot_to_dragonsteel_lightning_block");
    private static final List<String> BLOCK_LOOT = List.of(
        "silver_ore", "deepslate_silver_ore", "raw_silver_block", "silver_block", "sapphire_ore",
        "sapphire_block", "dragonsteel_fire_block", "dragonsteel_ice_block", "dragonsteel_lightning_block");
    private static final Map<String, String> LOOT_REPLACEMENTS = Map.of(
        "iceandfire:silver_ingot", "tfc:metal/ingot/silver",
        "iceandfire:silver_sword", "tfc:metal/sword/wrought_iron",
        "iceandfire:sapphire_gem", "tfc:gem/sapphire");
    private static final List<String> SANITIZED_LOOT = List.of(
        "chest/graveyard", "chest/mausoleum_chest", "entities/troll_frost");

    private final PackOutput.PathProvider recipes;
    private final PackOutput.PathProvider loot;
    private final PackOutput.PathProvider biomeModifiers;
    private final PackOutput.PathProvider tags;

    public DisabledContentData(PackOutput output) {
        recipes = output.createPathProvider(PackOutput.Target.DATA_PACK, "recipe");
        loot = output.createPathProvider(PackOutput.Target.DATA_PACK, "loot_table");
        biomeModifiers = output.createPathProvider(PackOutput.Target.DATA_PACK, "neoforge/biome_modifier");
        tags = output.createPathProvider(PackOutput.Target.DATA_PACK, "tags/item");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        try {
            List<CompletableFuture<?>> writes = new ArrayList<>();
            JsonObject falseRecipe = falseCondition();
            for (String id : DISABLED_RECIPES) {
                save(writes, output, recipes, "iceandfire", id, falseRecipe);
            }
            for (String id : BLOCK_LOOT) {
                JsonObject table = new JsonObject();
                table.addProperty("type", "minecraft:block");
                table.add("pools", new JsonArray());
                save(writes, output, loot, "iceandfire", "blocks/" + id, table);
            }
            for (String id : SANITIZED_LOOT) {
                JsonObject table = readIceAndFire("data/iceandfire/loot_table/" + id + ".json");
                replaceLootItems(table);
                save(writes, output, loot, "iceandfire", id, table);
            }
            for (String id : List.of("silver_ore", "sapphire_ore")) {
                JsonObject none = new JsonObject();
                none.addProperty("type", "neoforge:none");
                save(writes, output, biomeModifiers, "iceandfire", id, none);
            }
            JsonObject disabled = new JsonObject();
            disabled.addProperty("replace", false);
            JsonArray values = new JsonArray();
            DISABLED_ITEMS.forEach(id -> values.add("iceandfire:" + id));
            disabled.add("values", values);
            save(writes, output, tags, "iaftfc", "disabled", disabled);
            return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
        } catch (IOException exception) {
            return CompletableFuture.failedFuture(exception);
        }
    }

    private static JsonObject falseCondition() {
        JsonObject recipe = new JsonObject();
        JsonArray conditions = new JsonArray();
        JsonObject condition = new JsonObject();
        condition.addProperty("type", "neoforge:false");
        conditions.add(condition);
        recipe.add("neoforge:conditions", conditions);
        return recipe;
    }

    private static JsonObject readIceAndFire(String resource) throws IOException {
        Path path = ModList.get().getModFileById("iceandfire").getFile().findResource(resource);
        if (!Files.exists(path)) {
            throw new IOException("Missing Ice and Fire data: " + resource);
        }
        return JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private static void replaceLootItems(JsonElement element) {
        if (element.isJsonArray()) {
            element.getAsJsonArray().forEach(DisabledContentData::replaceLootItems);
        } else if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            if (object.has("name") && object.get("name").isJsonPrimitive()) {
                String replacement = LOOT_REPLACEMENTS.get(object.get("name").getAsString());
                if (replacement != null) {
                    object.addProperty("name", replacement);
                }
            }
            new LinkedHashMap<>(object.asMap()).values().forEach(DisabledContentData::replaceLootItems);
        }
    }

    private static void save(List<CompletableFuture<?>> writes, CachedOutput output,
        PackOutput.PathProvider provider, String namespace, String path, JsonObject json) {
        writes.add(DataProvider.saveStable(output, json,
            provider.json(ResourceLocation.fromNamespaceAndPath(namespace, path))));
    }

    @Override
    public String getName() {
        return "IaFTFC disabled Ice and Fire content";
    }
}
