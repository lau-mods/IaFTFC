package net.claustra01.iaftfc_test.gametest;

import com.iafenvoy.iceandfire.data.DragonArmorPart;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.item.DragonArmorItem;
import com.iafenvoy.iceandfire.screen.menu.DragonMenu;
import com.iafenvoy.iceandfire.screen.slot.DragonArmorSlot;
import java.util.ArrayList;
import java.util.List;
import net.claustra01.iaftfc.dragonarmor.DragonArmor;
import net.dries007.tfc.common.component.size.ItemSizeManager;
import net.dries007.tfc.common.component.size.Size;
import net.dries007.tfc.common.component.size.Weight;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("iaftfc")
@PrefixGameTestTemplate(false)
public final class DragonArmorGameTests {
    private static final String EMPTY_TEMPLATE = "empty";
    private static final List<List<String>> PATTERNS = List.of(
        List.of("   ", " ##", "###"),
        List.of("   ", "###", " ##"),
        List.of("###", "###", "# #"),
        List.of("   ", "  #", "## "));
    private static final EquipmentSlot[] SLOTS = {
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };
    private static final List<String> METAL_IDS = List.of(
        "steel", "black_steel", "red_steel", "blue_steel");
    private static final List<Double> PROTECTION = List.of(3.5D, 5.5D, 7.5D, 7.5D);

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 200)
    public static void tfcDragonArmorUsesCeSlotsAndProtectionOnce(GameTestHelper helper) {
        var owner = helper.makeMockPlayer(GameType.SURVIVAL);
        DragonArmorItem iron = dragonArmor("iceandfire:dragonarmor_iron_head");
        DragonArmorItem dragonsteel = dragonArmor("iceandfire:dragonarmor_dragon_steel_fire_head");
        double steel = dragonArmor("iaftfc:dragonarmor_steel_head").type.protection();
        double blackSteel = dragonArmor("iaftfc:dragonarmor_black_steel_head").type.protection();
        double redSteel = dragonArmor("iaftfc:dragonarmor_red_steel_head").type.protection();
        double blueSteel = dragonArmor("iaftfc:dragonarmor_blue_steel_head").type.protection();
        helper.assertTrue(iron.type.protection() == 1.5D
            && dragonsteel.type.protection() == 10.0D
            && iron.type.protection() < steel && steel < blackSteel
            && blackSteel < redSteel && redSteel == blueSteel
            && blueSteel < dragonsteel.type.protection(),
            "Dragon armor strength must be Iron < Steel < Black Steel < Red/Blue Steel < Dragonsteel");

        for (int i = 0; i < METAL_IDS.size(); i++) {
            String metal = METAL_IDS.get(i);
            double protection = PROTECTION.get(i);
            equipAndAssert(helper, owner, new BlockPos(1 + i * 2, 2, 1),
                "iaftfc", metal, protection);
        }
        equipAndAssert(helper, owner, new BlockPos(9, 2, 1), "iceandfire", "iron", 1.5D);
        equipAndAssert(helper, owner, new BlockPos(11, 2, 1),
            "iceandfire", "dragon_steel_fire", 10.0D);
        helper.succeed();
    }

    private static void equipAndAssert(GameTestHelper helper, Player owner, BlockPos position,
        String namespace, String metal, double protection) {
        DragonBaseEntity dragon = dragon(helper, position);
        dragon.tame(owner);
        helper.assertTrue(dragon.isTame(), "Dragon armor must be tested on a tamed dragon");
        DragonMenu menu = new DragonMenu(0, dragon.dragonInventory, owner.getInventory(), dragon);
        double baseline = dragon.getAttributeValue(Attributes.ARMOR);

        for (DragonArmorPart part : DragonArmorPart.values()) {
            ItemStack stack = item(namespace + ":" + DragonArmor.id(metal, part));
            DragonArmorItem armor = (DragonArmorItem)stack.getItem();
            helper.assertValueEqual(armor.dragonSlot, part,
                "Each armor item must retain its CE dragon armor part");
            helper.assertValueEqual(armor.type.name(), metal,
                "Each armor item must retain its texture material name");
            helper.assertTrue(Math.abs(armor.type.protection() - protection) < 0.00001D,
                "Each armor item must use its specified protection");

            for (int slot = 0; slot < SLOTS.length; slot++) {
                helper.assertTrue(menu.getSlot(slot + 1) instanceof DragonArmorSlot,
                    "Dragon menu must use CE dragon armor slots");
                helper.assertTrue(menu.getSlot(slot + 1).mayPlace(stack) == (slot == part.ordinal()),
                    "Only the matching dragon armor slot must accept the part");
            }

            menu.getSlot(part.ordinal() + 1).set(stack);
            helper.assertTrue(dragon.getItemBySlot(SLOTS[part.ordinal()]).is(stack.getItem()),
                "The dragon must equip the item through its inventory");
            helper.assertTrue(Math.abs(dragon.getAttributeValue(Attributes.ARMOR)
                - (baseline + protection)) < 0.00001D,
                "One equipped part must add its protection exactly once");
            menu.getSlot(part.ordinal() + 1).set(ItemStack.EMPTY);
            helper.assertTrue(Math.abs(dragon.getAttributeValue(Attributes.ARMOR) - baseline) < 0.00001D,
                "Removing the part must restore the original protection");
        }

        for (DragonArmorPart part : DragonArmorPart.values()) {
            menu.getSlot(part.ordinal() + 1).set(
                item(namespace + ":" + DragonArmor.id(metal, part)));
        }
        helper.assertTrue(Math.abs(dragon.getAttributeValue(Attributes.ARMOR)
            - Math.min(30.0D, baseline + protection * SLOTS.length)) < 0.00001D,
            "Four equipped parts must use CE's single armor calculation");
        menu.removed(owner);
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 200)
    public static void dragonArmorRecipesUseTfcDoubleSheets(GameTestHelper helper) {
        for (DragonArmor.Metal metal : DragonArmor.METALS) {
            for (DragonArmorPart part : DragonArmorPart.values()) {
                String id = "iaftfc:" + DragonArmor.id(metal.id(), part);
                assertRecipe(helper, id, "tfc:metal/double_sheet/" + metal.id());
                ItemStack armor = item(id);
                helper.assertValueEqual(ItemSizeManager.get(armor).getSize(armor), Size.VERY_LARGE,
                    "Dragon armor must use TFC's very large size");
                helper.assertValueEqual(ItemSizeManager.get(armor).getWeight(armor), Weight.VERY_HEAVY,
                    "Dragon armor must use TFC's very heavy weight");
            }
        }

        List<String> existingMetals = List.of(
            "iron", "dragon_steel_fire", "dragon_steel_ice", "dragon_steel_lightning");
        List<String> inputs = List.of(
            "tfc:metal/double_sheet/wrought_iron",
            "iaftfc:metal/double_sheet/dragonsteel_fire",
            "iaftfc:metal/double_sheet/dragonsteel_ice",
            "iaftfc:metal/double_sheet/dragonsteel_lightning");
        for (int metal = 0; metal < existingMetals.size(); metal++) {
            for (DragonArmorPart part : DragonArmorPart.values()) {
                assertRecipe(helper, "iceandfire:" + DragonArmor.id(existingMetals.get(metal), part),
                    inputs.get(metal));
            }
        }
        for (String metal : DragonArmor.DISABLED_METALS) {
            for (DragonArmorPart part : DragonArmorPart.values()) {
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                    "iceandfire", DragonArmor.id(metal, part));
                helper.assertTrue(helper.getLevel().getRecipeManager().byKey(id).isEmpty(),
                    "Obsolete CE dragon armor recipe must be disabled: " + id);
            }
        }
        helper.succeed();
    }

    private static void assertRecipe(GameTestHelper helper, String id, String ingredient) {
        Recipe<CraftingInput> recipe = crafting(helper, id);
        CraftingInput input = grid(PATTERNS.get(dragonArmor(id).dragonSlot.ordinal()), ingredient);
        helper.assertTrue(recipe.matches(input, helper.getLevel()),
            "Matching TFC double sheets must craft " + id);
        helper.assertTrue(recipe.assemble(input, helper.getLevel().registryAccess()).is(item(id).getItem()),
            "Dragon armor recipe must preserve its registered output ID: " + id);
    }

    private static DragonBaseEntity dragon(GameTestHelper helper, BlockPos position) {
        var type = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("iceandfire:fire_dragon"));
        DragonBaseEntity dragon = (DragonBaseEntity)type.create(helper.getLevel());
        dragon.moveTo(helper.absolutePos(position), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(dragon);
        return dragon;
    }

    private static CraftingInput grid(List<String> pattern, String ingredient) {
        List<ItemStack> stacks = new ArrayList<>();
        for (String row : pattern) {
            for (char cell : row.toCharArray()) {
                stacks.add(cell == '#' ? item(ingredient) : ItemStack.EMPTY);
            }
        }
        return CraftingInput.of(3, 3, stacks);
    }

    private static DragonArmorItem dragonArmor(String id) {
        return (DragonArmorItem)item(id).getItem();
    }

    private static ItemStack item(String id) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
    }

    @SuppressWarnings("unchecked")
    private static Recipe<CraftingInput> crafting(GameTestHelper helper, String id) {
        var holder = helper.getLevel().getRecipeManager().byKey(ResourceLocation.parse(id)).orElseThrow();
        helper.assertValueEqual(holder.value().getType(), RecipeType.CRAFTING,
            "Dragon armor must use a crafting recipe");
        return (Recipe<CraftingInput>)holder.value();
    }

    private DragonArmorGameTests() {
    }
}
