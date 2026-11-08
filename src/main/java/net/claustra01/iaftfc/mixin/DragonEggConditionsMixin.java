package net.claustra01.iaftfc.mixin;

import com.iafenvoy.iceandfire.entity.DragonEggEntity;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.claustra01.iaftfc.dragon.EggHeatSources;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(DragonEggEntity.class)
public abstract class DragonEggConditionsMixin {
    @ModifyExpressionValue(
        method = "lambda$static$0",
        at = @At(value = "INVOKE",
            target = "Lcom/iafenvoy/uranus/object/BlockUtil;isBurning(Lnet/minecraft/world/level/block/state/BlockState;)Z",
            remap = false)
    )
    private static boolean iaftfc$acceptTfcHeatSource(boolean ceHeatSource, DragonEggEntity egg,
        Level level, BlockPos eggPos, boolean readyToHatch) {
        return ceHeatSource || EggHeatSources.isActiveTfcHeatSource(level, eggPos);
    }

    @ModifyExpressionValue(
        method = "lambda$static$1",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z")
    )
    private static boolean iaftfc$acceptTfcFreshWater(boolean ceWater, DragonEggEntity egg,
        Level level, BlockPos eggPos, boolean readyToHatch) {
        return ceWater || EggHeatSources.isFreshWater(level, eggPos);
    }
}
