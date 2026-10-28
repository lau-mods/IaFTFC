package net.claustra01.iaftfc.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.mojang.authlib.GameProfile;
import net.dries007.tfc.common.blockentities.AnvilBlockEntity;
import net.dries007.tfc.common.component.forge.ForgeStep;
import net.dries007.tfc.common.component.forge.ForgingBonus;
import net.dries007.tfc.common.component.forge.ForgingBonusComponent;
import net.dries007.tfc.common.component.forge.ForgingCapability;
import net.dries007.tfc.common.component.heat.HeatCapability;
import net.dries007.tfc.common.component.mold.IMold;
import net.dries007.tfc.common.items.MoldItem;
import net.dries007.tfc.common.recipes.AnvilRecipe;
import net.dries007.tfc.common.recipes.CastingRecipe;
import net.dries007.tfc.common.recipes.HeatingRecipe;
import net.dries007.tfc.common.recipes.WeldingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;

@GameTestHolder("iaftfc")
public final class MetalworkingTests {
    static {
        GameTestReports.configure();
    }

    private static final String[] METALS = {"dragonsteel_fire", "dragonsteel_ice", "dragonsteel_lightning"};
    private static final String[] FORMS = {"axe_head", "hoe_head", "pickaxe_head", "shovel_head", "sword_blade", "rod", "sheet", "unfinished_helmet", "unfinished_chestplate", "unfinished_greaves", "unfinished_boots"};
    private static final String[] SOURCES = {"ingot", "ingot", "ingot", "ingot", "double_ingot", "ingot", "double_ingot", "double_sheet", "double_sheet", "double_sheet", "sheet"};
    private static final String[][] RULES = {
        {"punch_last", "hit_second_last", "upset_third_last"},
        {"punch_last", "hit_not_last", "bend_not_last"},
        {"punch_last", "draw_not_last", "bend_not_last"},
        {"punch_last", "hit_not_last"},
        {"hit_last", "bend_second_last", "bend_third_last"},
        {"hit_last", "hit_second_last", "hit_third_last"},
        {"hit_third_last", "hit_second_last", "hit_last"},
        {"hit_last", "bend_second_last", "bend_third_last"},
        {"hit_last", "hit_second_last", "upset_third_last"},
        {"hit_any", "draw_any", "bend_any"},
        {"bend_last", "bend_second_last", "shrink_third_last"}
    };
    private static final ForgeStep[][] STEPS = {
        {ForgeStep.UPSET, ForgeStep.HIT_LIGHT, ForgeStep.PUNCH},
        {ForgeStep.BEND, ForgeStep.HIT_LIGHT, ForgeStep.PUNCH},
        {ForgeStep.BEND, ForgeStep.DRAW, ForgeStep.PUNCH},
        {ForgeStep.HIT_LIGHT, ForgeStep.PUNCH},
        {ForgeStep.BEND, ForgeStep.BEND, ForgeStep.HIT_LIGHT},
        {ForgeStep.HIT_LIGHT, ForgeStep.HIT_LIGHT, ForgeStep.HIT_LIGHT},
        {ForgeStep.HIT_LIGHT, ForgeStep.HIT_LIGHT, ForgeStep.HIT_LIGHT},
        {ForgeStep.BEND, ForgeStep.BEND, ForgeStep.HIT_LIGHT},
        {ForgeStep.UPSET, ForgeStep.HIT_LIGHT, ForgeStep.HIT_LIGHT},
        {ForgeStep.HIT_LIGHT, ForgeStep.DRAW, ForgeStep.BEND},
        {ForgeStep.SHRINK, ForgeStep.BEND, ForgeStep.BEND}
    };
    private static final String[] HEAT_FORMS = {"ingot", "axe_head", "hoe_head", "pickaxe_head", "shovel_head", "rod", "double_ingot", "sheet", "sword_blade", "unfinished_boots", "double_sheet", "unfinished_helmet", "unfinished_chestplate", "unfinished_greaves", "block", "block_slab", "block_stairs", "anvil", "axe", "hoe", "pickaxe", "shovel", "sword", "helmet", "leggings", "chestplate", "boots"};
    private static final int[] AMOUNTS = {100, 100, 100, 100, 100, 50, 200, 200, 200, 200, 400, 400, 400, 400, 100, 50, 75, 1400, 100, 100, 100, 100, 200, 600, 600, 800, 400};

    @GameTest(template = "empty")
    public static void reachesTierSeven(GameTestHelper test) {
        for (String metal : METALS) {
            WeldingRecipe weld = recipe(test, "iaftfc:welding/double_ingot/" + metal, WeldingRecipe.class);
            if (!weld.isCorrectTier(6) || weld.isCorrectTier(5)) test.fail("Double ingot tier " + metal);
            if (!weld.getFirstInput().test(item("iaftfc:metal/ingot/" + metal)) || !weld.getSecondInput().test(item("iaftfc:metal/ingot/" + metal))) test.fail("Ingot inputs " + metal);
            for (String other : METALS) if (!other.equals(metal) && (weld.getFirstInput().test(item("iaftfc:metal/ingot/" + other)) || weld.getSecondInput().test(item("iaftfc:metal/ingot/" + other)))) test.fail("Mixed metal weld " + metal);
            BlockPos pos = new BlockPos(1, 1, 1);
            test.setBlock(pos, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("tfc:metal/anvil/blue_steel")));
            AnvilBlockEntity anvilBlock = (AnvilBlockEntity) test.getBlockEntity(pos);
            List<ItemStack> doubleIngots = new ArrayList<>();
            for (int count = 0; count < 7; count++) {
                ItemStack first = item("iaftfc:metal/ingot/" + metal);
                ItemStack second = item("iaftfc:metal/ingot/" + metal);
                HeatCapability.setTemperature(first, 1600);
                HeatCapability.setTemperature(second, 1600);
                anvilBlock.getInventory().setStackInSlot(0, first);
                anvilBlock.getInventory().setStackInSlot(1, second);
                anvilBlock.getInventory().setStackInSlot(3, item("tfc:powder/flux"));
                if (!anvilBlock.canWeldNow() || !anvilBlock.weld(test.makeMockPlayer(GameType.SURVIVAL)).consumesAction()) test.fail("Tier six weld " + metal);
                ItemStack product = anvilBlock.getInventory().getStackInSlot(0);
                if (!product.is(item("iaftfc:metal/double_ingot/" + metal).getItem())) test.fail("Weld product " + metal);
                doubleIngots.add(product.copy());
            }
            ItemStack mixed = item("iaftfc:metal/ingot/" + different(metal));
            HeatCapability.setTemperature(mixed, 1600);
            ItemStack matching = item("iaftfc:metal/ingot/" + metal);
            HeatCapability.setTemperature(matching, 1600);
            anvilBlock.getInventory().setStackInSlot(0, matching);
            anvilBlock.getInventory().setStackInSlot(1, mixed);
            if (anvilBlock.canWeldNow()) test.fail("Mixed metal weld " + metal);
            anvilBlock.getInventory().setStackInSlot(0, mixed);
            anvilBlock.getInventory().setStackInSlot(1, matching);
            if (anvilBlock.canWeldNow()) test.fail("Reversed mixed metal weld " + metal);
            WeldingRecipe sheet = recipe(test, "iaftfc:welding/double_sheet/" + metal, WeldingRecipe.class);
            if (sheet.isCorrectTier(6) || !sheet.isCorrectTier(7)) test.fail("Double sheet tier " + metal);
            List<ItemStack> grid = new ArrayList<>();
            for (int slot = 0; slot < 9; slot++) grid.add(slot == 3 || slot == 5 ? ItemStack.EMPTY : doubleIngots.remove(0));
            CraftingRecipe anvil = recipe(test, "iaftfc:crafting/anvil/" + metal, CraftingRecipe.class);
            CraftingInput input = CraftingInput.of(3, 3, grid);
            if (!anvil.matches(input, test.getLevel()) || !anvil.assemble(input, test.getLevel().registryAccess()).is(item("iaftfc:metal/anvil/" + metal).getItem())) test.fail("Anvil crafting " + metal);
            grid.set(0, item("iaftfc:metal/double_ingot/" + different(metal)));
            if (anvil.matches(CraftingInput.of(3, 3, grid), test.getLevel())) test.fail("Mixed anvil crafting " + metal);
        }
        test.succeed();
    }

    @GameTest(template = "empty")
    public static void respectsTemperatureAndTier(GameTestHelper test) {
        for (String metal : METALS) {
            ItemStack stack = item("iaftfc:metal/ingot/" + metal);
            BlockPos pos = new BlockPos(1, 1, 1);
            for (float temperature : new float[] {1199.9f, 1200f, 1599.9f, 1600f, 1999.9f, 2000f}) {
                HeatCapability.setTemperature(stack, temperature);
                if (HeatCapability.view(stack).canWork() != (temperature >= 1200) || HeatCapability.view(stack).canWeld() != (temperature >= 1600)) test.fail("Working heat " + metal + " " + temperature);
                HeatingRecipe heating = recipe(test, "iaftfc:heating/" + metal + "/ingot", HeatingRecipe.class);
                if (heating.isValidTemperature(temperature) != (temperature >= 2000)) test.fail("Melting heat " + metal + " " + temperature);
                for (int tier : new int[] {6, 7}) {
                    test.setBlock(pos, BuiltInRegistries.BLOCK.get(ResourceLocation.parse(tier == 6 ? "tfc:metal/anvil/blue_steel" : "iaftfc:metal/anvil/" + metal)));
                    AnvilBlockEntity anvil = (AnvilBlockEntity) test.getBlockEntity(pos);
                    ItemStack second = stack.copy();
                    anvil.getInventory().setStackInSlot(0, stack.copy());
                    anvil.getInventory().setStackInSlot(1, second);
                    anvil.getInventory().setStackInSlot(3, item("tfc:powder/flux"));
                    if (anvil.canWeldNow() != (temperature >= 1600)) test.fail("Weld heat " + metal + " " + temperature);
                    anvil.getInventory().setStackInSlot(0, item("iaftfc:metal/sheet/" + metal));
                    anvil.getInventory().setStackInSlot(1, item("iaftfc:metal/sheet/" + metal));
                    if (tier == 6 && anvil.canWeldNow()) test.fail("Tier six sheet weld " + metal);
                    if (AnvilRecipe.hasAny(test.getLevel(), stack, tier) != (tier == 7)) test.fail("Forge tier " + metal + " " + tier);
                    for (int index = 0; index < FORMS.length; index++) {
                        ItemStack source = item("iaftfc:metal/" + SOURCES[index] + "/" + metal);
                        HeatCapability.setTemperature(source, temperature);
                        anvil.getInventory().setStackInSlot(0, source);
                        anvil.chooseRecipe(ResourceLocation.parse("iaftfc:anvil/" + FORMS[index] + "/" + metal));
                        if (anvil.workRemotely(ForgeStep.HIT_LIGHT, 1, false) != (tier == 7 && temperature >= 1200)) test.fail("Forge boundary " + metal + " " + FORMS[index] + " " + temperature);
                    }
                    String[] welded = {"double_sheet", "helmet", "chestplate", "leggings", "boots"};
                    String[] firstForms = {"sheet", "unfinished_helmet", "unfinished_chestplate", "unfinished_greaves", "unfinished_boots"};
                    String[] secondForms = {"sheet", "sheet", "double_sheet", "sheet", "sheet"};
                    for (int index = 0; index < welded.length; index++) {
                        ItemStack first = item("iaftfc:metal/" + firstForms[index] + "/" + metal);
                        ItemStack other = item("iaftfc:metal/" + secondForms[index] + "/" + metal);
                        HeatCapability.setTemperature(first, temperature);
                        HeatCapability.setTemperature(other, temperature);
                        anvil.getInventory().setStackInSlot(0, first);
                        anvil.getInventory().setStackInSlot(1, other);
                        if (anvil.canWeldNow() != (tier == 7 && temperature >= 1600)) test.fail("Weld boundary " + metal + " " + welded[index] + " " + temperature);
                    }
                }
            }
        }
        test.succeed();
    }

    @GameTest(template = "empty")
    public static void forgesEveryForm(GameTestHelper test) {
        ServerPlayer smith = new ServerPlayer(test.getLevel().getServer(), test.getLevel(), new GameProfile(UUID.randomUUID(), "smith"), ClientInformation.createDefault());
        for (String metal : METALS) for (int index = 0; index < FORMS.length; index++) {
            AnvilRecipe forge = recipe(test, "iaftfc:anvil/" + FORMS[index] + "/" + metal, AnvilRecipe.class);
            ItemStack source = item("iaftfc:metal/" + SOURCES[index] + "/" + metal);
            if (!forge.isCorrectTier(7) || forge.isCorrectTier(6) || !forge.getInput().test(source)) test.fail("Forge input " + metal + " " + FORMS[index]);
            if (!forge.getResultItem(test.getLevel().registryAccess()).is(item("iaftfc:metal/" + FORMS[index] + "/" + metal).getItem()) || forge.getResultItem(test.getLevel().registryAccess()).getCount() != (FORMS[index].equals("rod") ? 2 : 1)) test.fail("Forge output " + metal + " " + FORMS[index]);
            if (forge.shouldApplyForgingBonus() != (index != 5 && index != 6)) test.fail("Forge bonus " + metal + " " + FORMS[index]);
            if (forge.getRules().size() != RULES[index].length) test.fail("Forge rules " + FORMS[index]);
            for (int rule = 0; rule < RULES[index].length; rule++) if (!forge.getRules().get(rule).getSerializedName().equals(RULES[index][rule])) test.fail("Forge rule order " + FORMS[index]);
            if (forge.getInput().test(item("iaftfc:metal/" + SOURCES[index] + "/" + different(metal)))) test.fail("Mixed forge " + FORMS[index]);
            ItemStack heated = source.copy();
            HeatCapability.setTemperature(heated, 1200);
            BlockPos pos = new BlockPos(1, 1, 1);
            test.setBlock(pos, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("iaftfc:metal/anvil/" + metal)));
            AnvilBlockEntity anvil = (AnvilBlockEntity) test.getBlockEntity(pos);
            if (AnvilRecipe.getAll(test.getLevel(), heated, anvil.getTier()).stream().noneMatch(holder -> holder.value() == forge)) test.fail("Forge lookup " + metal + " " + FORMS[index]);
            anvil.getInventory().setStackInSlot(0, heated);
            anvil.getInventory().setStackInSlot(2, item("tfc:metal/hammer/steel"));
            anvil.chooseRecipe(ResourceLocation.parse("iaftfc:anvil/" + FORMS[index] + "/" + metal));
            int target = ForgingCapability.get(heated).target();
            ForgeStep[] steps = STEPS[index];
            int finishing = 0;
            for (ForgeStep step : steps) finishing += step.step();
            if (!anvil.workRemotely(ForgeStep.UPSET, target - finishing, false)) test.fail("Forge setup " + metal + " " + FORMS[index]);
            for (ForgeStep step : steps) anvil.work(smith, step);
            ItemStack product = anvil.getInventory().getStackInSlot(0);
            if (!product.is(item("iaftfc:metal/" + FORMS[index] + "/" + metal).getItem()) || product.getCount() != (index == 5 ? 2 : 1)) test.fail("Forge product " + metal + " " + FORMS[index]);
            if ((ForgingBonusComponent.get(product) != ForgingBonus.NONE) != (index != 5 && index != 6)) test.fail("Forge quality " + metal + " " + FORMS[index]);
        }
        test.succeed();
    }

    @GameTest(template = "empty")
    public static void craftsFinishedEquipment(GameTestHelper test) {
        String[] parts = {"axe", "hoe", "pickaxe", "shovel", "sword"};
        String[] armor = {"helmet", "chestplate", "leggings", "boots"};
        String[] unfinished = {"unfinished_helmet", "unfinished_chestplate", "unfinished_greaves", "unfinished_boots"};
        for (String metal : METALS) {
            String element = metal.substring("dragonsteel_".length());
            for (String part : parts) {
                CraftingRecipe recipe = recipe(test, "iaftfc:crafting/tool/" + metal + "_" + part, CraftingRecipe.class);
                ItemStack component = item("iaftfc:metal/" + (part.equals("sword") ? "sword_blade" : part + "_head") + "/" + metal);
                ForgingBonusComponent.set(component, ForgingBonus.EXPERT);
                CraftingInput input = CraftingInput.of(1, 2, List.of(component, item("tfc:wood/twig/oak")));
                if (!recipe.matches(input, test.getLevel())) test.fail("Tool inputs " + metal + " " + part);
                ItemStack output = recipe.assemble(input, test.getLevel().registryAccess());
                if (!output.is(item("iceandfire:dragonsteel_" + element + "_" + part).getItem()) || output.getCount() != 1 || ForgingBonusComponent.get(output) != ForgingBonus.EXPERT) test.fail("Tool " + metal + " " + part);
                if (test.getLevel().getRecipeManager().byKey(ResourceLocation.parse("iceandfire:dragonsteel_" + element + "_" + part)).isPresent()) test.fail("Old tool recipe remains " + part);
            }
            for (int index = 0; index < armor.length; index++) {
                WeldingRecipe weld = recipe(test, "iaftfc:welding/" + armor[index] + "/" + metal, WeldingRecipe.class);
                if (!weld.isCorrectTier(7) || weld.isCorrectTier(6) || !weld.getFirstInput().test(item("iaftfc:metal/" + unfinished[index] + "/" + metal)) || !weld.getSecondInput().test(item("iaftfc:metal/" + (index == 1 ? "double_sheet" : "sheet") + "/" + metal))) test.fail("Armor welding " + metal + " " + armor[index]);
                ItemStack first = item("iaftfc:metal/" + unfinished[index] + "/" + metal);
                ItemStack second = item("iaftfc:metal/" + (index == 1 ? "double_sheet" : "sheet") + "/" + metal);
                ForgingBonusComponent.set(first, ForgingBonus.MODEST);
                ForgingBonusComponent.set(second, ForgingBonus.EXPERT);
                HeatCapability.setTemperature(first, 1600);
                HeatCapability.setTemperature(second, 1600);
                BlockPos pos = new BlockPos(1, 1, 1);
                test.setBlock(pos, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("iaftfc:metal/anvil/" + metal)));
                AnvilBlockEntity anvil = (AnvilBlockEntity) test.getBlockEntity(pos);
                anvil.getInventory().setStackInSlot(0, first);
                anvil.getInventory().setStackInSlot(1, second);
                anvil.getInventory().setStackInSlot(3, item("tfc:powder/flux"));
                if (!anvil.canWeldNow() || !anvil.weld(test.makeMockPlayer(GameType.SURVIVAL)).consumesAction()) test.fail("Armor welding action " + metal + " " + armor[index]);
                ItemStack output = anvil.getInventory().getStackInSlot(0);
                if (!output.is(item("iceandfire:dragonsteel_" + element + "_" + armor[index]).getItem()) || output.getCount() != 1 || ForgingBonusComponent.get(output) != ForgingBonus.EXPERT) test.fail("Armor output " + metal + " " + armor[index]);
                if (test.getLevel().getRecipeManager().byKey(ResourceLocation.parse("iceandfire:dragonsteel_" + element + "_" + armor[index])).isPresent()) test.fail("Old armor recipe remains " + armor[index]);
            }
        }
        test.succeed();
    }

    @GameTest(template = "empty")
    public static void meltsAllAmounts(GameTestHelper test) {
        for (String metal : METALS) for (int index = 0; index < HEAT_FORMS.length; index++) {
            String form = HEAT_FORMS[index];
            ItemStack stack = item(heatedItem(metal, form));
            HeatingRecipe heating = recipe(test, "iaftfc:heating/" + metal + "/" + form, HeatingRecipe.class);
            FluidStack fluid = heating.assembleFluid(stack);
            if (!heating.matches(stack) || fluid.getAmount() != AMOUNTS[index] || fluid.getFluid() != BuiltInRegistries.FLUID.get(ResourceLocation.parse("tfc:metal/" + metal))) test.fail("Melting " + metal + " " + form);
            float expectedCapacity = AMOUNTS[index] * 0.02857143f;
            if (Math.abs(HeatCapability.view(stack).getHeatCapacity() - expectedCapacity) > 0.001f) test.fail("Heat capacity " + metal + " " + form);
            if (index >= 18 && index <= 22) {
                stack.setDamageValue(stack.getMaxDamage() / 2);
                int expected = (int) Math.floor(AMOUNTS[index] * (stack.getMaxDamage() - stack.getDamageValue()) / (double) stack.getMaxDamage());
                if (heating.assembleFluid(stack).getAmount() != expected) test.fail("Damaged tool recovery " + metal + " " + form);
            } else if (index >= 23) {
                stack.setDamageValue(stack.getMaxDamage() / 2);
                if (heating.assembleFluid(stack).getAmount() != AMOUNTS[index]) test.fail("Damaged armor recovery " + metal + " " + form);
            }
        }
        test.succeed();
    }

    @GameTest(template = "empty")
    public static void castsMatchingIngots(GameTestHelper test) {
        for (String metal : METALS) {
            CastingRecipe casting = recipe(test, "iaftfc:casting/" + metal + "_ingot", CastingRecipe.class);
            if (Math.abs(casting.getBreakChance() - 0.1f) > 0.0001f || !casting.getIngredient().test(item("tfc:ceramic/ingot_mold")) || !casting.getResultItem().is(item("iaftfc:metal/ingot/" + metal).getItem())) test.fail("Casting result " + metal);
            FluidStack enough = new FluidStack(BuiltInRegistries.FLUID.get(ResourceLocation.parse("tfc:metal/" + metal)), 100);
            FluidStack little = new FluidStack(enough.getFluid(), 99);
            FluidStack wrong = new FluidStack(BuiltInRegistries.FLUID.get(ResourceLocation.parse("tfc:metal/" + different(metal))), 100);
            if (!casting.getFluidIngredient().test(enough) || casting.getFluidIngredient().test(little) || casting.getFluidIngredient().test(wrong)) test.fail("Casting fluid " + metal);
            ItemStack wrongMold = item("minecraft:bucket");
            if (casting.getIngredient().test(wrongMold)) test.fail("Wrong mold " + metal);
            IMold incompatible = IMold.get(item("tfc:ceramic/ingot_mold"));
            if (incompatible == null || incompatible.fill(wrong, IFluidHandler.FluidAction.EXECUTE) != 100 || casting.matches(incompatible)) test.fail("Wrong metal casting " + metal);
            ItemStack moldItem = item("tfc:ceramic/ingot_mold");
            IMold mold = IMold.get(moldItem);
            if (mold == null || mold.fill(little, IFluidHandler.FluidAction.EXECUTE) != 99 || casting.matches(mold)) test.fail("Insufficient mold " + metal);
            if (mold.fill(new FluidStack(enough.getFluid(), 1), IFluidHandler.FluidAction.EXECUTE) != 1 || !casting.matches(mold)) test.fail("Filled mold " + metal);
            mold.setTemperature(2100);
            if (!mold.isMolten()) test.fail("Molten mold " + metal);
            mold.setTemperature(100);
            if (mold.isMolten() || CastingRecipe.get(mold) != casting) test.fail("Cooled casting lookup " + metal);
            ItemStack saved = ItemStack.parseOptional(test.getLevel().registryAccess(), (CompoundTag) mold.getContainer().save(test.getLevel().registryAccess()));
            IMold restored = IMold.get(saved);
            if (restored == null || !casting.matches(restored)) test.fail("Saved mold " + metal);
            Player player = test.makeMockPlayer(GameType.SURVIVAL);
            player.setShiftKeyDown(true);
            player.setItemInHand(InteractionHand.MAIN_HAND, saved);
            MoldItem moldType = (MoldItem) saved.getItem();
            moldType.use(test.getLevel(), player, InteractionHand.MAIN_HAND);
            ItemStack product = item("iaftfc:metal/ingot/" + metal);
            if (player.getInventory().items.stream().filter(stack -> stack.is(product.getItem())).mapToInt(ItemStack::getCount).sum() != 1) test.fail("Cast ingot " + metal);
            moldType.use(test.getLevel(), player, InteractionHand.MAIN_HAND);
            if (player.getInventory().items.stream().filter(stack -> stack.is(product.getItem())).mapToInt(ItemStack::getCount).sum() != 1) test.fail("Mold duplicated ingot " + metal);
        }
        test.succeed();
    }

    private static String heatedItem(String metal, String form) {
        if (form.equals("anvil")) return "iaftfc:metal/anvil/" + metal;
        if (form.startsWith("block")) return "iaftfc:metal/block/" + metal + (form.equals("block") ? "" : form.substring(5));
        if (List.of("axe", "hoe", "pickaxe", "shovel", "sword", "helmet", "leggings", "chestplate", "boots").contains(form)) return "iceandfire:" + metal + "_" + form;
        return "iaftfc:metal/" + form + "/" + metal;
    }

    private static String different(String metal) {
        return metal.equals(METALS[0]) ? METALS[1] : METALS[0];
    }

    private static ItemStack item(String id) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
    }

    private static <T> T recipe(GameTestHelper test, String id, Class<T> type) {
        Object value = test.getLevel().getRecipeManager().byKey(ResourceLocation.parse(id)).orElseThrow().value();
        if (!type.isInstance(value)) test.fail("Wrong recipe type " + id);
        return type.cast(value);
    }
}
