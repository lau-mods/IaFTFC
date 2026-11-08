package net.claustra01.iaftfc.mixin;

import java.util.Set;
import net.claustra01.iaftfc.worldgen.DragonCaveOres;
import net.claustra01.iaftfc.worldgen.DragonChests;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.iafenvoy.iceandfire.world.structure.DragonCaveStructure$DragonCavePiece", remap = false)
public abstract class DragonCaveOresMixin {
    @Shadow
    protected abstract BlockState getPaletteBlock(RandomSource random);

    @Inject(method = "createShell", at = @At("HEAD"), cancellable = true)
    private void iaftfc$replaceCaveOres(LevelAccessor level, RandomSource random, Set<BlockPos> shell,
        CallbackInfo callback) {
        DragonCaveOres.replaceShell(level, random, shell, DragonChests.element(this), this::getPaletteBlock);
        callback.cancel();
    }
}
