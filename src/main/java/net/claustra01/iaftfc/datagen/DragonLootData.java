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

public final class DragonLootData implements DataProvider {
    private static final List<String> ORDINARY = List.of("copper", "bronze", "bismuth_bronze", "black_bronze");
    private static final List<String> DANGEROUS = List.of("wrought_iron");
    private static final List<String> LATEGAME = List.of("steel");
    private static final List<String> EQUIPMENT = List.of("sword", "helmet", "chestplate", "greaves", "boots");
    private final PackOutput.PathProvider path;

    public DragonLootData(PackOutput output) {
        path = output.createPathProvider(PackOutput.Target.DATA_PACK, "loot_table");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        List<CompletableFuture<?>> writes = new ArrayList<>();
        addSharedEquipment(writes, output, "ordinary", ORDINARY, false);
        addSharedEquipment(writes, output, "dangerous", DANGEROUS, false);
        addSharedEquipment(writes, output, "lategame", LATEGAME, true);
        for (Element element : Element.values()) {
            addChest(writes, output, element, "roost", "ordinary", ORDINARY, false);
            addChest(writes, output, element, "female_cave", "dangerous", DANGEROUS, false);
            addChest(writes, output, element, "male_cave", "lategame", LATEGAME, true);
        }
        addPile(writes, output, "gold", "native_gold");
        addPile(writes, output, "silver", "native_silver");
        addPile(writes, output, "copper", "native_copper");
        return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
    }

    private void addChest(List<CompletableFuture<?>> writes, CachedOutput output, Element element, String variant,
        String profileName, List<String> profile, boolean male) {
        JsonObject root = new JsonObject();
        JsonArray pools = new JsonArray();
        JsonObject pool = new JsonObject();
        pool.addProperty("name", element.pool(male));
        pool.add("rolls", uniform(1, male ? 6 : 4));
        JsonArray entries = new JsonArray();

        if (male) {
            entries.add(item("minecraft:book", 12, enchant(30)));
        }
        entries.add(item("tfc:gem/diamond", 2, count(uniform(1, 4))));
        entries.add(item(element.gem, male ? 3 : element.gemWeight, count(uniform(1, 4))));
        addProfileIngots(entries, profile, 20, male ? uniform(1, 25) : constant(10), element);
        entries.add(item("minecraft:bone", 20, count(male ? uniform(1, 25) : constant(10))));
        if (!male) {
            entries.add(item("minecraft:book", 10, count(uniform(1, 2))));
        }
        entries.add(item("tfc:metal/chain/wrought_iron", 20, count(uniform(1, 2))));
        entries.add(item("iceandfire:manuscript", 20, count(uniform(1, 6))));
        if (!profile.contains(element.metal)) {
            entries.add(item(element.ingot, male ? 20 : element.ingotWeight,
                count(male ? uniform(1, 25) : constant(10))));
        }
        entries.add(item(element.smallOre, male ? 30 : element.smallOreWeight, count(constant(16))));
        entries.add(item("minecraft:skeleton_skull", male ? 1 : 3, count(constant(1))));
        entries.add(item("minecraft:cobweb", male ? 5 : 15, count(uniform(1, 6))));
        addEquipment(entries, profileName, male ? 10 : 5);
        entries.add(item(male ? "minecraft:obsidian" : element.environment, 7,
            count(uniform(1, male ? 6 : element.environmentMax))));
        if (male) {
            entries.add(item(element == Element.FIRE ? "minecraft:golden_apple"
                : "minecraft:enchanted_golden_apple", 1));
        }
        entries.add(item("minecraft:experience_bottle", 1, count(uniform(1, 5))));
        if (element == Element.LIGHTNING) {
            entries.add(item("minecraft:flint", male ? 13 : 16, count(uniform(1, male ? 6 : 5))));
            entries.add(item("iceandfire:cannoli", 1, male ? null : count(constant(1))));
        }

        pool.add("entries", entries);
        pools.add(pool);
        root.add("pools", pools);
        ResourceLocation id = ResourceLocation.parse("iceandfire:chest/" + element.id + "_dragon_" + variant);
        writes.add(DataProvider.saveStable(output, root, path.json(id)));
    }

    private void addProfileIngots(JsonArray entries, List<String> profile, int totalWeight, JsonObject amount,
        Element element) {
        int weight = totalWeight / profile.size();
        for (String metal : profile) {
            entries.add(item("tfc:metal/ingot/" + metal,
                weight + (metal.equals(element.metal) ? element.ingotWeight : 0), count(amount)));
        }
    }

    private void addEquipment(JsonArray entries, String profile, int weight) {
        for (String equipment : EQUIPMENT) {
            entries.add(table("iaftfc:shared/equipment/" + profile + "/" + equipment, weight,
                count(constant(1))));
        }
    }

    private void addSharedEquipment(List<CompletableFuture<?>> writes, CachedOutput output, String profile,
        List<String> metals, boolean enchanted) {
        for (String equipment : EQUIPMENT) {
            JsonObject root = new JsonObject();
            root.addProperty("type", "minecraft:generic");
            JsonObject pool = new JsonObject();
            pool.addProperty("rolls", 1);
            JsonArray entries = new JsonArray();
            for (String metal : metals) {
                String unfinished = equipment.equals("sword") ? "sword_blade" : "unfinished_" + equipment;
                entries.add(item("tfc:metal/" + unfinished + "/" + metal, 2));
                JsonArray functions = new JsonArray();
                if (enchanted) {
                    functions.add(enchant(25));
                }
                entries.add(item("tfc:metal/" + equipment + "/" + metal, 1, functions));
            }
            pool.add("entries", entries);
            JsonArray pools = new JsonArray();
            pools.add(pool);
            root.add("pools", pools);
            ResourceLocation id = ResourceLocation.parse("iaftfc:shared/equipment/" + profile + "/" + equipment);
            writes.add(DataProvider.saveStable(output, root, path.json(id)));
        }
    }

    private void addPile(List<CompletableFuture<?>> writes, CachedOutput output, String metal, String ore) {
        JsonObject root = new JsonObject();
        root.addProperty("type", "minecraft:block");
        JsonArray pools = new JsonArray();
        JsonObject pool = new JsonObject();
        pool.addProperty("name", metal + "_pile");
        pool.addProperty("rolls", 1);
        JsonArray entries = new JsonArray();
        for (int layers = 1; layers <= 8; layers++) {
            JsonObject entry = item("tfc:ore/small_" + ore);
            JsonObject count = new JsonObject();
            count.addProperty("function", "minecraft:set_count");
            count.addProperty("count", layers);
            JsonArray functions = new JsonArray();
            functions.add(count);
            entry.add("functions", functions);
            JsonArray conditions = new JsonArray();
            JsonObject condition = new JsonObject();
            condition.addProperty("condition", "minecraft:block_state_property");
            condition.addProperty("block", "iceandfire:" + metal + "_pile");
            JsonObject properties = new JsonObject();
            properties.addProperty("layers", Integer.toString(layers));
            condition.add("properties", properties);
            conditions.add(condition);
            entry.add("conditions", conditions);
            entries.add(entry);
        }
        pool.add("entries", entries);
        pools.add(pool);
        root.add("pools", pools);
        ResourceLocation id = ResourceLocation.parse("iceandfire:blocks/" + metal + "_pile");
        writes.add(DataProvider.saveStable(output, root, path.json(id)));
    }

    private static JsonObject item(String name, int weight, JsonArray functions) {
        JsonObject entry = item(name, weight);
        if (functions != null && !functions.isEmpty()) {
            entry.add("functions", functions);
        }
        return entry;
    }

    private static JsonObject item(String name, int weight, JsonObject function) {
        JsonArray functions = new JsonArray();
        functions.add(function);
        return item(name, weight, functions);
    }

    private static JsonObject item(String name, int weight) {
        JsonObject entry = item(name);
        entry.addProperty("weight", weight);
        return entry;
    }

    private static JsonObject item(String name) {
        JsonObject entry = new JsonObject();
        entry.addProperty("type", "minecraft:item");
        entry.addProperty("name", name);
        return entry;
    }

    private static JsonObject table(String value, int weight, JsonArray functions) {
        JsonObject entry = new JsonObject();
        entry.addProperty("type", "minecraft:loot_table");
        entry.addProperty("value", value);
        entry.addProperty("weight", weight);
        entry.add("functions", functions);
        return entry;
    }

    private static JsonArray count(JsonObject amount) {
        JsonArray functions = new JsonArray();
        JsonObject function = new JsonObject();
        function.addProperty("function", "minecraft:set_count");
        function.add("count", amount);
        functions.add(function);
        return functions;
    }

    private static JsonObject enchant(int levels) {
        JsonObject function = new JsonObject();
        function.addProperty("function", "minecraft:enchant_with_levels");
        function.addProperty("levels", levels);
        function.addProperty("treasure", true);
        return function;
    }

    private static JsonObject constant(int value) {
        JsonObject number = new JsonObject();
        number.addProperty("type", "minecraft:constant");
        number.addProperty("value", value);
        return number;
    }

    private static JsonObject uniform(int min, int max) {
        JsonObject number = new JsonObject();
        number.addProperty("type", "minecraft:uniform");
        number.addProperty("min", min);
        number.addProperty("max", max);
        return number;
    }

    @Override
    public String getName() {
        return "IaFTFC dragon structure loot";
    }

    private enum Element {
        FIRE("fire", "tfc:gem/ruby", 3, "gold", "tfc:metal/ingot/gold", "tfc:ore/small_native_gold",
            "minecraft:obsidian", 7, 12, 6),
        ICE("ice", "tfc:gem/sapphire", 7, "silver", "tfc:metal/ingot/silver", "tfc:ore/small_native_silver",
            "minecraft:packed_ice", 10, 15, 6),
        LIGHTNING("lightning", "tfc:gem/topaz", 7, "copper", "tfc:metal/ingot/copper",
            "tfc:ore/small_native_copper", "iceandfire:crackled_gravel", 10, 15, 2);

        private final String id;
        private final String gem;
        private final int gemWeight;
        private final String metal;
        private final String ingot;
        private final String smallOre;
        private final String environment;
        private final int ingotWeight;
        private final int smallOreWeight;
        private final int environmentMax;

        Element(String id, String gem, int gemWeight, String metal, String ingot, String smallOre, String environment,
            int ingotWeight, int smallOreWeight, int environmentMax) {
            this.id = id;
            this.gem = gem;
            this.gemWeight = gemWeight;
            this.metal = metal;
            this.ingot = ingot;
            this.smallOre = smallOre;
            this.environment = environment;
            this.ingotWeight = ingotWeight;
            this.smallOreWeight = smallOreWeight;
            this.environmentMax = environmentMax;
        }

        private String pool(boolean male) {
            if (this == LIGHTNING) {
                return male ? "lightning_dragon_cave" : "lightning_dragon_cave_female";
            }
            return id + "_dragon_cave";
        }
    }
}
