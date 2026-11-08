package net.claustra01.iaftfc_test.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.claustra01.iaftfc.datagen.DisabledContentData;
import net.claustra01.iaftfc.disable.DisabledContent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.common.world.NoneBiomeModifier;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("iaftfc")
@PrefixGameTestTemplate(false)
public final class DisabledItemsGameTests {
    @GameTest(template = "empty", batch = "m11", timeoutTicks = 200)
    public static void duplicateSurvivalSourcesAreStopped(GameTestHelper helper) {
        Set<String> expected = new HashSet<>(DisabledContentData.DISABLED_ITEMS);
        for (String id : expected) {
            ItemStack stack = stack("iceandfire:" + id);
            helper.assertTrue(!stack.isEmpty(), "Disabled CE item is not registered: " + id);
            helper.assertTrue(stack.is(DisabledContent.ITEMS), "Disabled CE item is missing its tag: " + id);
        }

        for (String id : List.of(
            "iceandfire:gold_pile", "iceandfire:silver_pile", "iceandfire:copper_pile",
            "iceandfire:dragonscales_silver", "iceandfire:dragonscales_sapphire",
            "iceandfire:armor_silver_helmet", "iceandfire:armor_sapphire_helmet",
            "iceandfire:dragonsteel_fire_sword", "iceandfire:dragonsteel_ice_chestplate",
            "iceandfire:dragonarmor_dragon_steel_lightning_body")) {
            helper.assertTrue(!stack(id).is(DisabledContent.ITEMS), "Usable CE content was disabled: " + id);
        }
        for (String id : List.of(
            "tfc:metal/ingot/silver", "tfc:gem/sapphire",
            "iaftfc:metal/ingot/dragonsteel_fire", "iaftfc:metal/ingot/dragonsteel_ice",
            "iaftfc:metal/ingot/dragonsteel_lightning")) {
            ItemStack replacement = stack(id);
            helper.assertTrue(!replacement.isEmpty() && !replacement.is(DisabledContent.ITEMS),
                "Replacement material is unavailable or disabled: " + id);
        }
        assertRecipeProduces(helper, "tfc:casting/silver_ingot", "tfc:metal/ingot/silver");
        assertRecipeProduces(helper, "tfc:crafting/gem/sapphire", "tfc:gem/sapphire");
        for (String element : List.of("fire", "ice", "lightning")) {
            assertRecipeProduces(helper, "iceandfire:dragonforge/dragonsteel_" + element + "_ingot",
                "iaftfc:metal/ingot/dragonsteel_" + element);
        }
        var sapphireLoot = ResourceKey.create(Registries.LOOT_TABLE,
            ResourceLocation.parse("tfc:blocks/ore/sapphire/granite"));
        helper.assertTrue(helper.getLevel().getServer().reloadableRegistries().getLootTable(sapphireLoot)
            != LootTable.EMPTY,
            "TFC sapphire ore has no loaded drop route");

        helper.getLevel().getRecipeManager().getRecipes().forEach(holder -> {
            ItemStack result = holder.value().getResultItem(helper.getLevel().registryAccess());
            helper.assertTrue(result.isEmpty() || !result.is(DisabledContent.ITEMS),
                "Recipe still produces disabled content: " + holder.id());
        });
        assertLootHasNoDisabledItems(helper, expected);
        assertOreModifierDisabled(helper, "silver_ore");
        assertOreModifierDisabled(helper, "sapphire_ore");
        assertTradesHaveNoDisabledOutputs(helper);
        helper.succeed();
    }

    private static void assertLootHasNoDisabledItems(GameTestHelper helper, Set<String> disabled) {
        var resources = helper.getLevel().getServer().getResourceManager().listResources("loot_table",
            id -> id.getNamespace().equals("iceandfire") && id.getPath().endsWith(".json"));
        resources.forEach((id, resource) -> {
            try (var reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
                JsonElement json = JsonParser.parseReader(reader);
                for (String item : disabled) {
                    helper.assertTrue(!contains(json, "iceandfire:" + item),
                        "Loot table still contains disabled content: " + id + " -> " + item);
                }
            } catch (IOException exception) {
                throw new IllegalStateException("Unable to inspect loot table " + id, exception);
            }
        });
    }

    private static boolean contains(JsonElement element, String value) {
        if (element.isJsonPrimitive()) {
            return element.getAsJsonPrimitive().isString() && element.getAsString().equals(value);
        }
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                if (contains(child, value)) {
                    return true;
                }
            }
        } else if (element.isJsonObject()) {
            for (JsonElement child : element.getAsJsonObject().asMap().values()) {
                if (contains(child, value)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void assertOreModifierDisabled(GameTestHelper helper, String id) {
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath("iceandfire",
            id);
        var modifier = helper.getLevel().registryAccess().registryOrThrow(NeoForgeRegistries.Keys.BIOME_MODIFIERS)
            .get(location);
        helper.assertTrue(modifier instanceof NoneBiomeModifier,
            "Loaded CE ore biome modifier must be disabled: " + location);
    }

    private static void assertRecipeProduces(GameTestHelper helper, String id, String output) {
        var recipe = helper.getLevel().getRecipeManager().byKey(ResourceLocation.parse(id)).orElseThrow();
        helper.assertTrue(recipe.value().getResultItem(helper.getLevel().registryAccess()).is(stack(output).getItem()),
            "Loaded survival recipe does not produce " + output + ": " + id);
    }

    private static void assertTradesHaveNoDisabledOutputs(GameTestHelper helper) {
        var player = FakePlayerFactory.getMinecraft(helper.getLevel());
        RandomSource random = RandomSource.create(0);
        for (var levels : VillagerTrades.TRADES.values()) {
            for (VillagerTrades.ItemListing[] listings : levels.values()) {
                for (VillagerTrades.ItemListing listing : listings) {
                    var offer = listing.getOffer(player, random);
                    helper.assertTrue(offer == null || !offer.getResult().is(DisabledContent.ITEMS),
                        "Villager trade still outputs disabled content");
                }
            }
        }
        for (VillagerTrades.ItemListing[] listings : VillagerTrades.WANDERING_TRADER_TRADES.values()) {
            for (VillagerTrades.ItemListing listing : listings) {
                var offer = listing.getOffer(player, random);
                helper.assertTrue(offer == null || !offer.getResult().is(DisabledContent.ITEMS),
                    "Wandering trader still outputs disabled content");
            }
        }
    }

    private static ItemStack stack(String id) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
    }

    private DisabledItemsGameTests() {
    }
}
