package net.claustra01.iaftfc.gametest;

import java.io.File;
import javax.xml.parsers.ParserConfigurationException;
import net.minecraft.gametest.framework.GlobalTestReporter;
import net.minecraft.gametest.framework.JUnitLikeTestReporter;

final class GameTestReports {
    static void configure() {
        String report = System.getProperty("iaftfc.gametestReport");
        if (report != null) {
            try {
                GlobalTestReporter.replaceWith(new JUnitLikeTestReporter(new File(report)));
            } catch (ParserConfigurationException exception) {
                throw new IllegalStateException("Cannot create GameTest report", exception);
            }
        }
    }

    private GameTestReports() {}
}
