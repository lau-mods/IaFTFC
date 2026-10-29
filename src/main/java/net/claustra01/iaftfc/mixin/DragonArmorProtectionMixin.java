package net.claustra01.iaftfc.mixin;

import com.iafenvoy.iceandfire.data.DragonArmorPart;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.item.DragonArmorItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = DragonBaseEntity.class, remap = false)
public abstract class DragonArmorProtectionMixin {
    private static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    @Inject(method = "calculateArmorModifier", at = @At("RETURN"), cancellable = true)
    private void excludeWrongArmorSlots(CallbackInfoReturnable<Double> result) {
        DragonBaseEntity dragon = (DragonBaseEntity) (Object) this;
        double protection = result.getReturnValue();
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = dragon.getItemBySlot(slot);
            if (stack.getItem() instanceof DragonArmorItem armor
                && BuiltInRegistries.ITEM.getKey(armor).getNamespace().equals("iaftfc")
                && armor.dragonSlot != DragonArmorPart.fromSlot(slot)) {
                protection -= armor.type.protection();
            }
        }
        result.setReturnValue(protection);
    }
}
