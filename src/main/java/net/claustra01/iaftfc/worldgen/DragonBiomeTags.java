package net.claustra01.iaftfc.worldgen;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.BiomeTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public final class DragonBiomeTags extends BiomeTagsProvider {
    public DragonBiomeTags(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup,
        ExistingFileHelper files) {
        super(output, lookup, "iaftfc", files);
    }

    @Override
    protected void addTags(HolderLookup.Provider lookup) {
        TagKey<Biome> land = TagKey.create(Registries.BIOME, ResourceLocation.parse("iaftfc:dragon_land"));
        tag(land).addOptional(ResourceLocation.parse("tfc:active_shield_volcano"));
        tag(land).addOptional(ResourceLocation.parse("tfc:ancient_shield_volcano"));
        tag(land).addOptional(ResourceLocation.parse("tfc:badlands"));
        tag(land).addOptional(ResourceLocation.parse("tfc:burren_badlands"));
        tag(land).addOptional(ResourceLocation.parse("tfc:burren_badlands_tall"));
        tag(land).addOptional(ResourceLocation.parse("tfc:burren_plains"));
        tag(land).addOptional(ResourceLocation.parse("tfc:burren_plateau"));
        tag(land).addOptional(ResourceLocation.parse("tfc:burren_roche_moutonee"));
        tag(land).addOptional(ResourceLocation.parse("tfc:buttes"));
        tag(land).addOptional(ResourceLocation.parse("tfc:canyons"));
        tag(land).addOptional(ResourceLocation.parse("tfc:cenote_canyons"));
        tag(land).addOptional(ResourceLocation.parse("tfc:cenote_highlands"));
        tag(land).addOptional(ResourceLocation.parse("tfc:cenote_hills"));
        tag(land).addOptional(ResourceLocation.parse("tfc:cenote_plains"));
        tag(land).addOptional(ResourceLocation.parse("tfc:cenote_plateau"));
        tag(land).addOptional(ResourceLocation.parse("tfc:cenote_rolling_hills"));
        tag(land).addOptional(ResourceLocation.parse("tfc:collisional_mountains"));
        tag(land).addOptional(ResourceLocation.parse("tfc:doline_canyons"));
        tag(land).addOptional(ResourceLocation.parse("tfc:doline_highlands"));
        tag(land).addOptional(ResourceLocation.parse("tfc:doline_hills"));
        tag(land).addOptional(ResourceLocation.parse("tfc:doline_plains"));
        tag(land).addOptional(ResourceLocation.parse("tfc:doline_plateau"));
        tag(land).addOptional(ResourceLocation.parse("tfc:doline_rolling_hills"));
        tag(land).addOptional(ResourceLocation.parse("tfc:dormant_shield_volcano"));
        tag(land).addOptional(ResourceLocation.parse("tfc:drumlins"));
        tag(land).addOptional(ResourceLocation.parse("tfc:dune_sea"));
        tag(land).addOptional(ResourceLocation.parse("tfc:extinct_shield_volcano"));
        tag(land).addOptional(ResourceLocation.parse("tfc:extreme_doline_mountains"));
        tag(land).addOptional(ResourceLocation.parse("tfc:extreme_doline_plateau"));
        tag(land).addOptional(ResourceLocation.parse("tfc:glacially_carved_mountains"));
        tag(land).addOptional(ResourceLocation.parse("tfc:glacially_carved_volcanic_mountains"));
        tag(land).addOptional(ResourceLocation.parse("tfc:glaciated_mountains"));
        tag(land).addOptional(ResourceLocation.parse("tfc:glaciated_shield_volcano"));
        tag(land).addOptional(ResourceLocation.parse("tfc:glaciated_volcanic_mountains"));
        tag(land).addOptional(ResourceLocation.parse("tfc:grassy_dunes"));
        tag(land).addOptional(ResourceLocation.parse("tfc:highlands"));
        tag(land).addOptional(ResourceLocation.parse("tfc:hills"));
        tag(land).addOptional(ResourceLocation.parse("tfc:hoodoos"));
        tag(land).addOptional(ResourceLocation.parse("tfc:ice_sheet"));
        tag(land).addOptional(ResourceLocation.parse("tfc:ice_sheet_edge"));
        tag(land).addOptional(ResourceLocation.parse("tfc:ice_sheet_mountains"));
        tag(land).addOptional(ResourceLocation.parse("tfc:ice_sheet_mountains_edge"));
        tag(land).addOptional(ResourceLocation.parse("tfc:ice_sheet_shield_volcano"));
        tag(land).addOptional(ResourceLocation.parse("tfc:ice_sheet_tuyas"));
        tag(land).addOptional(ResourceLocation.parse("tfc:ice_sheet_tuyas_edge"));
        tag(land).addOptional(ResourceLocation.parse("tfc:ice_sheet_volcanic_mountains"));
        tag(land).addOptional(ResourceLocation.parse("tfc:inverted_patterned_ground"));
        tag(land).addOptional(ResourceLocation.parse("tfc:knob_and_kettle"));
        tag(land).addOptional(ResourceLocation.parse("tfc:low_canyons"));
        tag(land).addOptional(ResourceLocation.parse("tfc:lowlands"));
        tag(land).addOptional(ResourceLocation.parse("tfc:mesas"));
        tag(land).addOptional(ResourceLocation.parse("tfc:mountains"));
        tag(land).addOptional(ResourceLocation.parse("tfc:mud_flats"));
        tag(land).addOptional(ResourceLocation.parse("tfc:old_mountains"));
        tag(land).addOptional(ResourceLocation.parse("tfc:patterned_ground"));
        tag(land).addOptional(ResourceLocation.parse("tfc:plains"));
        tag(land).addOptional(ResourceLocation.parse("tfc:plateau"));
        tag(land).addOptional(ResourceLocation.parse("tfc:plateau_wide"));
        tag(land).addOptional(ResourceLocation.parse("tfc:rift_valley"));
        tag(land).addOptional(ResourceLocation.parse("tfc:rocky_plateau"));
        tag(land).addOptional(ResourceLocation.parse("tfc:rolling_hills"));
        tag(land).addOptional(ResourceLocation.parse("tfc:salt_flats"));
        tag(land).addOptional(ResourceLocation.parse("tfc:salt_marsh"));
        tag(land).addOptional(ResourceLocation.parse("tfc:shilin_canyons"));
        tag(land).addOptional(ResourceLocation.parse("tfc:shilin_highlands"));
        tag(land).addOptional(ResourceLocation.parse("tfc:shilin_hills"));
        tag(land).addOptional(ResourceLocation.parse("tfc:shilin_plains"));
        tag(land).addOptional(ResourceLocation.parse("tfc:shilin_plateau"));
        tag(land).addOptional(ResourceLocation.parse("tfc:stair_step_canyons"));
        tag(land).addOptional(ResourceLocation.parse("tfc:stone_circles"));
        tag(land).addOptional(ResourceLocation.parse("tfc:tower_karst_canyons"));
        tag(land).addOptional(ResourceLocation.parse("tfc:tower_karst_highlands"));
        tag(land).addOptional(ResourceLocation.parse("tfc:tower_karst_hills"));
        tag(land).addOptional(ResourceLocation.parse("tfc:tower_karst_plains"));
        tag(land).addOptional(ResourceLocation.parse("tfc:tuyas"));
        tag(land).addOptional(ResourceLocation.parse("tfc:volcanic_mountains"));
        tag(land).addOptional(ResourceLocation.parse("tfc:whorled_canyons"));
        for (String element : new String[] {"fire", "ice", "lightning"}) {
            for (String kind : new String[] {"roost", "cave"}) {
                tag(TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("iceandfire",
                    "structure_gen/" + element + "_" + kind))).addTag(land);
            }
        }
    }
}
