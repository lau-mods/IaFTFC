package net.claustra01.iaftfc.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.iafenvoy.iceandfire.item.block.entity.DragonForgeBlockEntity;
import com.iafenvoy.iceandfire.recipe.DragonForgeRecipe;
import java.util.Map;
import net.claustra01.iaftfc.common.ForgeHeatConfig;
import net.dries007.tfc.common.component.TFCComponents;
import net.dries007.tfc.common.component.heat.HeatCapability;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.gametest.GameTestHolder;

@GameTestHolder("iaftfc")
public final class DragonForgeTests {
    static {
        GameTestReports.configure();
    }

    @GameTest(template = "empty")
    public static void producesAllThreeIngots(GameTestHelper test) {
        BlockPos pos = new BlockPos(2, 2, 2);
        String[] elements = {"fire", "ice", "lightning"};
        String[] steels = {"red_steel", "blue_steel", "black_steel"};
        for (int index = 0; index < elements.length; index++) {
            String element = elements[index];
            DragonForgeBlockEntity forge = forge(test, pos, element);
            ItemStack steel = item("tfc", "metal/ingot/" + steels[index]);
            ItemStack blood = item("iceandfire", element + "_dragon_blood");
            forge.setItem(0, steel);
            forge.setItem(1, blood);
            DragonForgeRecipe recipe = forge.getCurrentRecipe().orElseThrow();
            if (recipe.getCookTime() != 1000 || forge.getMaxCookTime() != 1000) test.fail("Wrong cook progress for " + element);
            complete(test, forge);
            if (!steel.isEmpty() || !blood.isEmpty()) test.fail("Wrong ingredient consumption for " + element);
            ItemStack output = forge.getItem(2);
            if (!output.is(item("iaftfc", "metal/ingot/dragonsteel_" + element).getItem()) || output.getCount() != 1) test.fail("Wrong output for " + element);
            if (Math.abs(HeatCapability.getTemperature(output) - 1800) > 1) test.fail("Wrong output heat for " + element);
            if (Math.abs(HeatCapability.view(output).getHeatCapacity() - 2.857143f) > 0.001f) test.fail("Wrong output heat capacity for " + element);
            if (Math.abs(HeatCapability.getTemperature(recipe.getResultItem()) - ForgeHeatConfig.temperature()) > 1) test.fail("Wrong recipe preview heat for " + element);
        }
        test.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 250)
    public static void heatsOnlyCompletedOutput(GameTestHelper test) {
        DragonForgeBlockEntity forge = forge(test, new BlockPos(2, 2, 2), "fire");
        ItemStack steel = item("tfc", "metal/ingot/red_steel");
        ItemStack blood = item("iceandfire", "fire_dragon_blood");
        steel.setCount(2);
        blood.setCount(2);
        ItemStack previous = item("iaftfc", "metal/ingot/dragonsteel_fire");
        ForgeHeatConfig.prepareHeat(previous);
        HeatCapability.setTemperature(previous, 100);
        ItemStack stored = previous.copy();
        forge.setItem(0, steel);
        forge.setItem(1, blood);
        forge.setItem(2, previous);
        complete(test, forge);
        if (previous.getCount() != 2 || Math.abs(HeatCapability.getTemperature(previous) - 1800) > 1) test.fail("Output stack did not heat together");
        if (steel.getCount() != 1 || blood.getCount() != 1 || HeatCapability.getTemperature(steel) >= 1800
            || HeatCapability.getTemperature(blood) >= 1800 || HeatCapability.getTemperature(stored) >= 1800)
            test.fail("Unrelated item heated or consumed unexpectedly");
        ItemStack taken = forge.removeItemNoUpdate(2);
        float before = HeatCapability.getTemperature(taken);
        test.runAfterDelay(200, () -> {
            if (HeatCapability.getTemperature(taken) >= before) test.fail("Produced item did not cool");
            test.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void rejectsBlockedAndWrongIngredients(GameTestHelper test) {
        BlockPos pos = new BlockPos(2, 2, 2);
        String[] elements = {"fire", "ice", "lightning"};
        String[] steels = {"red_steel", "blue_steel", "black_steel"};
        for (int index = 0; index < elements.length; index++) {
            String element = elements[index];
            for (String blocked : new String[] {"full", "other"}) {
                DragonForgeBlockEntity forge = forge(test, pos, element);
                forge.setItem(0, item("tfc", "metal/ingot/" + steels[index]));
                forge.setItem(1, item("iceandfire", element + "_dragon_blood"));
                ItemStack output = blocked.equals("full") ? item("iaftfc", "metal/ingot/dragonsteel_" + element) : new ItemStack(Items.DIAMOND);
                if (blocked.equals("full")) output.setCount(output.getMaxStackSize());
                forge.setItem(2, output);
                ItemStack before = output.copy();
                complete(test, forge);
                if (forge.getItem(0).getCount() != 1 || forge.getItem(1).getCount() != 1 || !ItemStack.matches(before, forge.getItem(2))
                    || HeatCapability.getTemperature(forge.getItem(2)) >= 1800) test.fail("Blocked forge changed inventory or heat");
            }
            for (String wrong : new String[] {"steel", "blood"}) {
                DragonForgeBlockEntity forge = forge(test, pos, element);
                forge.setItem(0, item("tfc", "metal/ingot/" + steels[wrong.equals("steel") ? (index + 1) % 3 : index]));
                forge.setItem(1, item("iceandfire", elements[wrong.equals("blood") ? (index + 1) % 3 : index] + "_dragon_blood"));
                complete(test, forge);
                String fallback = element.equals("ice") ? "dragon_ice" : "ash";
                if (!forge.getItem(2).is(item("iceandfire", fallback).getItem()) || forge.getItem(2).getCount() != 1
                    || forge.getItem(2).has(TFCComponents.HEAT) || HeatCapability.getTemperature(forge.getItem(2)) != 0)
                    test.fail("Wrong material changed fallback");
            }
        }
        test.succeed();
    }

    @GameTest(template = "empty")
    public static void savesInventoryAndProgress(GameTestHelper test) {
        BlockPos pos = new BlockPos(2, 2, 2);
        DragonForgeBlockEntity forge = forge(test, pos, "fire");
        forge.setItem(0, item("tfc", "metal/ingot/red_steel"));
        forge.setItem(1, item("iceandfire", "fire_dragon_blood"));
        CompoundTag saved = forge.saveWithFullMetadata(test.getLevel().registryAccess());
        saved.putInt("CookTime", 500);
        forge.clearContent();
        forge.loadWithComponents(saved, test.getLevel().registryAccess());
        if (forge.getItem(0).getCount() != 1 || forge.getItem(1).getCount() != 1 || !forge.getItem(2).isEmpty()) test.fail("Forge inventory did not survive reload");
        if (forge.saveWithFullMetadata(test.getLevel().registryAccess()).getInt("CookTime") != 500) test.fail("Forge progress did not survive reload");
        complete(test, forge);
        if (forge.getItem(2).getCount() != 1 || forge.getItem(0).getCount() != 0 || forge.getItem(1).getCount() != 0) test.fail("Reload duplicated output");
        test.succeed();
    }

    @GameTest(template = "empty", batch = "forge_heat_config")
    public static void configurableHeatChangesOnlyNewOutput(GameTestHelper test) {
        ItemStack stored = item("iaftfc", "metal/ingot/dragonsteel_fire");
        ForgeHeatConfig.prepareHeat(stored);
        HeatCapability.setTemperature(stored, 100);
        ForgeHeatConfig listener = new ForgeHeatConfig(test.getLevel().getServer().getServerResources().managers());
        try {
            for (String setting : new String[] {"1500", "0", "-1", "\"NaN\"", "\"Infinity\""}) {
                Map<ResourceLocation, JsonElement> values = Map.of(
                    ResourceLocation.fromNamespaceAndPath("iaftfc", "dragonsteel"),
                    JsonParser.parseString("{\"temperature\":" + setting + "}"));
                listener.apply(values, null, null);
                float expected = setting.equals("1500") ? 1500 : 1800;
                DragonForgeBlockEntity forge = forge(test, new BlockPos(2, 2, 2), "fire");
                forge.setItem(0, item("tfc", "metal/ingot/red_steel"));
                forge.setItem(1, item("iceandfire", "fire_dragon_blood"));
                DragonForgeRecipe recipe = forge.getCurrentRecipe().orElseThrow();
                if (Math.abs(HeatCapability.getTemperature(recipe.getResultItem()) - expected) > 1) test.fail("Recipe preview ignored configuration");
                complete(test, forge);
                if (Math.abs(HeatCapability.getTemperature(forge.getItem(2)) - expected) > 1
                    || forge.getItem(0).getCount() != 0 || forge.getItem(1).getCount() != 0) test.fail("Invalid configuration damaged production");
                if (HeatCapability.getTemperature(stored) >= 150) test.fail("Stored output was changed");
            }
        } finally {
            listener.apply(Map.of(), null, null);
        }
        test.succeed();
    }

    private static void complete(GameTestHelper test, DragonForgeBlockEntity forge) {
        forge.transferPower(1000);
        DragonForgeBlockEntity.tick(test.getLevel(), forge.getBlockPos(), forge.getBlockState(), forge);
    }

    private static DragonForgeBlockEntity forge(GameTestHelper test, BlockPos pos, String element) {
        Block brick = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("iceandfire", "dragonforge_" + element + "_brick"));
        Block bone = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("iceandfire", "dragon_bone_block"));
        for (int y : new int[] {-1, 1}) {
            test.setBlock(pos.offset(0, y, 0), brick);
            for (int x : new int[] {-1, 1}) {
                for (int z : new int[] {-1, 1}) test.setBlock(pos.offset(x, y, z), bone);
            }
            for (int direction : new int[] {-1, 1}) {
                test.setBlock(pos.offset(direction, y, 0), brick);
                test.setBlock(pos.offset(0, y, direction), brick);
            }
        }
        for (int x : new int[] {-1, 1}) {
            for (int z : new int[] {-1, 1}) test.setBlock(pos.offset(x, 0, z), brick);
        }
        test.setBlock(pos.offset(0, 0, -1), brick);
        test.setBlock(pos.offset(0, 0, 1), brick);
        test.setBlock(pos.offset(-1, 0, 0), brick);
        test.setBlock(pos.offset(1, 0, 0), BuiltInRegistries.BLOCK.get(
            ResourceLocation.fromNamespaceAndPath("iceandfire", "dragonforge_" + element + "_input")));
        Block core = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("iceandfire", "dragonforge_" + element + "_core_disabled"));
        test.setBlock(pos, core);
        DragonForgeBlockEntity forge = (DragonForgeBlockEntity) test.getBlockEntity(pos);
        if (!forge.assembled()) test.fail("Forge is not assembled");
        DragonForgeBlockEntity.tick(test.getLevel(), forge.getBlockPos(), forge.getBlockState(), forge);
        return (DragonForgeBlockEntity) test.getBlockEntity(pos);
    }

    private static ItemStack item(String namespace, String path) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(namespace, path)));
    }
}
