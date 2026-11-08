package net.claustra01.iaftfc;

import net.claustra01.iaftfc.config.IaFTFCConfig;
import net.claustra01.iaftfc.datagen.DragonBiomeTags;
import net.claustra01.iaftfc.datagen.DragonFoodData;
import net.claustra01.iaftfc.datagen.DragonLootData;
import net.claustra01.iaftfc.datagen.DragonStructureSets;
import net.claustra01.iaftfc.datagen.ElementalWorldgenData;
import net.claustra01.iaftfc.datagen.ElementalPlantAssets;
import net.claustra01.iaftfc.datagen.KnappingData;
import net.claustra01.iaftfc.datagen.TfcCompatData;
import net.claustra01.iaftfc.knapping.DragonboneParts;
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
        DragonboneParts.register(modBus);
        IaFTFCPlants.register(modBus);
        container.registerConfig(ModConfig.Type.COMMON, IaFTFCConfig.SPEC);
        modBus.addListener(IaFTFC::gatherData);
        NeoForge.EVENT_BUS.addListener((ServerAboutToStartEvent event) ->
            StructureSpacingResolver.apply(event.getServer()));
    }

    private static void gatherData(GatherDataEvent event) {
        if (event.includeServer()) {
            event.createProvider((output, lookup) -> new DragonBiomeTags(output, lookup, event.getExistingFileHelper()));
            event.createProvider(DragonFoodData::new);
            event.createProvider(DragonLootData::new);
            event.createProvider(DragonStructureSets::new);
            event.createProvider(ElementalWorldgenData::new);
            event.createProvider(KnappingData::new);
            event.createProvider(TfcCompatData::new);
        }
        if (event.includeClient()) {
            event.createProvider(ElementalPlantAssets::new);
            event.createProvider(output -> new LanguageProvider(output, MOD_ID, "en_us") {
                @Override
                protected void addTranslations() {
                    add("iaftfc.configuration.dragonRoostSpacing", "Dragon roost spacing");
                    add("iaftfc.configuration.dragonCaveSpacing", "Dragon cave spacing");
                    add("block.iaftfc.plant.fire_lily", "Fire Lily");
                    add("block.iaftfc.plant.frost_lily", "Frost Lily");
                    add("block.iaftfc.plant.lightning_lily", "Lightning Lily");
                    add("item.iaftfc.dragonbone.axe_head", "Dragon Bone Axe Head");
                    add("item.iaftfc.dragonbone.hoe_head", "Dragon Bone Hoe Head");
                    add("item.iaftfc.dragonbone.pickaxe_head", "Dragon Bone Pickaxe Head");
                    add("item.iaftfc.dragonbone.shovel_head", "Dragon Bone Shovel Head");
                    add("item.iaftfc.dragonbone.sword_blade", "Dragon Bone Sword Blade");
                }
            });
        }
    }

}
