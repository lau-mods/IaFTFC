package net.claustra01.iaftfc;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

final class MetalworkingDataTest {
    @Test
    void temperatureBoundaries() {
        JsonObject heat = resource("data/iaftfc/tfc/item_heat/dragonsteel_ingots.json");
        assertEquals(1200, heat.get("forging_temperature").getAsDouble());
        assertEquals(1600, heat.get("welding_temperature").getAsDouble());
        for (String metal : new String[] {"dragonsteel_fire", "dragonsteel_ice", "dragonsteel_lightning"}) {
            JsonObject melting = resource("data/iaftfc/recipe/heating/" + metal + "/ingot.json");
            assertEquals(2000, melting.get("temperature").getAsDouble());
        }
    }

    @Test
    void castingDefaults() {
        for (String metal : new String[] {"dragonsteel_fire", "dragonsteel_ice", "dragonsteel_lightning"}) {
            JsonObject casting = resource("data/iaftfc/recipe/casting/" + metal + "_ingot.json");
            assertEquals(0.1, casting.get("break_chance").getAsDouble());
            assertEquals(100, casting.getAsJsonObject("fluid").get("amount").getAsInt());
        }
    }

    private static JsonObject resource(String name) {
        try (var input = MetalworkingDataTest.class.getClassLoader().getResourceAsStream(name)) {
            assertNotNull(input, name);
            return JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException exception) {
            throw new AssertionError(exception);
        }
    }
}
