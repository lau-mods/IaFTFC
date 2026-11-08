package net.claustra01.iaftfc.mixin;

import com.iafenvoy.iceandfire.item.block.entity.DragonForgeBlockEntity;
import com.iafenvoy.iceandfire.recipe.DragonForgeRecipe;
import net.claustra01.iaftfc.metal.DragonForgeHeat;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DragonForgeRecipe.class)
public abstract class DragonForgeRecipeHeatMixin {
    @Inject(method = "getResultItem()Lnet/minecraft/world/item/ItemStack;", at = @At("RETURN"), cancellable = true)
    private void iaftfc$displayHotResult(CallbackInfoReturnable<ItemStack> callback) {
        callback.setReturnValue(DragonForgeHeat.apply(callback.getReturnValue()));
    }

    @Inject(method = "getResultItem(Lnet/minecraft/core/HolderLookup$Provider;)Lnet/minecraft/world/item/ItemStack;",
        at = @At("RETURN"), cancellable = true)
    private void iaftfc$codecHotResult(HolderLookup.Provider lookup,
        CallbackInfoReturnable<ItemStack> callback) {
        callback.setReturnValue(DragonForgeHeat.apply(callback.getReturnValue()));
    }

    @Inject(method = "assemble", at = @At("RETURN"), cancellable = true)
    private void iaftfc$assembleHotResult(DragonForgeBlockEntity.DragonForgeRecipeInput input,
        HolderLookup.Provider lookup, CallbackInfoReturnable<ItemStack> callback) {
        callback.setReturnValue(DragonForgeHeat.apply(callback.getReturnValue()));
    }
}
