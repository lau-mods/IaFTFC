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
    private final PackOutput.PathProvider path;

    public DragonLootData(PackOutput output) {
        path = output.createPathProvider(PackOutput.Target.DATA_PACK, "loot_table");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        List<CompletableFuture<?>> writes = new ArrayList<>();
        addPile(writes, output, "gold", "native_gold");
        addPile(writes, output, "silver", "native_silver");
        addPile(writes, output, "copper", "native_copper");
        return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
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
            count.addProperty("count", (layers + 1) / 2);
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

    private static JsonObject item(String name) {
        JsonObject entry = new JsonObject();
        entry.addProperty("type", "minecraft:item");
        entry.addProperty("name", name);
        return entry;
    }

    @Override
    public String getName() {
        return "IaFTFC dragon pile loot";
    }
}
