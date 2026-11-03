package net.claustra01.iaftfc.worldgen;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ElementalPlacements {
    public static final DeferredRegister<PlacementModifierType<?>> TYPES =
        DeferredRegister.create(Registries.PLACEMENT_MODIFIER_TYPE, "iaftfc");
    public static final DeferredHolder<PlacementModifierType<?>, PlacementModifierType<ElementalPlacement>> CLIMATE =
        TYPES.register("climate", () -> () -> ElementalPlacement.CODEC);

    private ElementalPlacements() {}
}
