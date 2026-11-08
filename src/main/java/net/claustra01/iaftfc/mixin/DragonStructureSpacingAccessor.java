package net.claustra01.iaftfc.mixin;

import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RandomSpreadStructurePlacement.class)
public interface DragonStructureSpacingAccessor {
    @Mutable
    @Accessor("spacing")
    void iaftfc$setSpacing(int spacing);

    @Mutable
    @Accessor("separation")
    void iaftfc$setSeparation(int separation);
}
