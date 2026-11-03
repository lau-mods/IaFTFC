package net.claustra01.iaftfc.dragonarmor;

import com.iafenvoy.iceandfire.data.DragonArmorMaterial;
import com.iafenvoy.iceandfire.data.DragonArmorPart;
import com.iafenvoy.iceandfire.item.DragonArmorItem;
import com.iafenvoy.iceandfire.registry.IafCreativeModeTabs;
import java.util.Arrays;
import java.util.List;
import net.claustra01.iaftfc.IaFTFC;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class DragonArmor {
    public static final String[] METALS = {"steel", "black_steel", "red_steel", "blue_steel"};
    public static final String[] DISABLED_METALS = {"copper", "silver", "gold", "diamond", "netherite"};
    public static final String[] PARTS = {"head", "neck", "body", "tail"};
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(IaFTFC.MOD_ID);

    static {
        double[] protection = {5, 7, 8, 8};
        for (int metal = 0; metal < METALS.length; metal++) {
            DragonArmorMaterial material = new DragonArmorMaterial(METALS[metal], protection[metal], false, false);
            for (DragonArmorPart part : DragonArmorPart.values()) {
                String id = "dragonarmor_" + METALS[metal] + "_" + part.getId();
                ITEMS.register(id, () -> new DragonArmorItem(material, part) {
                    @Override
                    public String getDescriptionId() {
                        return "item.iaftfc." + id;
                    }
                });
            }
        }
    }

    public static boolean disabled(ItemStack stack) {
        return stack.getItem() instanceof DragonArmorItem armor && disabled(armor.type);
    }

    public static boolean disabled(DragonArmorMaterial material) {
        return Arrays.asList(DISABLED_METALS).contains(material.name());
    }

    public static void buildCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        for (ItemStack stack : List.copyOf(event.getParentEntries())) {
            if (disabled(stack)) event.remove(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        }
        for (ItemStack stack : List.copyOf(event.getSearchEntries())) {
            if (disabled(stack)) event.remove(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        }
        if (event.getTab() == IafCreativeModeTabs.ARMORS.get()) {
            for (var item : ITEMS.getEntries()) {
                event.accept(new ItemStack(item.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            }
        }
    }
}
