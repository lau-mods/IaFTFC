package net.claustra01.iaftfc.registry;

import net.claustra01.iaftfc.common.DragonsteelMetal;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "iaftfc");
    private static final String[] PARTS = {"ingot", "double_ingot", "sheet", "double_sheet", "rod", "axe_head", "hoe_head", "pickaxe_head", "shovel_head", "sword_blade", "unfinished_helmet", "unfinished_chestplate", "unfinished_greaves", "unfinished_boots"};

    static {
        for (DragonsteelMetal metal : DragonsteelMetal.values()) {
            for (String part : PARTS) {
                String id = "metal/" + part + "/" + metal.id();
                ITEMS.register(id, () -> new Item(new Item.Properties().rarity(Rarity.EPIC)));
            }
        }

        for (String part : new String[] {"axe_head", "hoe_head", "pickaxe_head", "shovel_head", "sword_blade"}) {
            String id = "dragonbone/" + part;
            ITEMS.register(id, () -> new Item(new Item.Properties()));
        }
        for (String metal : new String[] {"steel", "black_steel", "red_steel", "blue_steel"}) {
            for (String part : new String[] {"head", "neck", "body", "tail"}) {
                String id = "dragonarmor_" + metal + "_" + part;
                ITEMS.register(id, () -> new Item(new Item.Properties()));
            }
        }
        ModBlocks.ALL.forEach((id, block) -> ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties())));
    }

    private ModItems() {}
}
