package net.claustra01.iaftfc.datagen;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;

public final class NonDragonWorldgenData implements DataProvider {
    private static final List<String> STRUCTURES = List.of(
        "cyclops_cave", "gorgon_temple", "graveyard", "hydra_cave", "mausoleum", "pixie_village", "siren_island");
    private static final List<String> MOB_MODIFIERS = List.of(
        "amphithere", "cockatrice", "hippogryphs", "lich", "siren_beach_night", "troll",
        "spawn_death_worm", "spawn_hippocampus", "spawn_sea_serpent", "spawn_stymphalian_bird",
        "spawn_wandering_cyclops");

    private final PackOutput.PathProvider structureSets;
    private final PackOutput.PathProvider biomeModifiers;

    public NonDragonWorldgenData(PackOutput output) {
        structureSets = output.createPathProvider(PackOutput.Target.DATA_PACK, "worldgen/structure_set");
        biomeModifiers = output.createPathProvider(PackOutput.Target.DATA_PACK, "neoforge/biome_modifier");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        List<CompletableFuture<?>> writes = new ArrayList<>();
        try {
            for (String id : STRUCTURES) {
                var source = ModList.get().getModFileById("iceandfire").getFile()
                    .findResource("data/iceandfire/worldgen/structure_set/" + id + ".json");
                JsonObject set = JsonParser.parseString(Files.readString(source, StandardCharsets.UTF_8)).getAsJsonObject();
                set.getAsJsonObject("placement").addProperty("frequency", 0.0F);
                writes.add(DataProvider.saveStable(output, set,
                    structureSets.json(ResourceLocation.fromNamespaceAndPath("iceandfire", id))));
            }
            JsonObject none = new JsonObject();
            none.addProperty("type", "neoforge:none");
            for (String id : MOB_MODIFIERS) {
                writes.add(DataProvider.saveStable(output, none,
                    biomeModifiers.json(ResourceLocation.fromNamespaceAndPath("iceandfire", id))));
            }
        } catch (IOException exception) {
            return CompletableFuture.failedFuture(exception);
        }
        return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "IaFTFC non-dragon natural generation exclusions";
    }
}
