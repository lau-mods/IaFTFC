package net.claustra01.iaftfc.gametest;

import com.iafenvoy.iceandfire.data.BestiaryPage;
import com.iafenvoy.iceandfire.item.block.entity.LecternBlockEntity;
import com.iafenvoy.iceandfire.item.component.BestiaryPageComponent;
import com.iafenvoy.iceandfire.registry.IafDataComponents;
import com.iafenvoy.iceandfire.registry.IafRegistries;
import com.iafenvoy.iceandfire.recipe.DragonForgeRecipe;
import com.iafenvoy.iceandfire.screen.menu.LecternMenu;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.fml.ModList;

@GameTestHolder("iaftfc")
@PrefixGameTestTemplate(false)
public final class BestiaryGameTests {
    private static final List<String> ORIGINAL_PAGES = List.of("introduction", "firedragon", "firedragonegg",
        "icedragon", "icedragonegg", "lightningdragon", "lightningdragonegg", "tameddragons",
        "materials", "alchemy", "dragonforge", "hippogryph", "gorgon", "pixie", "cyclops", "siren",
        "hippocampus", "deathworm", "cockatrice", "stymphalianbird", "troll", "amphithere",
        "seaserpent", "dread_mobs", "ghost");

    @GameTest(template = "empty")
    public static void allPagesAndLectern(GameTestHelper helper) {
        List<String> registered = IafRegistries.BESTIARY_PAGE.stream().map(BestiaryPage::name).toList();
        helper.assertTrue(registered.equals(ORIGINAL_PAGES), "IaF page order or contents changed: " + registered);

        Recipe<CraftingInput> bookRecipe = crafting(helper, "iceandfire:bestiary");
        ItemStack manuscript = item("iceandfire:manuscript");
        CraftingInput threeManuscripts = CraftingInput.of(2, 2,
            List.of(manuscript.copy(), manuscript.copy(), manuscript.copy(), ItemStack.EMPTY));
        helper.assertTrue(bookRecipe.matches(threeManuscripts, helper.getLevel()),
            "Three manuscripts no longer craft a Bestiary");
        ItemStack book = bookRecipe.assemble(threeManuscripts, helper.getLevel().registryAccess());
        helper.assertTrue(book.is(item("iceandfire:bestiary").getItem()) && book.getCount() == 1,
            "Bestiary crafting result changed");
        BestiaryPageComponent startingPages = book.get(IafDataComponents.BESTIARY_PAGES.get());
        helper.assertTrue(startingPages != null && startingPages.pages().size() == 1
            && startingPages.pages().getFirst().name().equals("introduction"),
            "New Bestiary does not start with Introduction");

        Recipe<CraftingInput> lecternRecipe = crafting(helper, "iceandfire:lectern");
        CraftingInput tfcPlanks = CraftingInput.of(1, 3,
            List.of(manuscript.copy(), item("tfc:wood/planks/oak"), item("tfc:wood/planks/oak")));
        helper.assertTrue(lecternRecipe.matches(tfcPlanks, helper.getLevel())
            && lecternRecipe.assemble(tfcPlanks, helper.getLevel().registryAccess())
                .is(item("iceandfire:lectern").getItem()),
            "TFC planks cannot craft the IaF Lectern");

        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("iceandfire:lectern")));
        LecternBlockEntity lectern = (LecternBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        lectern.setItem(0, book);
        var player = FakePlayerFactory.getMinecraft(helper.getLevel());
        LecternMenu menu = new LecternMenu(1, lectern, player.getInventory(), lectern.propertyDelegate);
        var unlocked = new HashSet<BestiaryPage>();
        unlocked.add(startingPages.pages().getFirst());
        while (unlocked.size() < ORIGINAL_PAGES.size()) {
            lectern.setItem(1, manuscript.copyWithCount(3));
            List<BestiaryPage> remaining = new ArrayList<>(IafRegistries.BESTIARY_PAGE.stream().toList());
            remaining.removeAll(unlocked);
            BestiaryPage[] choices = menu.getPossiblePages();
            int shown = 0;
            for (BestiaryPage choice : choices) {
                if (choice == null) continue;
                helper.assertTrue(remaining.contains(choice), "Lectern offered an already unlocked page");
                shown++;
            }
            helper.assertTrue(shown == Math.min(3, remaining.size()),
                "Lectern filtered its three original candidates: " + shown);
            BestiaryPage selected = choices[0];
            helper.assertTrue(selected != null && menu.clickMenuButton(player, 0),
                "Lectern could not unlock a selected page");
            helper.assertTrue(lectern.getItem(1).isEmpty(), "Unlock did not consume exactly three manuscripts");
            helper.assertTrue(book.get(IafDataComponents.BESTIARY_PAGES.get()).pages().contains(selected),
                "Selected page was not saved to the Bestiary");
            unlocked.add(selected);
        }
        helper.assertTrue(unlocked.size() == 25 && BestiaryPage.possiblePages(book).isEmpty(),
            "Not every dragon and non-dragon page can be unlocked");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void updatedBestiaryGuidance(GameTestHelper helper) throws IOException {
        for (String locale : List.of("en_us", "ja_jp", "es_es", "pt_br", "ru_ru", "uk_ua", "zh_cn")) {
            String forge = page(locale, "dragonforge_2");
            String metal = page(locale, "dragonforge_3");
            String armor = page(locale, "tameddragons_3");
            helper.assertTrue(!forge.contains("minecraft:iron_ingot")
                && !forge.contains("iceandfire:dragonsteel_fire_ingot"),
                "Forge page still depicts the old iron conversion in " + locale);
            String brick = page(locale, "dragonforge_0");
            helper.assertTrue(brick.contains("tfc:rock/bricks/granite")
                && brick.contains("tfc:metal/ingot/wrought_iron"),
                "Forge construction page depicts old materials in " + locale);
            helper.assertTrue(!armor.isBlank() && !metal.isBlank(), "Missing metal or armor guidance in " + locale);
            List<String> names = new ArrayList<>(List.of("dragonforge_0", "dragonforge_2",
                "dragonforge_3", "tameddragons_3"));
            if (!locale.equals("ja_jp")) names.add("lightningdragon_5");
            for (String name : names) {
                String content = page(locale, name);
                var ids = Pattern.compile("[a-z0-9_]+:[a-z0-9_./]+(?:_[a-z0-9]+)*").matcher(content);
                while (ids.find()) {
                    String id = ids.group();
                    if (id.equals("minecraft:air")) continue;
                    ResourceLocation key = ResourceLocation.parse(id);
                    helper.assertTrue(BuiltInRegistries.ITEM.containsKey(key),
                        "Unknown drawn item " + id + " in " + locale + "/" + name);
                }
            }
            var items = Pattern.compile("<item><([^ ]+)").matcher(forge);
            List<String> drawn = new ArrayList<>();
            while (items.find()) drawn.add(items.group(1));
            helper.assertTrue(drawn.size() == 9, "Forge page must draw three input/blood/result triples");
            String[] elements = {"fire", "ice", "lightning"};
            String[] steels = {"red_steel", "blue_steel", "black_steel"};
            for (int index = 0; index < elements.length; index++) {
                String element = elements[index];
                DragonForgeRecipe recipe = (DragonForgeRecipe) helper.getLevel().getRecipeManager()
                    .byKey(ResourceLocation.parse("iceandfire:dragonforge/dragonsteel_" + element + "_ingot"))
                    .orElseThrow().value();
                String input = "tfc:metal/ingot/" + steels[index];
                String blood = "iceandfire:" + element + "_dragon_blood";
                String result = "iaftfc:metal/ingot/dragonsteel_" + element;
                helper.assertTrue(drawn.subList(index * 3, index * 3 + 3).equals(List.of(input, blood, result))
                    && recipe.getInput().getItems().length == 1
                    && recipe.getInput().getItems()[0].is(item(input).getItem())
                    && recipe.getBlood().getItems().length == 1
                    && recipe.getBlood().getItems()[0].is(item(blood).getItem())
                    && recipe.getResultItem().is(item(result).getItem()),
                    "Drawn Forge triple differs from recipe in " + locale + ": " + element);
            }
        }
        helper.succeed();
    }

    private static String page(String locale, String name) throws IOException {
        String path = "assets/iceandfire/lang/bestiary/" + locale + "_0/" + name + ".txt";
        var resource = ModList.get().getModFileById("iaftfc").getFile().findResource(path);
        if (!java.nio.file.Files.exists(resource)) throw new IOException("Missing Bestiary page: " + path);
        return java.nio.file.Files.readString(resource, StandardCharsets.UTF_8);
    }

    private static ItemStack item(String id) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
    }

    @SuppressWarnings("unchecked")
    private static Recipe<CraftingInput> crafting(GameTestHelper helper, String id) {
        var holder = helper.getLevel().getRecipeManager().byKey(ResourceLocation.parse(id)).orElseThrow();
        helper.assertTrue(holder.value().getType() == RecipeType.CRAFTING, "Not a crafting recipe: " + id);
        return (Recipe<CraftingInput>) holder.value();
    }
}
