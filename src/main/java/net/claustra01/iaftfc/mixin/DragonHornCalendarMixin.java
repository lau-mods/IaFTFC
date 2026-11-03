package net.claustra01.iaftfc.mixin;

import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.item.DragonHornItem;
import net.claustra01.iaftfc.dragon.CalendarDragonGrowth;
import net.dries007.tfc.util.calendar.Calendars;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(DragonHornItem.class)
public abstract class DragonHornCalendarMixin {
    @Redirect(method = "useOn", at = @At(value = "INVOKE",
        target = "Lcom/iafenvoy/iceandfire/entity/DragonBaseEntity;load(Lnet/minecraft/nbt/CompoundTag;)V"))
    private void iaftfc$releaseWithoutStoredGrowth(DragonBaseEntity dragon, CompoundTag tag) {
        dragon.load(tag);
        if (!dragon.level().isClientSide) {
            ((CalendarDragonGrowth) dragon).iaftfc$resetCalendar(Calendars.SERVER.getCalendarTicks());
        }
    }
}
