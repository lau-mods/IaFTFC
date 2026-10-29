package net.claustra01.iaftfc.gametest;

import com.iafenvoy.iceandfire.data.DragonArmorPart;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.item.DragonArmorItem;
import com.iafenvoy.iceandfire.registry.IafEntities;
import com.iafenvoy.iceandfire.screen.slot.DragonArmorSlot;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.neoforged.neoforge.gametest.GameTestHolder;

@GameTestHolder("iaftfc")
public final class DragonArmorTests {
    static {
        GameTestReports.configure();
    }

    private static final String[] MATERIALS = {"iron", "steel", "black_steel", "red_steel", "blue_steel", "dragonsteel_fire", "dragonsteel_ice", "dragonsteel_lightning"};
    private static final String[] SHEETS = {"wrought_iron", "steel", "black_steel", "red_steel", "blue_steel", "dragonsteel_fire", "dragonsteel_ice", "dragonsteel_lightning"};
    private static final String[] PARTS = {"head", "neck", "body", "tail"};
    private static final String[][] PATTERNS = {{"   ", " ##", "###"}, {"   ", "###", " ##"}, {"###", "###", "# #"}, {"   ", "  #", "## "}};
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    @GameTest(template = "empty")
    public static void craftsAllMaterialsAndParts(GameTestHelper test) {
        for (int material = 0; material < MATERIALS.length; material++) {
            String prefix = material == 0 || material >= 5 ? "iceandfire:dragonarmor_" : "iaftfc:dragonarmor_";
            String name = material >= 5 ? "dragon_steel_" + MATERIALS[material].substring(12) : MATERIALS[material];
            for (int part = 0; part < PARTS.length; part++) {
                String id = prefix + name + "_" + PARTS[part];
                String recipeId = material == 0 || material >= 5 ? id : "iaftfc:dragonarmor/dragonarmor_" + name + "_" + PARTS[part];
                CraftingRecipe recipe = (CraftingRecipe) test.getLevel().getRecipeManager().byKey(ResourceLocation.parse(recipeId)).orElseThrow().value();
                List<ItemStack> grid = grid(PATTERNS[part], sheet(SHEETS[material]));
                CraftingInput input = CraftingInput.of(3, 3, grid);
                if (!recipe.matches(input, test.getLevel())) test.fail("Missing crafting pattern " + id);
                ItemStack result = recipe.assemble(input, test.getLevel().registryAccess());
                if (!result.is(item(id).getItem()) || result.getCount() != 1) test.fail("Wrong crafting result " + id);
                int count = (int) grid.stream().filter(stack -> !stack.isEmpty()).count();
                if (count != new int[] {5, 5, 8, 3}[part]) test.fail("Wrong material count " + id);
                List<ItemStack> wrong = new ArrayList<>(grid);
                int slot = 0;
                while (wrong.get(slot).isEmpty()) slot++;
                ItemStack singleSheet = item((material >= 5 ? "iaftfc" : "tfc") + ":metal/sheet/" + SHEETS[material]);
                if (singleSheet.isEmpty()) test.fail("Single sheet missing " + id);
                wrong.set(slot, singleSheet);
                if (recipe.matches(CraftingInput.of(3, 3, wrong), test.getLevel())) test.fail("Single sheet accepted " + id);
                wrong.set(slot, sheet(SHEETS[(material + 1) % SHEETS.length]));
                if (recipe.matches(CraftingInput.of(3, 3, wrong), test.getLevel())) test.fail("Mixed material accepted " + id);
                wrong.set(slot, ItemStack.EMPTY);
                if (recipe.matches(CraftingInput.of(3, 3, wrong), test.getLevel())) test.fail("Missing sheet accepted " + id);
            }
        }
        test.succeed();
    }

    @GameTest(template = "empty")
    public static void equipsAndSavesProtection(GameTestHelper test) {
        DragonBaseEntity dragon = IafEntities.FIRE_DRAGON.get().create(test.getLevel());
        if (dragon == null) test.fail("Dragon unavailable");
        dragon.refreshDirtyAttributes();
        String[] materials = {"steel", "black_steel", "red_steel", "blue_steel"};
        double[] protection = {3.5, 5.5, 7.5, 7.5};
        for (int material = 0; material < materials.length; material++) {
            for (int part = 0; part < PARTS.length; part++) {
                ItemStack stack = item("iaftfc:dragonarmor_" + materials[material] + "_" + PARTS[part]);
                if (!(stack.getItem() instanceof DragonArmorItem armor) || armor.dragonSlot != DragonArmorPart.values()[part]
                    || armor.type.protection() != protection[material]) test.fail("Wrong armor properties " + materials[material] + " " + PARTS[part]);
                for (int other = 0; other < PARTS.length; other++) {
                    DragonArmorSlot slot = new DragonArmorSlot(new SimpleContainer(1), 0, 0, 0, DragonArmorPart.values()[other]);
                    if (slot.mayPlace(stack) != (part == other)) test.fail("Wrong armor slot accepted " + materials[material] + " " + PARTS[part]);
                }
                double baseArmor = armorModifier(dragon);
                ItemStack displaced = dragon.getItemBySlot(SLOTS[(part + 1) % PARTS.length]).copy();
                dragon.setItemSlot(SLOTS[(part + 1) % PARTS.length], ItemStack.EMPTY);
                dragon.refreshDirtyAttributes();
                double emptySlotArmor = armorModifier(dragon);
                dragon.setItemSlot(SLOTS[(part + 1) % PARTS.length], stack);
                dragon.refreshDirtyAttributes();
                if (Math.abs(armorModifier(dragon) - emptySlotArmor) > 0.0001) test.fail("Wrong slot provided protection " + materials[material] + " " + PARTS[part] + ": " + emptySlotArmor + " -> " + armorModifier(dragon));
                dragon.setItemSlot(SLOTS[(part + 1) % PARTS.length], displaced);
                dragon.setItemSlot(SLOTS[part], stack);
                dragon.refreshDirtyAttributes();
                if (Math.abs(armorModifier(dragon) - baseArmor - protection[material]) > 0.0001) test.fail("Wrong armor contribution " + materials[material]);
                if (!dragon.getItemBySlot(SLOTS[part]).is(stack.getItem())) test.fail("Armor not equipped");
                CompoundTag saved = new CompoundTag();
                dragon.addAdditionalSaveData(saved);
                DragonBaseEntity restored = IafEntities.FIRE_DRAGON.get().create(test.getLevel());
                if (restored == null) test.fail("Dragon restore unavailable");
                restored.readAdditionalSaveData(saved);
                if (!restored.getItemBySlot(SLOTS[part]).is(stack.getItem())) test.fail("Armor not saved");
                restored.refreshDirtyAttributes();
                if (Math.abs(armorModifier(restored) - armorModifier(dragon)) > 0.0001) test.fail("Armor contribution not saved");
            }
            for (int part = 0; part < PARTS.length; part++) dragon.setItemSlot(SLOTS[part], ItemStack.EMPTY);
        }
        test.succeed();
    }

    private static List<ItemStack> grid(String[] pattern, ItemStack sheet) {
        List<ItemStack> result = new ArrayList<>();
        for (String row : pattern) for (char cell : row.toCharArray()) result.add(cell == '#' ? sheet.copy() : ItemStack.EMPTY);
        return result;
    }

    private static ItemStack sheet(String metal) {
        return item(metal.startsWith("dragonsteel_") ? "iaftfc:metal/double_sheet/" + metal : "tfc:metal/double_sheet/" + metal);
    }

    private static ItemStack item(String id) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
    }

    private static double armorModifier(DragonBaseEntity dragon) {
        return dragon.getAttribute(Attributes.ARMOR).getModifier(ResourceLocation.fromNamespaceAndPath("iceandfire", "armor_modifier")).amount();
    }
}
