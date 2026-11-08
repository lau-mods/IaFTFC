package net.claustra01.iaftfc.dragon;

import net.dries007.tfc.common.TFCTags;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;

public final class BreathTerrainConversion {
    private static final TagKey<Block> RAW_STONE = tag("c:stones/raw");
    private static final TagKey<Block> HARDENED_STONE = tag("c:stones/hardened");

    public static BlockState convert(BlockState source, BlockState ceResult, Element element) {
        ResourceLocation sourceId = BuiltInRegistries.BLOCK.getKey(source.getBlock());
        if (!sourceId.getNamespace().equals("tfc")) {
            return ceResult;
        }

        String terrain = terrain(source, sourceId.getPath());
        if (terrain == null) {
            return ceResult;
        }

        return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(
            "iceandfire", element.prefix + "_" + terrain)).defaultBlockState();
    }

    private static String terrain(BlockState state, String path) {
        if (state.is(TFCTags.Blocks.GRASS)) {
            return "grass";
        }
        if (path.startsWith("dirt/") || path.startsWith("rooted_dirt/")
            || state.is(TFCTags.Blocks.COARSE_DIRT)
            || state.is(TFCTags.Blocks.MUD)
            || state.is(TFCTags.Blocks.FARMLANDS)) {
            return "dirt";
        }
        if (state.is(TFCTags.Blocks.PATHS)) {
            return "dirt_path";
        }
        if (state.is(RAW_STONE) || state.is(HARDENED_STONE)) {
            return "stone";
        }
        if (state.is(Tags.Blocks.COBBLESTONES_NORMAL) || state.is(Tags.Blocks.COBBLESTONES_MOSSY)) {
            return "cobblestone";
        }
        if (state.is(Tags.Blocks.GRAVELS)) {
            return "gravel";
        }
        return null;
    }

    private static TagKey<Block> tag(String id) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.parse(id));
    }

    public enum Element {
        FIRE("chared"),
        ICE("frozen"),
        LIGHTNING("crackled");

        private final String prefix;

        Element(String prefix) {
            this.prefix = prefix;
        }
    }

    private BreathTerrainConversion() {
    }
}
