package net.claustra01.iaftfc.metal;

import net.dries007.tfc.common.component.TFCComponents;
import net.dries007.tfc.common.component.heat.HeatCapability;
import net.dries007.tfc.common.component.heat.HeatComponent;
import net.dries007.tfc.common.component.heat.HeatDefinition;
import net.minecraft.world.item.ItemStack;

public final class DragonForgeHeat {
    public static ItemStack apply(ItemStack stack) {
        if (Dragonsteel.isIngot(stack.getItem())) {
            if (!HeatCapability.has(stack)) {
                HeatDefinition definition = HeatCapability.getDefinition(stack);
                if (definition != null) {
                    stack.set(TFCComponents.HEAT, HeatComponent.of(definition));
                }
            }
            HeatCapability.setTemperature(stack, Dragonsteel.FORGE_OUTPUT_TEMPERATURE);
        }
        return stack;
    }

    private DragonForgeHeat() {
    }
}
