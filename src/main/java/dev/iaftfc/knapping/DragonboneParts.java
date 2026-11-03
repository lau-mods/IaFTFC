package dev.iaftfc.knapping;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.world.item.Item;

public final class DragonboneParts {
    public static final String[] TOOLS = {"sword", "pickaxe", "axe", "shovel", "hoe"};
    public static final String[] PARTS = {"sword_blade", "pickaxe_head", "axe_head", "shovel_head", "hoe_head"};
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("iaftfc");

    static {
        for (String part : PARTS) {
            ITEMS.register("dragonbone/" + part,
                () -> new Item(new Item.Properties()));
        }
    }

    private DragonboneParts() {
    }
}
