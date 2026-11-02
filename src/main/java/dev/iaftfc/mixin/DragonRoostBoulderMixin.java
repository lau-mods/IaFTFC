package dev.iaftfc.mixin;

import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.Tags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "com.iafenvoy.iceandfire.world.structure.DragonRoostStructure$DragonRoostPiece")
public abstract class DragonRoostBoulderMixin {
    @Redirect(method = "generateBoulder", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z", ordinal = 2))
    private boolean iaftfc$isGround(BlockState state, Block block) {
        return state.is(block) || state.is(Tags.Blocks.STONES) || state.is(Tags.Blocks.GRAVELS) || state.is(BlockTags.DIRT);
    }
}
