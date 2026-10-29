package net.claustra01.iaftfc.registry;

import com.iafenvoy.iceandfire.data.DragonArmorPart;
import com.iafenvoy.iceandfire.item.DragonArmorItem;
import net.claustra01.iaftfc.common.DragonArmorMaterials;
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
        for (var material : DragonArmorMaterials.ALL) {
            for (DragonArmorPart part : DragonArmorPart.values()) {
                String id = "dragonarmor_" + material.name() + "_" + part.getId();
                ITEMS.register(id, () -> new DragonArmorItem(material, part) {
                    @Override
                    public String getDescriptionId() {
                        return "item.iaftfc." + id;
                    }
                });
            }
        }
        ModBlocks.ALL.forEach((id, block) -> ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties())));
    }

    private ModItems() {}
}
