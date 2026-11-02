package dev.iaftfc.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "com.iafenvoy.iceandfire.world.structure.DragonRoostStructure$DragonRoostPiece")
public abstract class DragonRoostProtectionMixin {
    @Redirect(method = {"lambda$generateShell$2", "lambda$hollowOut$1", "lambda$generateSurface$3", "generateRoostPile"},
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/WorldGenLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private static boolean iaftfc$preserveProtectedGround(WorldGenLevel level, BlockPos pos, BlockState state, int flags) {
        return level.getBlockEntity(pos) == null && level.getBlockState(pos).getDestroySpeed(level, pos) >= 0
            && level.setBlock(pos, state, flags);
    }

    @Redirect(method = {"generateBoulder", "generateArch"}, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/LevelAccessor;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private static boolean iaftfc$preserveBoulderGround(LevelAccessor level, BlockPos pos, BlockState state, int flags) {
        return level.getBlockEntity(pos) == null && level.getBlockState(pos).getDestroySpeed(level, pos) >= 0
            && level.setBlock(pos, state, flags);
    }
}
