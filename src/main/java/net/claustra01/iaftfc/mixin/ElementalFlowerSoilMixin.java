package net.claustra01.iaftfc.mixin;

import com.iafenvoy.iceandfire.item.block.ElementalFlowerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ElementalFlowerBlock.class)
public abstract class ElementalFlowerSoilMixin {
    @Inject(method = "mayPlaceOn", at = @At("HEAD"), cancellable = true)
    private void iaftfc$recognizeTfcDirt(BlockState soil, BlockGetter level, BlockPos pos,
        CallbackInfoReturnable<Boolean> result) {
        if (soil.is(BlockTags.DIRT)
            && BuiltInRegistries.BLOCK.getKey(soil.getBlock()).getNamespace().equals("tfc")) {
            result.setReturnValue(true);
        }
    }
}
