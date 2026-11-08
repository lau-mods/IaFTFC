package net.claustra01.iaftfc.mixin;

import java.util.Optional;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(StructurePlacement.class)
public interface StructurePlacementAccessor {
    @Accessor("exclusionZone")
    Optional<StructurePlacement.ExclusionZone> iaftfc$getExclusionZone();
}
