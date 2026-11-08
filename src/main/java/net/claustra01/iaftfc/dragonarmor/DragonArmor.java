package net.claustra01.iaftfc.dragonarmor;

import com.iafenvoy.iceandfire.data.DragonArmorMaterial;
import com.iafenvoy.iceandfire.data.DragonArmorPart;
import com.iafenvoy.iceandfire.item.DragonArmorItem;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.claustra01.iaftfc.IaFTFC;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class DragonArmor {
    public static final List<Metal> METALS = List.of(
        new Metal("steel", "Steel", 3.5D),
        new Metal("black_steel", "Black Steel", 5.5D),
        new Metal("red_steel", "Red Steel", 7.5D),
        new Metal("blue_steel", "Blue Steel", 7.5D));
    public static final List<String> DISABLED_METALS = List.of(
        "copper", "gold", "silver", "diamond", "netherite");

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(IaFTFC.MOD_ID);
    private static final Map<String, DeferredItem<DragonArmorItem>> ARMOR = new LinkedHashMap<>();

    static {
        for (Metal metal : METALS) {
            DragonArmorMaterial material = new DragonArmorMaterial(metal.id(), metal.protection(), false, false);
            for (DragonArmorPart part : DragonArmorPart.values()) {
                String id = id(metal.id(), part);
                ARMOR.put(id, ITEMS.register(id, () -> new DragonArmorItem(material, part) {
                    @Override
                    public String getDescriptionId() {
                        return "item.iaftfc." + id;
                    }
                }));
            }
        }
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }

    public static DeferredItem<DragonArmorItem> item(Metal metal, DragonArmorPart part) {
        return ARMOR.get(id(metal.id(), part));
    }

    public static String id(String metal, DragonArmorPart part) {
        return "dragonarmor_" + metal + "_" + part.getId();
    }

    public record Metal(String id, String displayName, double protection) {
    }

    private DragonArmor() {
    }
}
