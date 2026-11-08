package net.claustra01.iaftfc.dragon;

import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.blocks.devices.CharcoalForgeBlock;
import net.dries007.tfc.common.blocks.devices.FirepitBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class EggHeatSources {
    public static boolean isActiveTfcHeatSource(LevelReader level, BlockPos eggPos) {
        BlockState state = level.getBlockState(eggPos.below());
        if (state.getBlock() instanceof FirepitBlock) {
            return state.getValue(FirepitBlock.LIT);
        }
        return state.getBlock() instanceof CharcoalForgeBlock
            && state.getValue(CharcoalForgeBlock.HEAT) > 0;
    }

    public static boolean isFreshWater(LevelReader level, BlockPos eggPos) {
        BlockState state = level.getBlockState(eggPos);
        return state.getBlock() instanceof LiquidBlock
            && state.getFluidState().is(TFCTags.Fluids.ANY_FRESH_WATER);
    }

    private EggHeatSources() {
    }
}
