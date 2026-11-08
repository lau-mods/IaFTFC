package net.claustra01.iaftfc_test.gametest;

import com.iafenvoy.iceandfire.entity.DragonEggEntity;
import com.iafenvoy.iceandfire.registry.IafBlocks;
import com.iafenvoy.iceandfire.registry.IafDragonColors;
import com.iafenvoy.iceandfire.registry.IafEntities;
import net.dries007.tfc.common.blocks.TFCBlocks;
import net.dries007.tfc.common.blocks.devices.CharcoalForgeBlock;
import net.dries007.tfc.common.blocks.devices.FirepitBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("iaftfc")
@PrefixGameTestTemplate(false)
public final class DragonEggGameTests {
    private static final String EMPTY_TEMPLATE = "empty";
    private static final long ICE_CONVERSION_SEED = findIceConversionSeed();

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 200)
    public static void activeTfcHeatSourcesWarmFireEggs(GameTestHelper helper) {
        BlockPos eggPos = new BlockPos(2, 2, 2);
        BlockState firepit = TFCBlocks.FIREPIT.get().defaultBlockState();
        assertFireAge(helper, eggPos, firepit.setValue(FirepitBlock.LIT, true), 1,
            "A lit TFC Firepit must warm a Fire Egg");
        assertFireAge(helper, eggPos, firepit.setValue(FirepitBlock.LIT, false), 0,
            "An unlit TFC Firepit must not warm a Fire Egg");

        BlockState forge = TFCBlocks.CHARCOAL_FORGE.get().defaultBlockState();
        assertFireAge(helper, eggPos, forge.setValue(CharcoalForgeBlock.HEAT, 1), 1,
            "An active TFC Charcoal Forge must warm a Fire Egg");
        assertFireAge(helper, eggPos, forge.setValue(CharcoalForgeBlock.HEAT, 0), 0,
            "An inactive TFC Charcoal Forge must not warm a Fire Egg");

        helper.setBlock(eggPos.below(), Blocks.STONE);
        helper.setBlock(eggPos, Blocks.FIRE);
        DragonEggEntity vanillaFireEgg = fireEgg(helper, eggPos);
        vanillaFireEgg.updateEggCondition();
        helper.assertValueEqual(vanillaFireEgg.getDragonAge(), 1,
            "CE's original fire condition must keep warming a Fire Egg");
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 200)
    public static void tfcFreshWaterFreezesIceEggs(GameTestHelper helper) {
        BlockPos freshPos = new BlockPos(2, 2, 2);
        helper.setBlock(freshPos, TFCBlocks.RIVER_WATER.get());
        PredictableIceEgg freshEgg = iceEgg(helper, freshPos);
        freshEgg.updateEggCondition();
        helper.assertBlockPresent(IafBlocks.EGG_IN_ICE.get(), freshPos);
        helper.assertTrue(freshEgg.isRemoved(), "Fresh Water must convert and remove the Ice Egg entity");

        BlockPos saltPos = new BlockPos(4, 2, 2);
        helper.setBlock(saltPos, TFCBlocks.SALT_WATER.get());
        PredictableIceEgg saltEgg = iceEgg(helper, saltPos);
        saltEgg.updateEggCondition();
        helper.assertBlockPresent(TFCBlocks.SALT_WATER.get(), saltPos);
        helper.assertTrue(!saltEgg.isRemoved(), "Salt Water must not convert an Ice Egg");

        BlockPos waterloggedPos = new BlockPos(8, 2, 2);
        BlockState waterloggedSlab = Blocks.OAK_SLAB.defaultBlockState()
            .setValue(SlabBlock.WATERLOGGED, true);
        helper.setBlock(waterloggedPos, waterloggedSlab);
        PredictableIceEgg waterloggedEgg = iceEgg(helper, waterloggedPos);
        waterloggedEgg.updateEggCondition();
        helper.assertBlockPresent(Blocks.OAK_SLAB, waterloggedPos);
        helper.assertTrue(!waterloggedEgg.isRemoved(), "Waterlogged blocks must not convert Ice Eggs");

        BlockPos vanillaPos = new BlockPos(6, 2, 2);
        helper.setBlock(vanillaPos, Blocks.WATER);
        PredictableIceEgg vanillaEgg = iceEgg(helper, vanillaPos);
        vanillaEgg.updateEggCondition();
        helper.assertBlockPresent(IafBlocks.EGG_IN_ICE.get(), vanillaPos);
        helper.assertTrue(vanillaEgg.isRemoved(), "CE's original water condition must keep converting Ice Eggs");
        helper.succeed();
    }

    private static void assertFireAge(GameTestHelper helper, BlockPos eggPos, BlockState heatSource,
        int expectedAge, String message) {
        helper.setBlock(eggPos, Blocks.AIR);
        helper.setBlock(eggPos.below(), heatSource);
        DragonEggEntity egg = fireEgg(helper, eggPos);
        egg.updateEggCondition();
        helper.assertValueEqual(egg.getDragonAge(), expectedAge, message);
    }

    private static DragonEggEntity fireEgg(GameTestHelper helper, BlockPos relativePos) {
        DragonEggEntity egg = new DragonEggEntity(IafEntities.DRAGON_EGG.get(), helper.getLevel());
        egg.setEggType(IafDragonColors.RED);
        BlockPos pos = helper.absolutePos(relativePos);
        egg.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        return egg;
    }

    private static PredictableIceEgg iceEgg(GameTestHelper helper, BlockPos relativePos) {
        PredictableIceEgg egg = new PredictableIceEgg(helper.getLevel());
        egg.setEggType(IafDragonColors.SAPPHIRE);
        BlockPos pos = helper.absolutePos(relativePos);
        egg.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        return egg;
    }

    private static long findIceConversionSeed() {
        for (long seed = 0; seed < 10_000; seed++) {
            if (RandomSource.create(seed).nextInt(500) == 0) {
                return seed;
            }
        }
        throw new IllegalStateException("No deterministic Ice Egg conversion seed found");
    }

    private static final class PredictableIceEgg extends DragonEggEntity {
        private final RandomSource conversionRandom = RandomSource.create(ICE_CONVERSION_SEED);

        private PredictableIceEgg(Level level) {
            super(IafEntities.DRAGON_EGG.get(), level);
        }

        @Override
        public RandomSource getRandom() {
            return conversionRandom;
        }
    }

    private DragonEggGameTests() {
    }
}
