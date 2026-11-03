package net.claustra01.iaftfc.gametest;

import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.item.block.entity.DragonForgeBlockEntity;
import com.iafenvoy.iceandfire.item.block.entity.DragonForgeInputBlockEntity;
import com.iafenvoy.iceandfire.recipe.DragonForgeRecipe;
import net.claustra01.iaftfc.metal.Dragonsteel;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("iaftfc")
@PrefixGameTestTemplate(false)
public final class DragonForgeGameTests {
    private static final String[] ELEMENTS = {"fire", "ice", "lightning"};
    private static final String[] STEELS = {"red_steel", "blue_steel", "black_steel"};

    @GameTest(template = "dragon_forge")
    public static void forgeRecipesAndAssembly(GameTestHelper helper) {
        for (int index = 0; index < ELEMENTS.length; index++) {
            String element = ELEMENTS[index];
            Recipe<CraftingInput> brick = crafting(helper, "iceandfire:dragonforge_" + element + "_brick");
            ItemStack scaleBlock = item("iceandfire:dragonscale_" + new String[] {"red", "blue", "electric"}[index]);
            ItemStack stone = item("tfc:rock/bricks/" + new String[] {"granite", "basalt", "quartzite"}[index]);
            CraftingInput brickInput = CraftingInput.of(3, 3, List.of(scaleBlock.copy(), stone.copy(), scaleBlock.copy(),
                stone.copy(), scaleBlock.copy(), stone.copy(), scaleBlock.copy(), stone.copy(), scaleBlock.copy()));
            helper.assertTrue(brick.matches(brickInput, helper.getLevel()), "TFC stone bricks cannot craft " + element + " bricks");
            ItemStack bricks = brick.assemble(brickInput, helper.getLevel().registryAccess());
            helper.assertTrue(bricks.is(item("iceandfire:dragonforge_" + element + "_brick").getItem())
                && bricks.getCount() == 4, "Forge brick count or output changed");

            Recipe<CraftingInput> input = crafting(helper, "iceandfire:dragonforge_" + element + "_input");
            ItemStack iron = item("tfc:metal/ingot/wrought_iron");
            CraftingInput inputGrid = CraftingInput.of(3, 3, List.of(bricks.copyWithCount(1), iron.copy(), bricks.copyWithCount(1),
                iron.copy(), ItemStack.EMPTY, iron.copy(), bricks.copyWithCount(1), iron.copy(), bricks.copyWithCount(1)));
            helper.assertTrue(input.matches(inputGrid, helper.getLevel())
                && input.assemble(inputGrid, helper.getLevel().registryAccess()).getCount() == 1,
                "Wrought iron cannot craft " + element + " input");
            helper.assertTrue(!input.matches(CraftingInput.of(3, 3, List.of(bricks.copyWithCount(1),
                item("minecraft:iron_ingot"), bricks.copyWithCount(1), item("minecraft:iron_ingot"), ItemStack.EMPTY,
                item("minecraft:iron_ingot"), bricks.copyWithCount(1), item("minecraft:iron_ingot"), bricks.copyWithCount(1))),
                helper.getLevel()), "Vanilla iron crafted the forge input");
            Recipe<CraftingInput> coreRecipe = crafting(helper, "iceandfire:dragonforge_" + element + "_core_disabled");
            CraftingInput coreGrid = CraftingInput.of(3, 3, List.of(bricks.copyWithCount(1), bricks.copyWithCount(1),
                bricks.copyWithCount(1), bricks.copyWithCount(1), item("iceandfire:" + element + "_dragon_heart"),
                bricks.copyWithCount(1), bricks.copyWithCount(1), bricks.copyWithCount(1), bricks.copyWithCount(1)));
            helper.assertTrue(coreRecipe.matches(coreGrid, helper.getLevel())
                && coreRecipe.assemble(coreGrid, helper.getLevel().registryAccess())
                    .is(item("iceandfire:dragonforge_" + element + "_core_disabled").getItem())
                && coreRecipe.assemble(coreGrid, helper.getLevel().registryAccess()).getCount() == 1,
                "Original Dragon Heart core recipe changed");
            buildForge(helper, index);
        }
        helper.succeed();
    }

    @GameTest(template = "dragon_forge", timeoutTicks = 120)
    public static void forgeFireDragonsteelIngot(GameTestHelper helper) {
        forgeDragonsteelIngot(helper, 0);
    }

    @GameTest(template = "dragon_forge", timeoutTicks = 120)
    public static void forgeIceDragonsteelIngot(GameTestHelper helper) {
        forgeDragonsteelIngot(helper, 1);
    }

    @GameTest(template = "dragon_forge", timeoutTicks = 120)
    public static void forgeLightningDragonsteelIngot(GameTestHelper helper) {
        forgeDragonsteelIngot(helper, 2);
    }

    private static void forgeDragonsteelIngot(GameTestHelper helper, int index) {
        buildForge(helper, index, 3);
        helper.runAfterDelay(3, () -> {
            String element = ELEMENTS[index];
            DragonForgeBlockEntity core = coreAt(helper, 3);
            DragonForgeInputBlockEntity input = inputAt(helper, 3);
            DragonForgeRecipe recipe = forgeRecipe(helper, "dragonsteel_" + element + "_ingot");
            helper.assertTrue(recipe.getCookTime() == 1000 && recipe.getDragonType().equals(element),
                "Forge element or cooking time changed");
            core.setItem(0, item("minecraft:iron_ingot"));
            core.setItem(1, item("iceandfire:" + element + "_dragon_blood"));
            helper.assertTrue(core.getCurrentRecipe().isEmpty(), "Vanilla iron matched a Dragonsteel recipe");
            core.setItem(0, item("iceandfire:dragonsteel_" + element + "_ingot"));
            helper.assertTrue(core.getCurrentRecipe().isEmpty(), "Legacy IaF ingot matched a Dragonsteel recipe");
            core.setItem(0, item("tfc:metal/ingot/" + STEELS[(index + 1) % 3]));
            helper.assertTrue(core.getCurrentRecipe().isEmpty(), "Wrong steel matched a Dragonsteel recipe");
            core.setItem(0, item("tfc:metal/ingot/" + STEELS[index]));
            core.setItem(1, item("iceandfire:" + ELEMENTS[(index + 1) % 3] + "_dragon_blood"));
            helper.assertTrue(core.getCurrentRecipe().isEmpty(), "Wrong blood matched a Dragonsteel recipe");
            core.setItem(1, item("iceandfire:" + element + "_dragon_blood"));
            helper.assertTrue(core.canSmelt() && input.isAssembled(), "Completed forge cannot smelt");
            for (int pulse = 0; pulse < 999; pulse++) {
                input.onHitWithFlame(1);
            }
            helper.assertTrue(core.getItem(2).isEmpty(), "Forge finished before 1000 breath pulses");
            input.onHitWithFlame(1);
            helper.runAfterDelay(2, () -> {
                helper.assertTrue(core.getItem(2).is(item("iaftfc:metal/ingot/" + Dragonsteel.METALS[index]).getItem())
                    && core.getItem(2).getCount() == 1 && core.getItem(0).isEmpty() && core.getItem(1).isEmpty(),
                    "Dragon breath did not consume both inputs and yield one Dragonsteel ingot");
                helper.succeed();
            });
        });
    }

    @GameTest(template = "dragon_forge", timeoutTicks = 120)
    public static void forgeBlood(GameTestHelper helper) {
        for (int index = 0; index < ELEMENTS.length; index++) {
            buildForge(helper, index);
        }
        helper.runAfterDelay(3, () -> {
            for (int index = 0; index < ELEMENTS.length; index++) {
                String element = ELEMENTS[index];
                DragonForgeRecipe recipe = forgeRecipe(helper, element + "_dragon_blood");
                DragonForgeBlockEntity core = core(helper, index);
                helper.assertTrue(recipe.getCookTime() == 750, "Original blood cooking time changed");
                core.setItem(0, item("iceandfire:dragonscales_" + new String[] {"red", "blue", "electric"}[index]));
                core.setItem(1, new ItemStack(Items.GLASS_BOTTLE));
                helper.assertTrue(core.canSmelt(), "Original blood recipe no longer works");
                DragonForgeInputBlockEntity input = input(helper, index);
                BlockPos inputPos = input.getBlockPos();
                DragonForgeInputBlockEntity.tick(helper.getLevel(), inputPos,
                    helper.getLevel().getBlockState(inputPos), input);
                helper.assertTrue(input.isAssembled(), "Blood forge input is not assembled");
                for (int pulse = 0; pulse < 750; pulse++) {
                    input.onHitWithFlame(1);
                }
            }
            helper.runAfterDelay(2, () -> {
                for (int index = 0; index < ELEMENTS.length; index++) {
                    DragonForgeBlockEntity core = core(helper, index);
                    helper.assertTrue(core.getItem(2).is(item("iceandfire:" + ELEMENTS[index] + "_dragon_blood").getItem())
                        && core.getItem(2).getCount() == 1, "Blood recipe did not finish at 750 pulses");
                }
                helper.succeed();
            });
        });
    }

    @GameTest(template = "dragon_forge", timeoutTicks = 200)
    public static void fireDragonBreathFeedsForge(GameTestHelper helper) {
        dragonBreathFeedsForge(helper, 0);
    }

    @GameTest(template = "dragon_forge", timeoutTicks = 200)
    public static void iceDragonBreathFeedsForge(GameTestHelper helper) {
        dragonBreathFeedsForge(helper, 1);
    }

    @GameTest(template = "dragon_forge", timeoutTicks = 200)
    public static void lightningDragonBreathFeedsForge(GameTestHelper helper) {
        dragonBreathFeedsForge(helper, 2);
    }

    private static void dragonBreathFeedsForge(GameTestHelper helper, int index) {
        buildForge(helper, index, 3);
        helper.runAfterDelay(3, () -> {
            DragonForgeBlockEntity forgeCore = coreAt(helper, 3);
            forgeCore.setItem(0, item("tfc:metal/ingot/" + STEELS[index]));
            forgeCore.setItem(1, item("iceandfire:" + ELEMENTS[index] + "_dragon_blood"));
            DragonBaseEntity dragon = (DragonBaseEntity) BuiltInRegistries.ENTITY_TYPE.get(
                ResourceLocation.parse("iceandfire:" + ELEMENTS[index] + "_dragon")).create(helper.getLevel());
            dragon.setAgeInDays(30);
            dragon.setTame(true, false);
            // Keep the breath path fixed while IaF advances the breath timers normally.
            dragon.setNoAi(true);
            dragon.setNoGravity(true);
            BlockPos dragonPos = helper.absolutePos(new BlockPos(3, 1, 6));
            dragon.setPos(dragonPos.getX() + 0.5, dragonPos.getY(), dragonPos.getZ() + 0.5);
            helper.getLevel().addFreshEntity(dragon);
            BlockPos pos = helper.absolutePos(new BlockPos(3, 3, 12));
            DragonForgeInputBlockEntity forgeInput = inputAt(helper, 3);
            helper.assertTrue(forgeInput.isAssembled(), "Forge input is not assembled");
            DragonForgeInputBlockEntity.tick(helper.getLevel(), pos,
                helper.getLevel().getBlockState(pos), forgeInput);
            helper.assertTrue(pos.equals(dragon.burningTarget),
                "Forge input did not lure " + ELEMENTS[index] + " dragon");
            helper.assertTrue(dragon.canPositionBeSeen(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5),
                "Dragon cannot see the forge input: " + ELEMENTS[index]);
            dragon.updateBurnTarget();
            helper.assertTrue(dragon.isBreathingFire(),
                "Lured " + ELEMENTS[index] + " dragon cannot start its breath");
            helper.succeedWhen(() -> {
                DragonForgeInputBlockEntity.tick(helper.getLevel(), pos,
                    helper.getLevel().getBlockState(pos), forgeInput);
                dragon.updateBurnTarget();
                helper.assertTrue(forgeCore.isBurning(),
                    "Dragon did not power " + ELEMENTS[index] + " forge through its breath"
                        + " (breath ticks=" + dragon.fireBreathTicks + ", progress=" + dragon.burnProgress
                        + ", target=" + dragon.burningTarget + ", position=" + dragon.position() + ")");
            });
        });
    }

    private static void buildForge(GameTestHelper helper, int index) {
        buildForge(helper, index, 3 + index * 12);
    }

    private static void buildForge(GameTestHelper helper, int index, int centerX) {
        String element = ELEMENTS[index];
        BlockPos center = new BlockPos(centerX, 3, 13);
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dy == 0 && dx == 0 && dz == 0) {
                        continue;
                    }
                    if (dy == 0 && dx == 0 && dz == -1) {
                        helper.setBlock(center.offset(dx, dy, dz), block("iceandfire:dragonforge_" + element + "_input"));
                    } else {
                        String id = dy != 0 && dx != 0 && dz != 0 ? "iceandfire:dragon_bone_block"
                            : "iceandfire:dragonforge_" + element + "_brick";
                        helper.setBlock(center.offset(dx, dy, dz), block(id));
                    }
                }
            }
        }
        helper.setBlock(center, block("iceandfire:dragonforge_" + element + "_core_disabled"));
        helper.assertTrue(coreAt(helper, centerX).assembled(), "Original IaF multiblock is incomplete: " + element);
    }

    private static DragonForgeBlockEntity core(GameTestHelper helper, int index) {
        return coreAt(helper, 3 + index * 12);
    }

    private static DragonForgeBlockEntity coreAt(GameTestHelper helper, int centerX) {
        BlockPos pos = new BlockPos(centerX, 3, 13);
        var entity = helper.getBlockEntity(pos);
        helper.assertTrue(entity instanceof DragonForgeBlockEntity,
            "Expected Forge core at " + centerX + " but found " + entity.getClass().getSimpleName());
        return (DragonForgeBlockEntity) entity;
    }

    private static DragonForgeInputBlockEntity input(GameTestHelper helper, int index) {
        return inputAt(helper, 3 + index * 12);
    }

    private static DragonForgeInputBlockEntity inputAt(GameTestHelper helper, int centerX) {
        return (DragonForgeInputBlockEntity) helper.getBlockEntity(new BlockPos(centerX, 3, 12));
    }

    private static DragonForgeRecipe forgeRecipe(GameTestHelper helper, String name) {
        return (DragonForgeRecipe) helper.getLevel().getServer().getRecipeManager()
            .byKey(ResourceLocation.parse("iceandfire:dragonforge/" + name)).orElseThrow().value();
    }

    @SuppressWarnings("unchecked")
    private static Recipe<CraftingInput> crafting(GameTestHelper helper, String name) {
        return (Recipe<CraftingInput>) helper.getLevel().getServer().getRecipeManager()
            .byKey(ResourceLocation.parse(name)).orElseThrow().value();
    }

    private static ItemStack item(String name) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(name)));
    }

    private static net.minecraft.world.level.block.Block block(String name) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.parse(name));
    }
}
