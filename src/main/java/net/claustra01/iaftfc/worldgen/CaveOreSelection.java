package net.claustra01.iaftfc.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public final class CaveOreSelection {
    private static final String[] GEMS = {
        "amethyst", "diamond", "emerald", "lapis_lazuli", "opal", "pyrite", "ruby", "sapphire", "topaz"
    };

    public static int index(long caveSeed, BlockPos pos) {
        long value = ClimateSelection.splitMix64(caveSeed ^ pos.asLong());
        long threshold = Long.remainderUnsigned(-9L, 9);
        while (Long.compareUnsigned(value, threshold) < 0) value = ClimateSelection.splitMix64(value);
        return (int) Long.remainderUnsigned(value, 9);
    }

    public static ResourceLocation select(ClimateSelection.Element element, int index) {
        String rock = switch (element) {
            case FIRE -> "basalt";
            case ICE -> "quartzite";
            case LIGHTNING -> "diorite";
        };
        return ResourceLocation.fromNamespaceAndPath("tfc", "ore/" + GEMS[index] + "/" + rock);
    }

    private CaveOreSelection() {}
}
