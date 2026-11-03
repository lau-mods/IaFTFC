package dev.iaftfc;

import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.minecraft.gametest.framework.GlobalTestReporter;
import net.minecraft.gametest.framework.JUnitLikeTestReporter;
import java.io.File;
import java.nio.file.Files;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.data.LanguageProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import com.mojang.logging.LogUtils;
import dev.iaftfc.worldgen.DragonBiomeTags;
import dev.iaftfc.datagen.DragonLoot;
import dev.iaftfc.loot.EnchantEquipment;
import dev.iaftfc.metal.Dragonsteel;
import dev.iaftfc.datagen.DragonsteelData;
import dev.iaftfc.datagen.KnappingData;
import dev.iaftfc.knapping.DragonboneParts;

@Mod(IaFTFC.MOD_ID)
public final class IaFTFC {
    public static final String MOD_ID = "iaftfc";

    public IaFTFC(IEventBus modBus) {
        LogUtils.getLogger().info("IaFTFC loaded on {}", FMLEnvironment.dist);
        modBus.addListener(this::gatherData);
        EnchantEquipment.FUNCTIONS.register(modBus);
        Dragonsteel.ITEMS.register(modBus);
        DragonboneParts.ITEMS.register(modBus);
        Dragonsteel.BLOCKS.register(modBus);
        Dragonsteel.FLUIDS.register(modBus);
        Dragonsteel.FLUID_TYPES.register(modBus);
        modBus.addListener(Dragonsteel::addAnvilBlocks);
        modBus.addListener(this::registerGameTests);
    }

    private void registerGameTests(RegisterGameTestsEvent event) {
        String path = System.getProperty("iaftfc.gametest.report");
        if (path != null) {
            File report = new File(path);
            try {
                Files.createDirectories(report.toPath().getParent());
            } catch (IOException exception) {
                throw new IllegalStateException("Cannot create GameTest report directory", exception);
            }
            try {
                GlobalTestReporter.replaceWith(new JUnitLikeTestReporter(report));
            } catch (ParserConfigurationException exception) {
                throw new IllegalStateException("Cannot create GameTest report", exception);
            }
        }
    }

    private void gatherData(GatherDataEvent event) {
        event.getGenerator().addProvider(event.includeServer(), new DragonLoot(event.getGenerator().getPackOutput()));
        event.getGenerator().addProvider(event.includeServer(), new DragonsteelData(event.getGenerator().getPackOutput()));
        event.getGenerator().addProvider(event.includeServer(), new KnappingData(event.getGenerator().getPackOutput()));
        event.getGenerator().addProvider(event.includeServer(), new DragonBiomeTags(event.getGenerator().getPackOutput(),
            event.getLookupProvider(), event.getExistingFileHelper()));
        event.getGenerator().addProvider(event.includeClient(), new LanguageProvider(event.getGenerator().getPackOutput(), MOD_ID, "en_us") {
            @Override
            protected void addTranslations() {
                add("fml.menu.mods.info.description.iaftfc", "Connects Ice and Fire dragons with TerraFirmaCraft.");
                String[] parts = {"Sword Blade", "Pickaxe Head", "Axe Head", "Shovel Head", "Hoe Head"};
                for (int index = 0; index < DragonboneParts.TOOLS.length; index++) {
                    add("item.iaftfc.dragonbone." + DragonboneParts.PARTS[index],
                        "Dragonbone " + parts[index]);
                }
                for (String metal : Dragonsteel.METALS) {
                    String name = switch (metal) {
                        case "dragonsteel_fire" -> "Fire Dragonsteel";
                        case "dragonsteel_ice" -> "Ice Dragonsteel";
                        default -> "Lightning Dragonsteel";
                    };
                    for (String part : Dragonsteel.PARTS) {
                        add("item.iaftfc.metal." + part + "." + metal,
                            name + " " + java.util.Arrays.stream(part.split("_"))
                                .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1))
                                .collect(java.util.stream.Collectors.joining(" ")));
                    }
                    add("block.iaftfc.metal.block." + metal, name + " Block");
                    add("block.iaftfc.metal.anvil." + metal, name + " Anvil");
                    add("block.iaftfc.fluid.metal." + metal, "Molten " + name);
                    add("fluid.iaftfc." + metal, "Molten " + name);
                    add("item.iaftfc.bucket.metal." + metal, "Bucket of Molten " + name);
                }
            }
        });
        event.getGenerator().addProvider(event.includeClient(), new LanguageProvider(event.getGenerator().getPackOutput(), MOD_ID, "ja_jp") {
            @Override
            protected void addTranslations() {
                add("fml.menu.mods.info.description.iaftfc", "Ice and Fire のドラゴンを TerraFirmaCraft に接続します。");
                String[] parts = {"剣刃", "ツルハシ頭部", "斧頭", "シャベル頭部", "クワ頭部"};
                for (int index = 0; index < DragonboneParts.TOOLS.length; index++) {
                    add("item.iaftfc.dragonbone." + DragonboneParts.PARTS[index],
                        "ドラゴンの骨の" + parts[index]);
                }
                for (String metal : Dragonsteel.METALS) {
                    String name = switch (metal) {
                        case "dragonsteel_fire" -> "炎のドラゴンスチール";
                        case "dragonsteel_ice" -> "氷のドラゴンスチール";
                        default -> "雷のドラゴンスチール";
                    };
                    String[] names = {"インゴット", "ダブルインゴット", "板", "ダブル板", "棒", "剣刃",
                        "ツルハシ頭部", "斧頭", "シャベル頭部", "クワ頭部", "未完成ヘルメット",
                        "未完成チェストプレート", "未完成レギンス", "未完成ブーツ"};
                    for (int index = 0; index < Dragonsteel.PARTS.length; index++) {
                        add("item.iaftfc.metal." + Dragonsteel.PARTS[index] + "." + metal,
                            name + names[index]);
                    }
                    add("block.iaftfc.metal.block." + metal, name + "ブロック");
                    add("block.iaftfc.metal.anvil." + metal, name + "の金床");
                    add("block.iaftfc.fluid.metal." + metal, "溶融" + name);
                    add("fluid.iaftfc." + metal, "溶融" + name);
                    add("item.iaftfc.bucket.metal." + metal, "溶融" + name + "のバケツ");
                }
            }
        });
    }
}
