package net.claustra01.iaftfc.mixin;

import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import net.claustra01.iaftfc.dragon.CalendarDragonGrowth;
import net.dries007.tfc.util.calendar.ICalendar;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DragonBaseEntity.class)
public abstract class DragonCalendarGrowthMixin implements CalendarDragonGrowth {
    @Shadow public abstract boolean isAgingDisabled();
    @Shadow public abstract int getAgeInTicks();
    @Shadow public abstract void setAgeInTicks(int age);
    @Shadow public abstract void refreshDirtyAttributes();
    @Shadow public abstract void growDragon(int days);

    @Unique private long iaftfc$lastCalendarTick;
    @Unique private long iaftfc$remainder;
    @Unique private boolean iaftfc$calendarInitialized;

    @Override
    public void iaftfc$advanceCalendar(long now) {
        if (!iaftfc$calendarInitialized || now < iaftfc$lastCalendarTick) {
            iaftfc$resetCalendar(now);
            return;
        }
        long elapsed = now - iaftfc$lastCalendarTick;
        iaftfc$lastCalendarTick = now;
        if (isAgingDisabled()) return;
        long accumulated = iaftfc$remainder + elapsed;
        long days = accumulated / ICalendar.CALENDAR_TICKS_IN_DAY;
        iaftfc$remainder = accumulated % ICalendar.CALENDAR_TICKS_IN_DAY;
        if (days > 0) {
            setAgeInTicks((int) Math.min(Integer.MAX_VALUE,
                (long) getAgeInTicks() + days * ICalendar.CALENDAR_TICKS_IN_DAY));
            refreshDirtyAttributes();
            growDragon(0);
        }
    }

    @Override
    public void iaftfc$resetCalendar(long now) {
        iaftfc$lastCalendarTick = now;
        iaftfc$calendarInitialized = true;
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void iaftfc$saveCalendar(CompoundTag tag, CallbackInfo callback) {
        if (iaftfc$calendarInitialized) {
            tag.putLong("IaftfcLastCalendarTick", iaftfc$lastCalendarTick);
            tag.putLong("IaftfcCalendarRemainder", iaftfc$remainder);
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void iaftfc$loadCalendar(CompoundTag tag, CallbackInfo callback) {
        iaftfc$calendarInitialized = tag.contains("IaftfcLastCalendarTick");
        if (iaftfc$calendarInitialized) {
            iaftfc$lastCalendarTick = tag.getLong("IaftfcLastCalendarTick");
            iaftfc$remainder = tag.getLong("IaftfcCalendarRemainder");
        }
    }
}
