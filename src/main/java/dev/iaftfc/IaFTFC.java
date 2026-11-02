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

@Mod(IaFTFC.MOD_ID)
public final class IaFTFC {
    public static final String MOD_ID = "iaftfc";

    public IaFTFC(IEventBus modBus) {
        LogUtils.getLogger().info("IaFTFC loaded on {}", FMLEnvironment.dist);
        modBus.addListener(this::gatherData);
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
        event.getGenerator().addProvider(event.includeClient(), new LanguageProvider(event.getGenerator().getPackOutput(), MOD_ID, "en_us") {
            @Override
            protected void addTranslations() {
                add("fml.menu.mods.info.description.iaftfc", "Connects Ice and Fire dragons with TerraFirmaCraft.");
            }
        });
        event.getGenerator().addProvider(event.includeClient(), new LanguageProvider(event.getGenerator().getPackOutput(), MOD_ID, "ja_jp") {
            @Override
            protected void addTranslations() {
                add("fml.menu.mods.info.description.iaftfc", "Ice and Fire のドラゴンを TerraFirmaCraft に接続します。");
            }
        });
    }
}
