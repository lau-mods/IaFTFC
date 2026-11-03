package net.claustra01.iaftfc.mixin;

import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.util.dragon.IafDragonLogic;
import net.claustra01.iaftfc.dragon.CalendarDragonGrowth;
import net.dries007.tfc.util.calendar.Calendars;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(IafDragonLogic.class)
public abstract class DragonCalendarLogicMixin {
    @Shadow @Final private DragonBaseEntity dragon;

    @Redirect(method = "updateDragonServer", at = @At(value = "INVOKE",
        target = "Lcom/iafenvoy/iceandfire/entity/DragonBaseEntity;isAgingDisabled()Z"))
    private boolean iaftfc$skipGameTickGrowth(DragonBaseEntity dragon) {
        return true;
    }

    @Inject(method = "updateDragonServer", at = @At("TAIL"))
    private void iaftfc$calendarGrowth(CallbackInfo callback) {
        ((CalendarDragonGrowth) dragon).iaftfc$advanceCalendar(Calendars.SERVER.getCalendarTicks());
    }
}
