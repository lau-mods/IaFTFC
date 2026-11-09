package net.claustra01.iaftfc.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.claustra01.iaftfc.dragon.TfcDragonFood;
import net.dries007.tfc.common.component.TFCComponents;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = TFCComponents.class, remap = false)
public abstract class DragonFoodComponentMixin {
    @WrapOperation(method = "onModifyDefaultComponentsAfterResourceReload",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/core/component/DataComponentPatch$Builder;remove(Lnet/minecraft/core/component/DataComponentType;)Lnet/minecraft/core/component/DataComponentPatch$Builder;"))
    private static DataComponentPatch.Builder iaftfc$keepCeFood(DataComponentPatch.Builder builder,
        DataComponentType<?> type, Operation<DataComponentPatch.Builder> original, @Local Item item) {
        if (type == DataComponents.FOOD && TfcDragonFood.keepsCeFoodComponent(item)) {
            return builder;
        }
        return original.call(builder, type);
    }

    @WrapOperation(method = "onModifyDefaultComponentsAfterResourceReload",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/core/component/DataComponentPatch$Builder;set(Lnet/minecraft/core/component/DataComponentType;Ljava/lang/Object;)Lnet/minecraft/core/component/DataComponentPatch$Builder;"))
    private static DataComponentPatch.Builder iaftfc$skipEmptyFood(DataComponentPatch.Builder builder,
        DataComponentType<?> type, Object value, Operation<DataComponentPatch.Builder> original, @Local Item item) {
        if (type == DataComponents.FOOD && TfcDragonFood.keepsCeFoodComponent(item)) {
            return builder;
        }
        return original.call(builder, type, value);
    }
}
