package net.claustra01.iaftfc.mixin;

import com.iafenvoy.iceandfire.entity.ai.DragonAITargetItemsGoal;
import net.dries007.tfc.common.component.food.FoodCapability;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = DragonAITargetItemsGoal.class, remap = false)
public abstract class DragonAITargetItemsGoalFoodMixin {
    @Shadow
    protected ItemEntity targetEntity;

    @Inject(method = "canContinueToUse", at = @At("HEAD"), cancellable = true)
    private void iaftfc$stopTrackingRottenFood(CallbackInfoReturnable<Boolean> callback) {
        if (iaftfc$isTargetRotten()) {
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void iaftfc$rejectRottenFoodBeforeContact(CallbackInfo callback) {
        if (iaftfc$isTargetRotten()) {
            callback.cancel();
        }
    }

    private boolean iaftfc$isTargetRotten() {
        return targetEntity != null && FoodCapability.isRotten(targetEntity.getItem());
    }
}
