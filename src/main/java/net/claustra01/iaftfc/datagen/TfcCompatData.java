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

public final class TfcCompatData implements DataProvider {
    private static final List<String> ELEMENTS = List.of("chared", "frozen", "crackled");
    private final PackOutput.PathProvider blockTags;

    public TfcCompatData(PackOutput output) {
        blockTags = output.createPathProvider(PackOutput.Target.DATA_PACK, "tags/block");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        List<CompletableFuture<?>> writes = new ArrayList<>();
        List<String> stones = blocks("stone");
        save(writes, output, "can_collapse", stones);
        save(writes, output, "can_trigger_collapse", stones);
        save(writes, output, "can_start_collapse", stones);

        List<String> looseTerrain = new ArrayList<>(blocks("cobblestone"));
        looseTerrain.addAll(blocks("gravel"));
        save(writes, output, "can_landslide", looseTerrain);
        save(writes, output, "supports_landslide", blocks("dirt_path"));
        return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
    }

    private static List<String> blocks(String terrain) {
        return ELEMENTS.stream().map(element -> "iceandfire:" + element + "_" + terrain).toList();
    }

    private void save(List<CompletableFuture<?>> writes, CachedOutput output, String name, List<String> values) {
        JsonObject tag = new JsonObject();
        tag.addProperty("replace", false);
        JsonArray entries = new JsonArray();
        values.forEach(entries::add);
        tag.add("values", entries);
        writes.add(DataProvider.saveStable(output, tag,
            blockTags.json(ResourceLocation.fromNamespaceAndPath("tfc", name))));
    }

    @Override
    public String getName() {
        return "IaFTFC TFC compatibility data";
    }
}
