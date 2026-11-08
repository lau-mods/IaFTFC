package net.claustra01.iaftfc.mixin;

import com.iafenvoy.uranus.object.item.FoodUtils;
import net.claustra01.iaftfc.dragon.TfcDragonFood;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = FoodUtils.class, remap = false)
public abstract class UranusFoodUtilsMixin {
    @Inject(
        method = "getFoodPoints(Lnet/minecraft/world/item/ItemStack;ZZ)I",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void iaftfc$useTfcHunger(ItemStack stack, boolean acceptsMeat, boolean acceptsFish,
        CallbackInfoReturnable<Integer> callback) {
        int foodPoints = TfcDragonFood.foodPointsOverride(stack, acceptsMeat, acceptsFish);
        if (foodPoints >= 0) {
            callback.setReturnValue(foodPoints);
        }
    }
}
