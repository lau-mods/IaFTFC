package net.claustra01.iaftfc.mixin;

import com.iafenvoy.iceandfire.entity.GhostEntity;
import com.iafenvoy.iceandfire.event.handler.ServerEvents;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerEvents.class)
public abstract class GhostDeathSpawnMixin {
    @WrapOperation(method = "onEntityDie", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private static boolean iaftfc$preventDeathGhost(Level level, Entity entity, Operation<Boolean> original) {
        return !(entity instanceof GhostEntity) && original.call(level, entity);
    }
}
