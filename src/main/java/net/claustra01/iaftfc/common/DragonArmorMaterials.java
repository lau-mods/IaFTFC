package net.claustra01.iaftfc.common;

import com.iafenvoy.iceandfire.data.DragonArmorMaterial;
import java.util.List;

public final class DragonArmorMaterials {
    public static final List<DragonArmorMaterial> ALL = List.of(
        new DragonArmorMaterial("steel", 3.5, false, false),
        new DragonArmorMaterial("black_steel", 5.5, false, false),
        new DragonArmorMaterial("red_steel", 7.5, false, false),
        new DragonArmorMaterial("blue_steel", 7.5, false, false)
    );

    private DragonArmorMaterials() {}
}
