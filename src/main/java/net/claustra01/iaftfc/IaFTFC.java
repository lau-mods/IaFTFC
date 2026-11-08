package net.claustra01.iaftfc;

import net.claustra01.iaftfc.config.IaFTFCConfig;
import net.claustra01.iaftfc.datagen.DragonArmorData;
import net.claustra01.iaftfc.datagen.DragonBiomeTags;
import net.claustra01.iaftfc.datagen.DragonFoodData;
import net.claustra01.iaftfc.datagen.DragonLootData;
import net.claustra01.iaftfc.datagen.DragonStructureSets;
import net.claustra01.iaftfc.datagen.DragonToolRecipeData;
import net.claustra01.iaftfc.datagen.DragonsteelData;
import net.claustra01.iaftfc.datagen.DisabledContentData;
import net.claustra01.iaftfc.datagen.ElementalWorldgenData;
import net.claustra01.iaftfc.datagen.ElementalPlantAssets;
import net.claustra01.iaftfc.datagen.KnappingData;
import net.claustra01.iaftfc.datagen.NonDragonWorldgenData;
import net.claustra01.iaftfc.datagen.TfcCompatData;
import net.claustra01.iaftfc.dragonarmor.DragonArmor;
import net.claustra01.iaftfc.knapping.DragonboneParts;
import net.claustra01.iaftfc.metal.Dragonsteel;
import net.claustra01.iaftfc.plant.IaFTFCPlants;
import net.claustra01.iaftfc.worldgen.StructureSpacingResolver;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.common.data.LanguageProvider;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

@Mod(IaFTFC.MOD_ID)
public final class IaFTFC {
    public static final String MOD_ID = "iaftfc";

    public IaFTFC(IEventBus modBus, ModContainer container) {
        Dragonsteel.register(modBus);
        DragonboneParts.register(modBus);
        DragonArmor.register(modBus);
        IaFTFCPlants.register(modBus);
        IaFTFCCreativeTab.TABS.register(modBus);
        container.registerConfig(ModConfig.Type.COMMON, IaFTFCConfig.SPEC);
        modBus.addListener(IaFTFC::gatherData);
        modBus.addListener(IaFTFCCreativeTab::removeDisabled);
        NeoForge.EVENT_BUS.addListener((ServerAboutToStartEvent event) ->
            StructureSpacingResolver.apply(event.getServer()));
    }

    private static void gatherData(GatherDataEvent event) {
        if (event.includeServer()) {
            event.createProvider((output, lookup) -> new DragonBiomeTags(output, lookup, event.getExistingFileHelper()));
            event.createProvider(DragonArmorData::new);
            event.createProvider(DisabledContentData::new);
            event.createProvider(DragonFoodData::new);
            event.createProvider(DragonLootData::new);
            event.createProvider(DragonStructureSets::new);
            event.createProvider(DragonToolRecipeData::new);
            event.createProvider(DragonsteelData::new);
            event.createProvider(ElementalWorldgenData::new);
            event.createProvider(KnappingData::new);
            event.createProvider(NonDragonWorldgenData::new);
            event.createProvider(TfcCompatData::new);
        }
        if (event.includeClient()) {
            event.createProvider(ElementalPlantAssets::new);
            event.createProvider(output -> new LanguageProvider(output, MOD_ID, "en_us") {
                @Override
                protected void addTranslations() {
                    add("iaftfc.configuration.dragonRoostSpacing", "Dragon roost spacing");
                    add("iaftfc.configuration.dragonCaveSpacing", "Dragon cave spacing");
                    add("itemGroup.iaftfc.main", "Ice and Fire for TFC");
                    add("tooltip.iaftfc.disabled", "[DISABLED]");
                    add("block.iaftfc.plant.fire_lily", "Fire Lily");
                    add("block.iaftfc.plant.frost_lily", "Frost Lily");
                    add("block.iaftfc.plant.lightning_lily", "Lightning Lily");
                    add("item.iaftfc.dragonbone.axe_head", "Dragon Bone Axe Head");
                    add("item.iaftfc.dragonbone.hoe_head", "Dragon Bone Hoe Head");
                    add("item.iaftfc.dragonbone.pickaxe_head", "Dragon Bone Pickaxe Head");
                    add("item.iaftfc.dragonbone.shovel_head", "Dragon Bone Shovel Head");
                    add("item.iaftfc.dragonbone.sword_blade", "Dragon Bone Sword Blade");
                    for (DragonArmor.Metal metal : DragonArmor.METALS) {
                        for (var part : com.iafenvoy.iceandfire.data.DragonArmorPart.values()) {
                            String partName = switch (part) {
                                case HEAD -> "Head";
                                case NECK -> "Neck";
                                case BODY -> "Body";
                                case TAIL -> "Tail";
                            };
                            add("item.iaftfc." + DragonArmor.id(metal.id(), part),
                                metal.displayName() + " Dragon Armor " + partName);
                        }
                    }
                    for (Dragonsteel metal : Dragonsteel.values()) {
                        String metalName = switch (metal) {
                            case FIRE -> "Fire Dragonsteel";
                            case ICE -> "Ice Dragonsteel";
                            case LIGHTNING -> "Lightning Dragonsteel";
                        };
                        for (var type : Dragonsteel.ITEM_TYPES) {
                            String form = type.name();
                            switch (type) {
                                case INGOT -> form = "Ingot";
                                case DOUBLE_INGOT -> form = "Double Ingot";
                                case SHEET -> form = "Sheet";
                                case DOUBLE_SHEET -> form = "Double Sheet";
                                case ROD -> form = "Rod";
                                case AXE_HEAD -> form = "Axe Head";
                                case HOE_HEAD -> form = "Hoe Head";
                                case PICKAXE_HEAD -> form = "Pickaxe Head";
                                case SHOVEL_HEAD -> form = "Shovel Head";
                                case SWORD_BLADE -> form = "Sword Blade";
                                case UNFINISHED_HELMET -> form = "Unfinished Helmet";
                                case UNFINISHED_CHESTPLATE -> form = "Unfinished Chestplate";
                                case UNFINISHED_GREAVES -> form = "Unfinished Leggings";
                                case UNFINISHED_BOOTS -> form = "Unfinished Boots";
                            }
                            add("item.iaftfc.metal." + type.name().toLowerCase(java.util.Locale.ROOT) + "."
                                + metal.getSerializedName(), metalName + " " + form);
                        }
                        add("block.iaftfc.metal.block." + metal.getSerializedName(), metalName + " Plated Block");
                        add("block.iaftfc.metal.block." + metal.getSerializedName() + "_slab",
                            metalName + " Plated Slab");
                        add("block.iaftfc.metal.block." + metal.getSerializedName() + "_stairs",
                            metalName + " Plated Stairs");
                        add("block.iaftfc.metal.anvil." + metal.getSerializedName(), metalName + " Anvil");
                        add("item.iaftfc.metal.bucket." + metal.getSerializedName(), "Molten " + metalName + " Bucket");
                        add("fluid.tfc.metal." + metal.getSerializedName(), "Molten " + metalName);
                    }
                }
            });
        }
    }

}
