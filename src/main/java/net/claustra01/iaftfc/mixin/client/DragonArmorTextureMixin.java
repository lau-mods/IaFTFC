package net.claustra01.iaftfc.mixin.client;

import com.iafenvoy.iceandfire.data.DragonArmorPart;
import com.iafenvoy.iceandfire.item.DragonArmorItem;
import com.iafenvoy.iceandfire.render.entity.feature.DragonArmorFeatureRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = DragonArmorFeatureRenderer.class, remap = false)
public abstract class DragonArmorTextureMixin {
    @Inject(method = "getArmorTexture", at = @At("RETURN"), cancellable = true)
    private static void useAddedArmorTexture(ItemStack stack, EquipmentSlot slot, CallbackInfoReturnable<ResourceLocation> result) {
        if (result.getReturnValue() == null || !(stack.getItem() instanceof DragonArmorItem armor)
            || !BuiltInRegistries.ITEM.getKey(armor).getNamespace().equals("iaftfc")) return;
        if (armor.dragonSlot != DragonArmorPart.fromSlot(slot)) {
            result.setReturnValue(null);
            return;
        }
        result.setReturnValue(ResourceLocation.fromNamespaceAndPath("iaftfc",
            "textures/models/dragonarmor/armor_" + DragonArmorPart.fromSlot(slot).getId() + "_" + armor.type.name() + ".png"));
    }
}
