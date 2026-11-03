package net.claustra01.iaftfc.mixin;

import com.iafenvoy.iceandfire.entity.DragonEggEntity;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.dries007.tfc.common.blockentities.IHeatable;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(DragonEggEntity.class)
public abstract class DragonEggConditionsMixin {
    @ModifyExpressionValue(method = "lambda$static$0", at = @At(value = "INVOKE",
        target = "Lcom/iafenvoy/uranus/object/BlockUtil;isBurning(Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private static boolean iaftfc$fireHeat(boolean burning, DragonEggEntity egg, Level level, BlockPos pos,
                                           boolean hatching) {
        return burning || level.getBlockEntity(pos.below()) instanceof IHeatable heat
            && heat.getTemperature() >= 1500;
    }

    @ModifyExpressionValue(method = "lambda$static$1", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z"))
    private static boolean iaftfc$iceFreshWater(boolean vanillaWater, DragonEggEntity egg, Level level,
                                                 BlockPos pos, boolean hatching) {
        return vanillaWater || level.getBlockState(pos).getBlock() instanceof LiquidBlock
            && level.getFluidState(pos).is(TagKey.create(Registries.FLUID,
                ResourceLocation.fromNamespaceAndPath("tfc", "any_fresh_water")));
    }
}
