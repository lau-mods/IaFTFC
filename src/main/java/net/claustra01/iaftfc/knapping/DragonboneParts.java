package net.claustra01.iaftfc.knapping;

import net.claustra01.iaftfc.IaFTFC;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class DragonboneParts {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(IaFTFC.MOD_ID);

    public static final DeferredItem<Item> AXE_HEAD = ITEMS.registerSimpleItem("dragonbone/axe_head");
    public static final DeferredItem<Item> HOE_HEAD = ITEMS.registerSimpleItem("dragonbone/hoe_head");
    public static final DeferredItem<Item> PICKAXE_HEAD = ITEMS.registerSimpleItem("dragonbone/pickaxe_head");
    public static final DeferredItem<Item> SHOVEL_HEAD = ITEMS.registerSimpleItem("dragonbone/shovel_head");
    public static final DeferredItem<Item> SWORD_BLADE = ITEMS.registerSimpleItem("dragonbone/sword_blade");

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }

    private DragonboneParts() {
    }
}
