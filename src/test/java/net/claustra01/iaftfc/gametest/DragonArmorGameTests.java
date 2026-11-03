package net.claustra01.iaftfc.gametest;

import com.iafenvoy.iceandfire.data.DragonArmorPart;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.item.DragonArmorItem;
import com.iafenvoy.iceandfire.registry.IafCreativeModeTabs;
import com.iafenvoy.iceandfire.screen.slot.DragonArmorSlot;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.claustra01.iaftfc.dragonarmor.DragonArmor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.dries007.tfc.common.component.size.ItemSizeManager;
import net.dries007.tfc.common.component.size.Size;
import net.dries007.tfc.common.component.size.Weight;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("iaftfc")
@PrefixGameTestTemplate(false)
public final class DragonArmorGameTests {
    private static final String[] MATERIALS = {"iron", "steel", "black_steel", "red_steel", "blue_steel",
        "dragon_steel_fire", "dragon_steel_ice", "dragon_steel_lightning"};
    private static final String[] INPUTS = {"tfc:metal/double_sheet/wrought_iron", "tfc:metal/double_sheet/steel",
        "tfc:metal/double_sheet/black_steel", "tfc:metal/double_sheet/red_steel",
        "tfc:metal/double_sheet/blue_steel", "iaftfc:metal/double_sheet/dragonsteel_fire",
        "iaftfc:metal/double_sheet/dragonsteel_ice", "iaftfc:metal/double_sheet/dragonsteel_lightning"};
    private static final double[] PROTECTION = {1.5, 5, 7, 8, 8, 10, 10, 10};
    private static final String[][] PATTERNS = {{"   ", " ##", "###"}, {"   ", "###", " ##"},
        {"###", "###", "# #"}, {"   ", "  #", "## "}};

    @GameTest(template = "empty")
    public static void newDragonArmor(GameTestHelper helper) {
        recipesAndArmor(helper, 1, 5);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void existingDragonArmor(GameTestHelper helper) {
        recipesAndArmor(helper, 0, 1);
        recipesAndArmor(helper, 5, 8);
        helper.succeed();
    }

    private static void recipesAndArmor(GameTestHelper helper, int first, int end) {
        DragonBaseEntity dragon = dragon(helper);
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        dragon.refreshDirtyAttributes();
        double baseline = dragon.getAttributeValue(Attributes.ARMOR);
        for (int material = first; material < end; material++) {
            for (int part = 0; part < 4; part++) {
                String id = (material == 0 || material > 4 ? "iceandfire:" : "iaftfc:")
                    + "dragonarmor_" + MATERIALS[material] + "_" + DragonArmor.PARTS[part];
                ItemStack output = item(id);
                helper.assertTrue(output.getItem() instanceof DragonArmorItem armor
                    && armor.dragonSlot == DragonArmorPart.values()[part]
                    && armor.type.protection() == PROTECTION[material], "Wrong armor material or part: " + id);
                if (material > 0 && material < 5) {
                    DragonArmorItem armor = (DragonArmorItem) output.getItem();
                    helper.assertTrue(armor.type.name().equals(MATERIALS[material])
                        && !armor.type.fireProof() && !armor.type.dragonSteel()
                        && output.getDescriptionId().equals("item.iaftfc.dragonarmor_"
                            + MATERIALS[material] + "_" + DragonArmor.PARTS[part]),
                        "New armor material or translation differs: " + id);
                }
                Recipe<CraftingInput> recipe = crafting(helper, id);
                CraftingInput sheets = grid(PATTERNS[part], INPUTS[material]);
                helper.assertTrue(recipe.matches(sheets, helper.getLevel())
                    && recipe.assemble(sheets, helper.getLevel().registryAccess()).is(output.getItem()),
                    "Double sheets cannot craft " + id);
                CraftingInput blocks = grid(PATTERNS[part], material > 4
                    ? "iceandfire:dragonsteel_" + MATERIALS[material].substring("dragon_steel_".length()) + "_block"
                    : "minecraft:iron_block");
                helper.assertTrue(!recipe.matches(blocks, helper.getLevel()), "Old blocks still craft " + id);
                for (int other = 0; other < INPUTS.length; other++) {
                    if (other == material) continue;
                    CraftingInput mixed = mixedGrid(PATTERNS[part], INPUTS[material], INPUTS[other]);
                    helper.assertTrue(!recipe.matches(mixed, helper.getLevel()), "Mixed metal crafted " + id);
                }
                helper.assertTrue(ItemSizeManager.get(output).getSize(output)
                    == Size.LARGE
                    && ItemSizeManager.get(output).getWeight(output)
                    == Weight.MEDIUM, "Wrong dragon armor size or weight: " + id);
                DragonArmorSlot slot = new DragonArmorSlot(new SimpleContainer(1), 0, 0, 0,
                    DragonArmorPart.values()[part]);
                helper.assertTrue(slot.mayPlace(output), "Armor cannot enter dragon slot: " + id);
                dragon.setItemSlot(slots[part], output.copy());
                dragon.refreshDirtyAttributes();
                helper.assertTrue(Math.abs(dragon.getAttributeValue(Attributes.ARMOR)
                    - Math.min(30, baseline + PROTECTION[material] * (part + 1))) < 0.00001,
                    "Actual dragon armor attribute differs: " + id + " actual="
                        + dragon.getAttributeValue(Attributes.ARMOR) + " baseline=" + baseline);
            }
            for (EquipmentSlot slot : slots) dragon.setItemSlot(slot, ItemStack.EMPTY);
        }
    }

    @GameTest(template = "empty")
    public static void disabledDragonArmor(GameTestHelper helper) throws IOException {
        DragonBaseEntity dragon = dragon(helper);
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        dragon.refreshDirtyAttributes();
        double baseline = dragon.getAttributeValue(Attributes.ARMOR);
        for (String material : DragonArmor.DISABLED_METALS) {
            for (int part = 0; part < 4; part++) {
                String id = "iceandfire:dragonarmor_" + material + "_" + DragonArmor.PARTS[part];
                ItemStack stack = item(id);
                helper.assertTrue(ItemSizeManager.get(stack).getSize(stack)
                    == Size.LARGE
                    && ItemSizeManager.get(stack).getWeight(stack)
                    == Weight.MEDIUM, "Wrong legacy armor size or weight: " + id);
                helper.assertTrue(helper.getLevel().getRecipeManager().byKey(ResourceLocation.parse(id)).isEmpty(),
                    "Disabled recipe still loads: " + id);
                DragonArmorSlot slot = new DragonArmorSlot(new SimpleContainer(1), 0, 0, 0,
                    DragonArmorPart.values()[part]);
                helper.assertTrue(!slot.mayPlace(stack), "Disabled armor can be equipped: " + id);
                CompoundTag saved = (CompoundTag) stack.save(helper.getLevel().registryAccess());
                ItemStack loaded = ItemStack.parseOptional(helper.getLevel().registryAccess(), saved);
                helper.assertTrue(loaded.is(stack.getItem()) && loaded.getCount() == 1,
                    "Old armor did not survive NBT round trip: " + id);
                dragon.setItemSlot(slots[part], loaded);
                dragon.refreshDirtyAttributes();
                helper.assertTrue(Math.abs(dragon.getAttributeValue(Attributes.ARMOR) - baseline) < 0.00001,
                    "Disabled armor applied protection: " + id);
                CompoundTag dragonData = dragon.saveWithoutId(new CompoundTag());
                DragonBaseEntity restored = (DragonBaseEntity) dragon.getType().create(helper.getLevel());
                restored.load(dragonData);
                helper.assertTrue(restored.getItemBySlot(slots[part]).is(stack.getItem()),
                    "Already-equipped armor disappeared on load: " + id);
                helper.assertTrue(Math.abs(restored.getAttributeValue(Attributes.ARMOR) - baseline) < 0.00001,
                    "Already-equipped armor applied protection after load: " + id);
                DragonArmorSlot equipped = new DragonArmorSlot(restored.dragonInventory, part + 1, 0, 0,
                    DragonArmorPart.values()[part]);
                helper.assertTrue(equipped.mayPickup(FakePlayerFactory.getMinecraft(helper.getLevel())),
                    "Old armor cannot be picked up: " + id);
                helper.assertTrue(equipped.remove(1).is(stack.getItem())
                    && restored.getItemBySlot(slots[part]).isEmpty(),
                    "Old armor cannot be removed: " + id);
                dragon.setItemSlot(slots[part], ItemStack.EMPTY);
            }
        }
        for (CreativeModeTab creativeTab : BuiltInRegistries.CREATIVE_MODE_TAB) {
            creativeTab.buildContents(new CreativeModeTab.ItemDisplayParameters(FeatureFlags.DEFAULT_FLAGS,
                true, helper.getLevel().registryAccess()));
            helper.assertTrue(creativeTab.getDisplayItems().stream().noneMatch(DragonArmor::disabled)
                && creativeTab.getSearchTabDisplayItems().stream().noneMatch(DragonArmor::disabled),
                "Disabled dragon armor remains in Creative inventory");
        }
        CreativeModeTab tab = IafCreativeModeTabs.ARMORS.get();
        for (String metal : DragonArmor.METALS) {
            for (String part : DragonArmor.PARTS) {
                ItemStack expected = item("iaftfc:dragonarmor_" + metal + "_" + part);
                helper.assertTrue(tab.getDisplayItems().stream().anyMatch(stack -> stack.is(expected.getItem())),
                    "New dragon armor absent from Creative inventory");
            }
        }
        var resources = helper.getLevel().getServer().getResourceManager()
            .listResources("loot_table", path -> path.getPath().endsWith(".json"));
        for (var entry : resources.entrySet()) {
            try (var reader = entry.getValue().openAsReader()) {
                String loot = com.google.gson.JsonParser.parseReader(reader).toString();
                for (String material : DragonArmor.DISABLED_METALS) {
                    helper.assertTrue(!loot.contains("iceandfire:dragonarmor_" + material + "_"),
                        "Disabled armor remains in loot: " + entry.getKey());
                }
            }
        }
        for (var recipe : helper.getLevel().getRecipeManager().getRecipes()) {
            helper.assertTrue(!DragonArmor.disabled(recipe.value().getResultItem(helper.getLevel().registryAccess())),
                "Disabled dragon armor remains in recipe: " + recipe.id());
        }
        helper.succeed();
    }

    private static DragonBaseEntity dragon(GameTestHelper helper) {
        var type = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("iceandfire:fire_dragon"));
        DragonBaseEntity dragon = (DragonBaseEntity) type.create(helper.getLevel());
        dragon.moveTo(helper.absolutePos(new net.minecraft.core.BlockPos(1, 2, 1)), 0, 0);
        helper.getLevel().addFreshEntity(dragon);
        return dragon;
    }

    private static CraftingInput grid(String[] rows, String input) {
        List<ItemStack> stacks = new ArrayList<>();
        for (String row : rows) for (char cell : row.toCharArray()) {
            stacks.add(cell == '#' ? item(input) : ItemStack.EMPTY);
        }
        return CraftingInput.of(3, 3, stacks);
    }

    private static CraftingInput mixedGrid(String[] rows, String first, String second) {
        List<ItemStack> stacks = new ArrayList<>();
        boolean replaced = false;
        for (String row : rows) for (char cell : row.toCharArray()) {
            if (cell == ' ') stacks.add(ItemStack.EMPTY);
            else if (!replaced) {stacks.add(item(second)); replaced = true;}
            else stacks.add(item(first));
        }
        return CraftingInput.of(3, 3, stacks);
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
