package net.claustra01.iaftfc.mixin;

import com.iafenvoy.iceandfire.screen.slot.DragonArmorSlot;
import net.claustra01.iaftfc.dragonarmor.DragonArmor;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DragonArmorSlot.class)
public abstract class DragonArmorSlotMixin {
    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void iaftfc$rejectDisabledArmor(ItemStack stack, CallbackInfoReturnable<Boolean> result) {
        if (DragonArmor.disabled(stack)) result.setReturnValue(false);
    }
}
