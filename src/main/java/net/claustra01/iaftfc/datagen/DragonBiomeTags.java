package net.claustra01.iaftfc.datagen;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.BiomeTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public final class DragonBiomeTags extends BiomeTagsProvider {
    private static final TagKey<Biome> DRAGON_LAND = biomeTag("iaftfc", "dragon_land");

    public DragonBiomeTags(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup,
        ExistingFileHelper files) {
        super(output, lookup, "iaftfc", files);
    }

    @Override
    protected void addTags(HolderLookup.Provider lookup) {
        String[] landBiomes = {
            "active_shield_volcano", "ancient_shield_volcano", "badlands", "burren_badlands",
            "burren_badlands_tall", "burren_plains", "burren_plateau", "burren_roche_moutonee",
            "buttes", "canyons", "cenote_canyons", "cenote_highlands", "cenote_hills",
            "cenote_plains", "cenote_plateau", "cenote_rolling_hills", "collisional_mountains",
            "doline_canyons", "doline_highlands", "doline_hills", "doline_plains", "doline_plateau",
            "doline_rolling_hills", "dormant_shield_volcano", "drumlins", "dune_sea",
            "extinct_shield_volcano", "extreme_doline_mountains", "extreme_doline_plateau",
            "glacially_carved_mountains", "glacially_carved_volcanic_mountains", "glaciated_mountains",
            "glaciated_shield_volcano", "glaciated_volcanic_mountains", "grassy_dunes", "highlands",
            "hills", "hoodoos", "ice_sheet", "ice_sheet_edge", "ice_sheet_mountains",
            "ice_sheet_mountains_edge", "ice_sheet_shield_volcano", "ice_sheet_tuyas",
            "ice_sheet_tuyas_edge", "ice_sheet_volcanic_mountains", "inverted_patterned_ground",
            "knob_and_kettle", "low_canyons", "lowlands", "mesas", "mountains", "mud_flats",
            "old_mountains", "patterned_ground", "plains", "plateau", "plateau_wide", "rift_valley",
            "rocky_plateau", "rolling_hills", "salt_flats", "salt_marsh", "shilin_canyons",
            "shilin_highlands", "shilin_hills", "shilin_plains", "shilin_plateau",
            "stair_step_canyons", "stone_circles", "tower_karst_canyons", "tower_karst_highlands",
            "tower_karst_hills", "tower_karst_plains", "tuyas", "volcanic_mountains",
            "whorled_canyons"
        };
        for (String biome : landBiomes) {
            tag(DRAGON_LAND).add(ResourceKey.create(Registries.BIOME,
                ResourceLocation.fromNamespaceAndPath("tfc", biome)));
        }
        for (String element : new String[] {"fire", "ice", "lightning"}) {
            tag(biomeTag("iceandfire", "structure_gen/" + element)).addTag(DRAGON_LAND);
        }
    }

    private static TagKey<Biome> biomeTag(String namespace, String path) {
        return TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(namespace, path));
    }
}
