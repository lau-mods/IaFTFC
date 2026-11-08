package net.claustra01.iaftfc.datagen;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.claustra01.iaftfc.worldgen.ElementalPlacement;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

public final class ElementalPlantAssets implements DataProvider {
    private final PackOutput.PathProvider blockStates;
    private final PackOutput.PathProvider blockModels;
    private final PackOutput.PathProvider itemModels;

    public ElementalPlantAssets(PackOutput output) {
        blockStates = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "blockstates");
        blockModels = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "models/block");
        itemModels = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "models/item");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        List<CompletableFuture<?>> writes = new ArrayList<>();
        for (ElementalPlacement.Element element : ElementalPlacement.Element.values()) {
            addModels(writes, output, element.id());
        }
        return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
    }

    private void addModels(List<CompletableFuture<?>> writes, CachedOutput output, String id) {
        JsonObject blockState = new JsonObject();
        JsonObject variants = new JsonObject();
        JsonObject variant = new JsonObject();
        variant.addProperty("model", "iaftfc:block/plant/" + id + "_dynamic");
        variants.add("", variant);
        blockState.add("variants", variants);
        save(writes, output, blockStates, "iaftfc", "plant/" + id, blockState);

        for (int stage = 0; stage < 4; stage++) {
            JsonObject model = new JsonObject();
            model.addProperty("parent", "block/cross");
            JsonObject textures = new JsonObject();
            textures.addProperty("cross", "iaftfc:block/plant/" + id + "/" + id + "_" + stage);
            model.add("textures", textures);
            save(writes, output, blockModels, "iaftfc", "plant/" + id + "_" + stage, model);
        }

        JsonObject dynamic = new JsonObject();
        dynamic.addProperty("loader", "tfc:plant");
        dynamic.add("blooming", parent(id, 0));
        dynamic.add("seeding", parent(id, 1));
        dynamic.add("dying", parent(id, 2));
        dynamic.add("dormant", parent(id, 2));
        dynamic.add("sprouting", parent(id, 2));
        dynamic.add("budding", parent(id, 3));
        save(writes, output, blockModels, "iaftfc", "plant/" + id + "_dynamic", dynamic);

        JsonObject item = new JsonObject();
        item.addProperty("parent", "minecraft:item/generated");
        JsonObject textures = new JsonObject();
        textures.addProperty("layer0", "iaftfc:block/plant/" + id + "/" + id + "_0");
        item.add("textures", textures);
        save(writes, output, itemModels, "iaftfc", "plant/" + id, item);
    }

    private static JsonObject parent(String id, int stage) {
        JsonObject parent = new JsonObject();
        parent.addProperty("parent", "iaftfc:block/plant/" + id + "_" + stage);
        return parent;
    }

    private static void save(List<CompletableFuture<?>> writes, CachedOutput output, PackOutput.PathProvider path,
        String namespace, String id, JsonObject json) {
        writes.add(DataProvider.saveStable(output, json,
            path.json(ResourceLocation.fromNamespaceAndPath(namespace, id))));
    }

    @Override
    public String getName() {
        return "IaFTFC elemental plant assets";
    }
}
