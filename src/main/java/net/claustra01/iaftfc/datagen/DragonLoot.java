package net.claustra01.iaftfc.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;

public final class DragonLoot implements DataProvider {
    private static final Map<String, String> CHEST_HASHES = Map.of(
        "fire_dragon_roost", "2ebcbf58bdd12c9adaa51ca5b4dd39577801194d52580a8a187bff0a9a8e0671",
        "fire_dragon_female_cave", "2ebcbf58bdd12c9adaa51ca5b4dd39577801194d52580a8a187bff0a9a8e0671",
        "fire_dragon_male_cave", "06635d3836d3ca5071ef48fba432b9b3d9076577ea20306abab541db3e8c3955",
        "ice_dragon_roost", "be1c49f836724bdfbcce645f2876bf9e653677f2eda7ffbc53f7708ba6b3a2e9",
        "ice_dragon_female_cave", "be1c49f836724bdfbcce645f2876bf9e653677f2eda7ffbc53f7708ba6b3a2e9",
        "ice_dragon_male_cave", "db0448b3048c5100c93c5bae0d358c8b9d89a6df00e5fb44435f41dc573d0884",
        "lightning_dragon_roost", "58978542aae2fd0b5af802390e13fd3cf02d39b6ac325ab678cb5ff7ba6e3a18",
        "lightning_dragon_female_cave", "58978542aae2fd0b5af802390e13fd3cf02d39b6ac325ab678cb5ff7ba6e3a18",
        "lightning_dragon_male_cave", "576e370c7bd3dd16d48e030a6671cc7f0e703473c151f859f1a07a4b3070433f"
    );
    private static final Map<String, String> PILE_HASHES = Map.of(
        "gold", "404c4f644564337d42ccb8b4419686d45979100177401326580c0c988e735cc5",
        "silver", "087e63e890183b10b0c323ddd604f9b4ce22edbc253eff25cec8bc0de8261105",
        "copper", "e3c4ebc07e73c09ac8df43e56f88f8323e9e7996846a0aa765b2d69b715da876"
    );
    private static final String[] GEMS = {"amethyst", "diamond", "emerald", "lapis_lazuli", "opal", "pyrite", "ruby", "sapphire", "topaz"};
    private static final String[] PROFILES = {"ordinary", "dangerous", "lategame"};
    private static final String[][] INGOTS = {
        {"copper", "bronze", "bismuth_bronze", "black_bronze", "wrought_iron"},
        {"wrought_iron", "steel"}, {"steel", "black_steel"}
    };
    private static final String[][] EQUIPMENT_METALS = {
        {"copper", "bronze", "bismuth_bronze", "black_bronze"}, {"wrought_iron"}, {"steel"}
    };
    private static final Map<String, String[]> TOOLS = Map.of(
        "pickaxe", new String[] {"pickaxe", "propick"},
        "axe", new String[] {"axe", "saw"},
        "hoe", new String[] {"hoe", "scythe"},
        "sword", new String[] {"sword", "javelin", "mace"},
        "shovel", new String[] {"shovel"},
        "knife", new String[] {"knife"},
        "shears", new String[] {"shears"},
        "shield", new String[] {"shield"},
        "bow", new String[] {"bow"},
        "crossbow", new String[] {"crossbow"}
    );

    private final PackOutput output;

    public DragonLoot(PackOutput output) {
        this.output = output;
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

    @Override
    public String getName() {
        return "IaFTFC dragon loot";
    }

    private void generate(CachedOutput cache, List<CompletableFuture<?>> writes) throws IOException {
        for (String element : List.of("fire", "ice", "lightning")) {
            for (String place : List.of("roost", "female_cave", "male_cave")) {
                String name = element + "_dragon_" + place;
                JsonObject table = original("chest/" + name, CHEST_HASHES.get(name));
                for (var pool : table.getAsJsonArray("pools")) {
                    for (var entry : pool.getAsJsonObject().getAsJsonArray("entries")) {
                        translate(entry.getAsJsonObject(), element, place);
                    }
                }
                write(cache, writes, "iceandfire", "chest/" + name, table);
            }
        }
        for (String metal : List.of("gold", "silver", "copper")) {
            JsonObject pile = original("blocks/" + metal + "_pile", PILE_HASHES.get(metal));
            for (var pool : pile.getAsJsonArray("pools")) {
                for (var entry : pool.getAsJsonObject().getAsJsonArray("entries")) {
                    entry.getAsJsonObject().addProperty("name", item("tfc:ore/small_native_" + metal));
                }
            }
            write(cache, writes, "iceandfire", "blocks/" + metal + "_pile", pile);
        }
        write(cache, writes, "iaftfc", "shared/gems", choices(List.of(GEMS).stream().map(gem -> "tfc:gem/" + gem).toList()));
        write(cache, writes, "iaftfc", "shared/coal_like", choices(List.of("minecraft:charcoal", "tfc:ore/bituminous_coal", "tfc:ore/lignite", "tfc:ore/graphite")));
        write(cache, writes, "iaftfc", "shared/utility", choices(List.of("tfc:powder/flux", "minecraft:glass", "minecraft:glass_pane",
            "tfc:kaolin_clay", "tfc:powder/wood_ash", "tfc:powder/saltpeter", "tfc:powder/sylvite")));
        for (int i = 0; i < PROFILES.length; i++) {
            String profile = PROFILES[i];
            List<String> ingots = new ArrayList<>();
            for (String metal : INGOTS[i]) ingots.add("tfc:metal/ingot/" + metal);
            write(cache, writes, "iaftfc", "shared/ingots/" + profile, choices(ingots));
            for (String family : TOOLS.keySet().stream().sorted().toList()) {
                write(cache, writes, "iaftfc", "shared/equipment/" + profile + "/" + family,
                    equipment(EQUIPMENT_METALS[i], family));
            }
            for (String part : List.of("helmet", "chestplate", "leggings", "boots")) {
                write(cache, writes, "iaftfc", "shared/equipment/" + profile + "/" + part,
                    equipment(EQUIPMENT_METALS[i], part));
            }
        }
    }

    private void translate(JsonObject entry, String element, String place) {
        String name = entry.get("name").getAsString();
        String profile = place.equals("roost") ? "ordinary" : place.equals("female_cave") ? "dangerous" : "lategame";
        String gem = switch (element) {
            case "fire" -> "minecraft:emerald";
            case "ice" -> "iceandfire:sapphire_gem";
            default -> "minecraft:amethyst_shard";
        };
        String metal = switch (element) {
            case "fire" -> "gold";
            case "ice" -> "silver";
            default -> "copper";
        };
        if (name.equals(gem)) {
            entry.addProperty("name", item("tfc:gem/" + switch (element) {
                case "fire" -> "ruby";
                case "ice" -> "sapphire";
                default -> "topaz";
            }));
        } else if (name.equals("minecraft:diamond")) {
            reference(entry, "shared/gems");
        } else if (name.equals("minecraft:iron_ingot") || name.equals("minecraft:gold_ingot") && !element.equals("fire")) {
            reference(entry, "shared/ingots/" + profile);
        } else if (name.equals(switch (metal) {
            case "gold" -> "minecraft:gold_ingot";
            case "silver" -> "iceandfire:silver_ingot";
            default -> "minecraft:copper_ingot";
        })) {
            entry.addProperty("name", item("tfc:metal/ingot/" + metal));
        } else if (name.endsWith("_nugget")) {
            String ore = name.contains("silver") ? "silver" : name.contains("copper") ? "copper" : "gold";
            entry.addProperty("name", item("tfc:ore/small_native_" + ore));
        } else if (name.equals("minecraft:coal")) {
            reference(entry, "shared/coal_like");
        } else if (List.of("minecraft:chain", "minecraft:cobweb", "minecraft:obsidian", "minecraft:packed_ice", "minecraft:flint").contains(name)) {
            reference(entry, "shared/utility");
        } else if (name.endsWith("_sword") && !name.contains("dragon")) {
            reference(entry, "shared/equipment/" + profile + "/sword");
            enchant(entry, name);
        } else {
            for (String part : List.of("helmet", "chestplate", "leggings", "boots")) {
                if (name.endsWith("_" + part)) {
                    reference(entry, "shared/equipment/" + profile + "/" + part);
                    enchant(entry, name);
                    break;
                }
            }
        }
    }

    private static void enchant(JsonObject entry, String source) {
        if (!entry.has("functions")) return;
        for (var function : entry.getAsJsonArray("functions")) {
            JsonObject data = function.getAsJsonObject();
            if (data.get("function").getAsString().equals("enchant_with_levels")) {
                data.addProperty("function", "iaftfc:enchant_equipment");
                data.addProperty("source", source);
            }
        }
    }

    private static void reference(JsonObject entry, String path) {
        entry.addProperty("type", "minecraft:loot_table");
        entry.remove("name");
        entry.addProperty("value", "iaftfc:" + path);
    }

    private static JsonObject choices(List<String> names) {
        JsonArray entries = new JsonArray();
        for (String name : names) entries.add(single(name, 1));
        return table(entries);
    }

    private static JsonObject equipment(String[] metals, String family) {
        JsonArray entries = new JsonArray();
        String[] kinds = TOOLS.get(family);
        if (family.equals("bow") || family.equals("crossbow")) {
            entries.add(single("minecraft:" + family, 1));
            return table(entries);
        }
        for (String metal : metals) {
            if (kinds == null) {
                String part = family.equals("leggings") ? "greaves" : family;
                entries.add(single("tfc:metal/unfinished_" + part + "/" + metal, 2));
                entries.add(single("tfc:metal/" + part + "/" + metal, 1));
            } else {
                for (String kind : kinds) {
                    String blade = kind.equals("sword") || kind.equals("saw") || kind.equals("scythe") || kind.equals("knife") ? "blade" : "head";
                    if (!List.of("shield", "shears").contains(kind)) {
                        entries.add(single("tfc:metal/" + kind + "_" + blade + "/" + metal, 4));
                    }
                    entries.add(single("tfc:metal/" + kind + "/" + metal, List.of("shield", "shears").contains(kind) ? 1 : 2));
                }
            }
        }
        return table(entries);
    }

    private static JsonObject single(String name, int weight) {
        JsonObject entry = new JsonObject();
        entry.addProperty("type", "minecraft:item");
        entry.addProperty("name", item(name));
        entry.addProperty("weight", weight);
        return entry;
    }

    private static JsonObject table(JsonArray entries) {
        JsonObject pool = new JsonObject();
        pool.addProperty("rolls", 1);
        pool.add("entries", entries);
        JsonArray pools = new JsonArray();
        pools.add(pool);
        JsonObject table = new JsonObject();
        table.addProperty("type", "minecraft:chest");
        table.add("pools", pools);
        return table;
    }

    private static String item(String id) {
        ResourceLocation key = ResourceLocation.parse(id);
        if (!BuiltInRegistries.ITEM.containsKey(key)) throw new IllegalStateException("Missing loot item: " + id);
        return id;
    }

    private static JsonObject original(String path, String expectedHash) throws IOException {
        Path resource = ModList.get().getModFileById("iceandfire").getFile()
            .findResource("data/iceandfire/loot_table/" + path + ".json");
        byte[] bytes = Files.readAllBytes(resource);
        try {
            String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
            if (!hash.equals(expectedHash)) throw new IllegalStateException("IaF loot input changed: " + path + " " + hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
        return JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private void write(CachedOutput cache, List<CompletableFuture<?>> writes, String namespace, String path, JsonObject data) {
        Path file = output.getOutputFolder().resolve("data").resolve(namespace).resolve("loot_table").resolve(path + ".json");
        writes.add(DataProvider.saveStable(cache, data, file));
    }
}
