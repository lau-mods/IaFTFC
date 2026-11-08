package net.claustra01.iaftfc.mixin;

import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import java.util.List;
import net.dries007.tfc.util.PhysicalDamageType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = PhysicalDamageType.class, remap = false)
public abstract class DragonPhysicalResistanceMixin {
    @Redirect(method = "calculateMultiplier",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;getArmorSlots()Ljava/lang/Iterable;"))
    private static Iterable<ItemStack> iaftfc$useDragonArmorSlots(LivingEntity entity) {
        if (entity instanceof DragonBaseEntity dragon) {
            return List.of(
                dragon.getItemBySlot(EquipmentSlot.HEAD),
                dragon.getItemBySlot(EquipmentSlot.CHEST),
                dragon.getItemBySlot(EquipmentSlot.LEGS),
                dragon.getItemBySlot(EquipmentSlot.FEET));
        }
        return entity.getArmorSlots();
    }
}
