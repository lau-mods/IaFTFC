package net.claustra01.iaftfc;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FoodSizeDamageDataTest {
    @Test
    void sizeGroupsMatchAdoptedItems() {
        JsonObject fixture = resource("m07-iaf-2.1.4.json");
        Set<String> publicIds = strings(fixture.getAsJsonArray("publicItemModels"));
        for (var group : fixture.getAsJsonObject("sizeGroups").entrySet()) {
            String name = group.getKey();
            JsonObject definition = resource("data/iaftfc/tfc/item_size/" + name + ".json");
            assertEquals("iaftfc:item_size/" + name, definition.getAsJsonObject("ingredient").get("tag").getAsString());
            JsonArray entries = resource("data/iaftfc/tags/item/item_size/" + name + ".json").getAsJsonArray("values");
            Set<String> actual = new HashSet<>();
            for (var value : entries) {
                JsonObject entry = value.getAsJsonObject();
                String id = entry.get("id").getAsString();
                assertFalse(entry.get("required").getAsBoolean(), id);
                assertTrue(publicIds.contains(id), id);
                assertTrue(actual.add(id), id);
            }
            assertEquals(strings(group.getValue().getAsJsonArray()), actual, name);
        }
    }

    @Test
    void resistanceGroupsMatchAdoptedItemsAndValues() {
        JsonObject fixture = resource("m07-iaf-2.1.4.json");
        Set<String> publicIds = strings(fixture.getAsJsonArray("publicItemModels"));
        for (var group : fixture.getAsJsonObject("resistanceGroups").entrySet()) {
            String name = group.getKey();
            JsonObject expected = group.getValue().getAsJsonObject();
            JsonObject data = resource("data/iaftfc/tfc/item_damage_resistance/" + name + ".json");
            assertEquals("iaftfc:item_damage_resistance/" + name, data.getAsJsonObject("ingredient").get("tag").getAsString());
            for (String field : new String[] {"piercing", "slashing", "crushing"}) {
                assertEquals(expected.get(field).getAsDouble(), data.get(field).getAsDouble(), name + " " + field);
            }
            JsonArray entries = resource("data/iaftfc/tags/item/item_damage_resistance/" + name + ".json").getAsJsonArray("values");
            Set<String> actual = new HashSet<>();
            for (var value : entries) {
                JsonObject entry = value.isJsonObject() ? value.getAsJsonObject() : null;
                String id = entry == null ? value.getAsString() : entry.get("id").getAsString();
                if (id.startsWith("iceandfire:")) {
                    assertNotNull(entry, id);
                    assertFalse(entry.get("required").getAsBoolean(), id);
                    assertTrue(publicIds.contains(id), id);
                }
                assertTrue(actual.add(id), id);
            }
            assertEquals(strings(expected.getAsJsonArray("items")), actual, name);
        }
    }

    private static Set<String> strings(JsonArray values) {
        Set<String> result = new HashSet<>();
        values.forEach(value -> assertTrue(result.add(value.getAsString()), value.toString()));
        return result;
    }

    private static JsonObject resource(String name) {
        try (var input = FoodSizeDamageDataTest.class.getClassLoader().getResourceAsStream(name)) {
            assertNotNull(input, name);
            return JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException exception) {
            throw new AssertionError(exception);
        }
    }
}
