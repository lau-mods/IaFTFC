package dev.iaftfc.mixin;

import com.iafenvoy.iceandfire.world.structure.FireDragonCaveStructure;
import com.iafenvoy.iceandfire.world.structure.IceDragonCaveStructure;
import dev.iaftfc.worldgen.CaveOreSelection;
import dev.iaftfc.worldgen.ClimateSelection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "com.iafenvoy.iceandfire.world.structure.DragonCaveStructure$DragonCavePiece")
public abstract class DragonCaveOreMixin {
    @Shadow private long seed;

    @Redirect(method = "lambda$createShell$2", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/LevelAccessor;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z", ordinal = 0))
    private boolean iaftfc$placeGemOre(LevelAccessor level, BlockPos pos, BlockState original, int flags) {
        if (level.getBlockEntity(pos) != null) return false;
        ClimateSelection.Element element = (Object) this instanceof FireDragonCaveStructure.FireDragonCavePiece
            ? ClimateSelection.Element.FIRE : (Object) this instanceof IceDragonCaveStructure.IceDragonCavePiece
            ? ClimateSelection.Element.ICE : ClimateSelection.Element.LIGHTNING;
        return level.setBlock(pos, BuiltInRegistries.BLOCK.get(
            CaveOreSelection.select(element, CaveOreSelection.index(seed, pos))).defaultBlockState(), flags);
    }

    @Redirect(method = "lambda$createShell$2", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/LevelAccessor;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z", ordinal = 1))
    private static boolean iaftfc$preserveShellBlockEntity(LevelAccessor level, BlockPos pos, BlockState state, int flags) {
        return level.getBlockEntity(pos) == null && level.setBlock(pos, state, flags);
    }

    @Redirect(method = "lambda$createShell$2", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/LevelAccessor;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z", ordinal = 2))
    private static boolean iaftfc$preservePlainShellBlockEntity(LevelAccessor level, BlockPos pos, BlockState state, int flags) {
        return iaftfc$preserveShellBlockEntity(level, pos, state, flags);
    }

    @Redirect(method = "lambda$hollowOut$4", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/LevelAccessor;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private static boolean iaftfc$preserveCaveBlockEntity(LevelAccessor level, BlockPos pos, BlockState state, int flags) {
        return level.getBlockEntity(pos) == null && level.setBlock(pos, state, flags);
    }
}
