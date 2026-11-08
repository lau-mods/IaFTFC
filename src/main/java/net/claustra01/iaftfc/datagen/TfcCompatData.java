package net.claustra01.iaftfc.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.iafenvoy.iceandfire.data.DragonArmorPart;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.claustra01.iaftfc.dragonarmor.DragonArmor;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

public final class TfcCompatData implements DataProvider {
    private static final List<String> ELEMENTS = List.of("chared", "frozen", "crackled");
    private static final List<String> DRAGON_ELEMENTS = List.of("fire", "ice", "lightning");
    private static final List<String> SCALE_COLORS = List.of(
        "amethyst", "black", "blue", "bronze", "copper", "electric",
        "gray", "green", "red", "sapphire", "silver", "white");
    private static final List<String> PLAYER_ARMOR_PARTS = List.of(
        "helmet", "chestplate", "leggings", "boots");

    private final PackOutput.PathProvider blockTags;
    private final PackOutput.PathProvider entityTags;
    private final PackOutput.PathProvider itemTags;
    private final PackOutput.PathProvider entityDamageResistance;
    private final PackOutput.PathProvider itemDamageResistance;
    private final PackOutput.PathProvider itemSizes;

    public TfcCompatData(PackOutput output) {
        blockTags = output.createPathProvider(PackOutput.Target.DATA_PACK, "tags/block");
        entityTags = output.createPathProvider(PackOutput.Target.DATA_PACK, "tags/entity_type");
        itemTags = output.createPathProvider(PackOutput.Target.DATA_PACK, "tags/item");
        entityDamageResistance = output.createPathProvider(
            PackOutput.Target.DATA_PACK, "tfc/entity_damage_resistance");
        itemDamageResistance = output.createPathProvider(
            PackOutput.Target.DATA_PACK, "tfc/item_damage_resistance");
        itemSizes = output.createPathProvider(PackOutput.Target.DATA_PACK, "tfc/item_size");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        List<CompletableFuture<?>> writes = new ArrayList<>();
        addTerrainTags(writes, output);
        addDamageData(writes, output);
        addItemSizes(writes, output);
        return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
    }

    private void addTerrainTags(List<CompletableFuture<?>> writes, CachedOutput output) {
        List<String> stones = terrainBlocks("stone");
        saveTag(writes, output, blockTags, "tfc", "can_collapse", stones);
        saveTag(writes, output, blockTags, "tfc", "can_trigger_collapse", stones);
        saveTag(writes, output, blockTags, "tfc", "can_start_collapse", stones);

        List<String> looseTerrain = new ArrayList<>(terrainBlocks("cobblestone"));
        looseTerrain.addAll(terrainBlocks("gravel"));
        saveTag(writes, output, blockTags, "tfc", "can_landslide", looseTerrain);
        saveTag(writes, output, blockTags, "tfc", "supports_landslide", terrainBlocks("dirt_path"));
    }

    private void addDamageData(List<CompletableFuture<?>> writes, CachedOutput output) {
        List<String> dragons = DRAGON_ELEMENTS.stream()
            .map(element -> "iceandfire:" + element + "_dragon")
            .toList();
        saveTag(writes, output, entityTags, "iaftfc", "dragons", dragons);
        JsonObject dragonResistance = damage(30, 30, 30);
        dragonResistance.addProperty("entity", "iaftfc:dragons");
        save(writes, output, entityDamageResistance, "iaftfc", "dragons", dragonResistance);

        List<String> scaleArmor = new ArrayList<>();
        for (String color : SCALE_COLORS) {
            for (String part : PLAYER_ARMOR_PARTS) {
                scaleArmor.add("iceandfire:armor_" + color + "_" + part);
            }
        }
        saveResistance(writes, output, "dragon_scale_armor", scaleArmor, 45, 33, 50);

        List<String> dragonsteelArmor = new ArrayList<>();
        for (String element : DRAGON_ELEMENTS) {
            for (String part : PLAYER_ARMOR_PARTS) {
                dragonsteelArmor.add("iceandfire:dragonsteel_" + element + "_" + part);
            }
            for (DragonArmorPart part : DragonArmorPart.values()) {
                dragonsteelArmor.add("iceandfire:" + DragonArmor.id("dragon_steel_" + element, part));
            }
        }
        saveResistance(writes, output, "dragonsteel_armor", dragonsteelArmor, 70, 60, 70);

        saveDragonArmorResistance(writes, output, "steel", 30, 16.5F, 25);
        saveDragonArmorResistance(writes, output, "black_steel", 45, 33, 50);
        saveDragonArmorResistance(writes, output, "red_steel", 62.5F, 50, 50);
        saveDragonArmorResistance(writes, output, "blue_steel", 50, 50, 62.5F);

        saveTag(writes, output, itemTags, "tfc", "deals_piercing_damage",
            List.of("iceandfire:dragonbone_bow"));
    }

    private void saveDragonArmorResistance(List<CompletableFuture<?>> writes, CachedOutput output,
        String metal, float piercing, float slashing, float crushing) {
        List<String> armor = new ArrayList<>();
        for (DragonArmorPart part : DragonArmorPart.values()) {
            armor.add("iaftfc:" + DragonArmor.id(metal, part));
        }
        saveResistance(writes, output, "dragonarmor_" + metal, armor, piercing, slashing, crushing);
    }

    private void saveResistance(List<CompletableFuture<?>> writes, CachedOutput output, String name,
        List<String> items, float piercing, float slashing, float crushing) {
        JsonObject resistance = damage(piercing, slashing, crushing);
        resistance.add("ingredient", ingredients(items));
        save(writes, output, itemDamageResistance, "iaftfc", name, resistance);
    }

    private void addItemSizes(List<CompletableFuture<?>> writes, CachedOutput output) {
        List<String> eggs = new ArrayList<>();
        for (String color : SCALE_COLORS) {
            eggs.add("iceandfire:dragonegg_" + color);
        }
        saveSize(writes, output, "dragon_eggs", eggs, "very_large", "very_heavy");

        saveSize(writes, output, "dragon_skulls", DRAGON_ELEMENTS.stream()
            .map(element -> "iceandfire:dragon_skull_" + element)
            .toList(), "huge", "very_heavy");

        List<String> dragonArmor = new ArrayList<>();
        for (String metal : List.of(
            "copper", "gold", "silver", "diamond", "netherite", "iron",
            "dragon_steel_fire", "dragon_steel_ice", "dragon_steel_lightning")) {
            for (DragonArmorPart part : DragonArmorPart.values()) {
                dragonArmor.add("iceandfire:" + DragonArmor.id(metal, part));
            }
        }
        for (DragonArmor.Metal metal : DragonArmor.METALS) {
            for (DragonArmorPart part : DragonArmorPart.values()) {
                dragonArmor.add("iaftfc:" + DragonArmor.id(metal.id(), part));
            }
        }
        saveSize(writes, output, "dragon_armor", dragonArmor, "very_large", "very_heavy");

        List<String> platedSlabs = new ArrayList<>();
        for (String element : DRAGON_ELEMENTS) {
            platedSlabs.add("iaftfc:metal/block/dragonsteel_" + element + "_slab");
        }
        saveSize(writes, output, "dragonsteel_plated_slabs", platedSlabs, "small", "very_light");
    }

    private void saveSize(List<CompletableFuture<?>> writes, CachedOutput output, String name,
        List<String> items, String size, String weight) {
        JsonObject definition = new JsonObject();
        definition.add("ingredient", ingredients(items));
        definition.addProperty("size", size);
        definition.addProperty("weight", weight);
        save(writes, output, itemSizes, "iaftfc", name, definition);
    }

    private static JsonObject damage(float piercing, float slashing, float crushing) {
        JsonObject damage = new JsonObject();
        damage.addProperty("piercing", piercing);
        damage.addProperty("slashing", slashing);
        damage.addProperty("crushing", crushing);
        return damage;
    }

    private static JsonArray ingredients(List<String> values) {
        JsonArray ingredients = new JsonArray();
        for (String value : values) {
            JsonObject ingredient = new JsonObject();
            ingredient.addProperty("item", value);
            ingredients.add(ingredient);
        }
        return ingredients;
    }

    private static List<String> terrainBlocks(String terrain) {
        return ELEMENTS.stream().map(element -> "iceandfire:" + element + "_" + terrain).toList();
    }

    private static void saveTag(List<CompletableFuture<?>> writes, CachedOutput output,
        PackOutput.PathProvider provider, String namespace, String name, List<String> values) {
        JsonObject tag = new JsonObject();
        tag.addProperty("replace", false);
        JsonArray entries = new JsonArray();
        values.forEach(entries::add);
        tag.add("values", entries);
        save(writes, output, provider, namespace, name, tag);
    }

    private static void save(List<CompletableFuture<?>> writes, CachedOutput output,
        PackOutput.PathProvider provider, String namespace, String path, JsonObject json) {
        writes.add(DataProvider.saveStable(output, json,
            provider.json(ResourceLocation.fromNamespaceAndPath(namespace, path))));
    }

    @Override
    public String getName() {
        return "IaFTFC TFC compatibility data";
    }
}
