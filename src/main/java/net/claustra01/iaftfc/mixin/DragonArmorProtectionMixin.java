package net.claustra01.iaftfc.mixin;

import com.iafenvoy.iceandfire.data.DragonArmorMaterial;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import net.claustra01.iaftfc.dragonarmor.DragonArmor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(DragonBaseEntity.class)
public abstract class DragonArmorProtectionMixin {
    @Redirect(method = "calculateArmorModifier", at = @At(value = "INVOKE",
        target = "Lcom/iafenvoy/iceandfire/data/DragonArmorMaterial;protection()D"))
    private double iaftfc$validProtection(DragonArmorMaterial material) {
        return DragonArmor.disabled(material) ? 0 : material.protection();
    }
}
