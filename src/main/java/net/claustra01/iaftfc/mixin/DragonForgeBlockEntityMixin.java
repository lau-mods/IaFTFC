package net.claustra01.iaftfc.mixin;

import com.iafenvoy.iceandfire.item.block.entity.DragonForgeBlockEntity;
import com.iafenvoy.iceandfire.recipe.DragonForgeRecipe;
import net.claustra01.iaftfc.common.ForgeHeatConfig;
import net.dries007.tfc.common.component.heat.HeatCapability;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DragonForgeBlockEntity.class)
public abstract class DragonForgeBlockEntityMixin {
    @Inject(method = "smeltItem", remap = false, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V", ordinal = 0))
    private void iaftfc$heatCompletedIngot(CallbackInfo callback) {
        DragonForgeBlockEntity forge = (DragonForgeBlockEntity) (Object) this;
        forge.getCurrentRecipe().map(DragonForgeRecipe::getResultItem).ifPresent(result -> {
            ItemStack output = forge.getItem(2);
            var id = BuiltInRegistries.ITEM.getKey(result.getItem());
            String path = id.getPath();
            if (id.getNamespace().equals("iaftfc")
                && (path.equals("metal/ingot/dragonsteel_fire") || path.equals("metal/ingot/dragonsteel_ice")
                    || path.equals("metal/ingot/dragonsteel_lightning")) && output.is(result.getItem())) {
                ForgeHeatConfig.prepareHeat(output);
                HeatCapability.setTemperature(output, ForgeHeatConfig.temperature());
            }
        });
    }
}
