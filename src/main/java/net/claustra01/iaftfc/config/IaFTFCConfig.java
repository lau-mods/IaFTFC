package net.claustra01.iaftfc.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class IaFTFCConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue DRAGON_ROOST_SPACING;
    public static final ModConfigSpec.IntValue DRAGON_CAVE_SPACING;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        DRAGON_ROOST_SPACING = builder.defineInRange("dragonRoostSpacing", 24, 2, 1024);
        DRAGON_CAVE_SPACING = builder.defineInRange("dragonCaveSpacing", 30, 2, 1024);
        SPEC = builder.build();
    }

    private IaFTFCConfig() {
    }
}
