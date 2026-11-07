package net.claustra01.iaftfc.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.claustra01.iaftfc.worldgen.StructureSpacingResolver;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

public final class DragonStructureSets implements DataProvider {
    private final PackOutput.PathProvider path;

    public DragonStructureSets(PackOutput output) {
        path = output.createPathProvider(PackOutput.Target.DATA_PACK, "worldgen/structure_set");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        List<CompletableFuture<?>> writes = new ArrayList<>();
        add(writes, output, "iceandfire", "dragon_roost", "fire_dragon_roost", 24, 10,
            StructureSpacingResolver.FIRE_ROOST_SALT);
        add(writes, output, "iaftfc", "ice_dragon_roost", "ice_dragon_roost", 24, 10,
            StructureSpacingResolver.ICE_ROOST_SALT);
        add(writes, output, "iaftfc", "lightning_dragon_roost", "lightning_dragon_roost", 24, 10,
            StructureSpacingResolver.LIGHTNING_ROOST_SALT);
        add(writes, output, "iceandfire", "dragon_cave", "fire_dragon_cave", 30, 20,
            StructureSpacingResolver.FIRE_CAVE_SALT);
        add(writes, output, "iaftfc", "ice_dragon_cave", "ice_dragon_cave", 30, 20,
            StructureSpacingResolver.ICE_CAVE_SALT);
        add(writes, output, "iaftfc", "lightning_dragon_cave", "lightning_dragon_cave", 30, 20,
            StructureSpacingResolver.LIGHTNING_CAVE_SALT);
        return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
    }

    private void add(List<CompletableFuture<?>> writes, CachedOutput output, String namespace, String setName,
        String structureName, int spacing, int separation, int salt) {
        JsonObject root = new JsonObject();
        JsonArray structures = new JsonArray();
        JsonObject entry = new JsonObject();
        entry.addProperty("structure", "iceandfire:" + structureName);
        entry.addProperty("weight", 1);
        structures.add(entry);
        root.add("structures", structures);

        JsonObject placement = new JsonObject();
        placement.addProperty("type", "minecraft:random_spread");
        placement.addProperty("salt", salt);
        placement.addProperty("spacing", spacing);
        placement.addProperty("separation", separation);
        placement.addProperty("spread_type", "linear");
        JsonObject exclusion = new JsonObject();
        exclusion.addProperty("other_set", "minecraft:villages");
        exclusion.addProperty("chunk_count", 10);
        placement.add("exclusion_zone", exclusion);
        root.add("placement", placement);

        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(namespace, setName);
        writes.add(DataProvider.saveStable(output, root, path.json(id)));
    }

    @Override
    public String getName() {
        return "IaFTFC dragon structure sets";
    }
}
