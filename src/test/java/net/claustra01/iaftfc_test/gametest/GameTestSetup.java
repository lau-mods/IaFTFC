package net.claustra01.iaftfc_test.gametest;

import java.io.File;
import javax.xml.parsers.ParserConfigurationException;
import net.minecraft.gametest.framework.GlobalTestReporter;
import net.minecraft.gametest.framework.JUnitLikeTestReporter;
import net.neoforged.fml.common.Mod;

@Mod("iaftfc_test")
public final class GameTestSetup {
    public GameTestSetup() {
        String path = System.getProperty("iaftfc.gametest.report");
        if (path != null) {
            File report = new File(path);
            report.getParentFile().mkdirs();
            try {
                GlobalTestReporter.replaceWith(new JUnitLikeTestReporter(report));
            } catch (ParserConfigurationException exception) {
                throw new IllegalStateException("Unable to create the GameTest report", exception);
            }
        }
    }
}
