package net.claustra01.iaftfc.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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

public final class ElementalWorldgenData implements DataProvider {
    private static final String[] ELEMENTS = {"fire", "ice", "lightning"};
    private final PackOutput output;

    public ElementalWorldgenData(PackOutput output) {
        this.output = output;
    }

    @Override
    public String getName() {
        return "IaFTFC elemental worldgen";
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
        JsonObject none = new JsonObject();
        none.addProperty("type", "neoforge:none");
        for (String ore : List.of("silver", "sapphire")) {
            write(cache, writes, "data/iceandfire/neoforge/biome_modifier/" + ore + "_ore.json", none);
        }

        for (String element : ELEMENTS) {
            String lily = element.equals("ice") ? "frost_lily" : element + "_lily";
            JsonObject configured = readIaf("data/iceandfire/worldgen/configured_feature/" + lily + ".json");
            JsonObject candidate = new JsonObject();
            candidate.addProperty("type", "iaftfc:climate");
            candidate.addProperty("element", element);
            candidate.addProperty("lily", true);
            configured.getAsJsonObject("config").getAsJsonObject("feature")
                .getAsJsonArray("placement").add(candidate);
            write(cache, writes, "data/iceandfire/worldgen/configured_feature/" + lily + ".json", configured);
            for (String feature : List.of(lily, "spawn_dragon_skeleton_" + element)) {
                JsonObject modifier = readIaf("data/iceandfire/neoforge/biome_modifier/" + feature + ".json");
                String originalTag = modifier.get("biomes").getAsString();
                if (!originalTag.startsWith("#iceandfire:"))
                    throw new IOException("Expected IaF biome tag for " + feature + ": " + originalTag);
                JsonObject tag = new JsonObject();
                JsonArray values = new JsonArray();
                values.add("#iaftfc:dragon_land");
                tag.add("values", values);
                if (feature.equals(lily)) write(cache, writes, "data/iceandfire/tags/worldgen/biome/"
                    + originalTag.substring("#iceandfire:".length()) + ".json", tag);

                JsonObject placed = readIaf("data/iceandfire/worldgen/placed_feature/" + feature + ".json");
                JsonObject climate = new JsonObject();
                climate.addProperty("type", "iaftfc:climate");
                climate.addProperty("element", element);
                climate.addProperty("lily", false);
                placed.getAsJsonArray("placement").add(climate);
                write(cache, writes, "data/iceandfire/worldgen/placed_feature/" + feature + ".json", placed);
            }
        }

        ground(cache, writes, "fire", "#minecraft:sand", "#minecraft:dirt");
        ground(cache, writes, "ice", "#minecraft:snow", "#minecraft:ice", "#minecraft:dirt");
        ground(cache, writes, "lightning", "#minecraft:dirt");
    }

    private void ground(CachedOutput cache, List<CompletableFuture<?>> writes, String element, String... values) {
        JsonArray entries = new JsonArray();
        for (String value : values) entries.add(value);
        JsonObject tag = new JsonObject();
        tag.add("values", entries);
        write(cache, writes, "data/iaftfc/tags/block/lily_ground/" + element + ".json", tag);
    }

    private static Path modPath(String mod, String path) throws IOException {
        Path resource = ModList.get().getModFileById(mod).getFile().findResource(path);
        if (!Files.exists(resource)) throw new IOException("Missing " + mod + " data: " + path);
        return resource;
    }

    private static JsonObject readIaf(String path) throws IOException {
        return JsonParser.parseString(Files.readString(modPath("iceandfire", path), StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private void write(CachedOutput cache, List<CompletableFuture<?>> writes, String path, JsonObject value) {
        writes.add(DataProvider.saveStable(cache, value, output.getOutputFolder().resolve(path)));
    }
}
