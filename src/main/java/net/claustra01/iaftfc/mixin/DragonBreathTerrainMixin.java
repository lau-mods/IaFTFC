package net.claustra01.iaftfc.mixin;

import com.iafenvoy.iceandfire.entity.util.dragon.IafDragonDestructionManager;
import com.iafenvoy.iceandfire.item.block.ReturningStateBlock;
import com.iafenvoy.iceandfire.item.block.FallingReturningStateBlock;
import com.iafenvoy.iceandfire.registry.IafBlocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(IafDragonDestructionManager.class)
public abstract class DragonBreathTerrainMixin {
    private static final TagKey<net.minecraft.world.level.block.Block> GRASS = TagKey.create(Registries.BLOCK,
        ResourceLocation.fromNamespaceAndPath("tfc", "grass"));
    private static final TagKey<net.minecraft.world.level.block.Block> DIRT = TagKey.create(Registries.BLOCK,
        ResourceLocation.fromNamespaceAndPath("tfc", "dirt"));
    private static final TagKey<net.minecraft.world.level.block.Block> GRAVELS = TagKey.create(Registries.BLOCK,
        ResourceLocation.fromNamespaceAndPath("c", "gravels"));
    private static final TagKey<net.minecraft.world.level.material.Fluid> FRESH_WATER = TagKey.create(Registries.FLUID,
        ResourceLocation.fromNamespaceAndPath("tfc", "any_fresh_water"));

    @Redirect(method = "attackBlock(Lnet/minecraft/world/level/Level;Lcom/iafenvoy/iceandfire/entity/DragonBaseEntity;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V",
        at = @At(value = "INVOKE", target = "Lcom/iafenvoy/iceandfire/entity/util/dragon/IafDragonDestructionManager;transformBlockFire(Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private static BlockState iaftfc$fireTerrain(BlockState state) {
        if (state.is(GRASS)) return IafBlocks.CHARRED_GRASS.get().defaultBlockState().setValue(ReturningStateBlock.REVERTS, true);
        if (state.is(DIRT)) return IafBlocks.CHARRED_DIRT.get().defaultBlockState().setValue(ReturningStateBlock.REVERTS, true);
        if (iaftfc$isTfcGravel(state)) return IafBlocks.CHARRED_GRAVEL.get().defaultBlockState().setValue(FallingReturningStateBlock.REVERTS, true);
        return IafDragonDestructionManager.transformBlockFire(state);
    }

    @Redirect(method = "attackBlock(Lnet/minecraft/world/level/Level;Lcom/iafenvoy/iceandfire/entity/DragonBaseEntity;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V",
        at = @At(value = "INVOKE", target = "Lcom/iafenvoy/iceandfire/entity/util/dragon/IafDragonDestructionManager;transformBlockIce(Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private static BlockState iaftfc$iceTerrain(BlockState state) {
        if (state.is(GRASS)) return IafBlocks.FROZEN_GRASS.get().defaultBlockState().setValue(ReturningStateBlock.REVERTS, true);
        if (state.is(DIRT)) return IafBlocks.FROZEN_DIRT.get().defaultBlockState().setValue(ReturningStateBlock.REVERTS, true);
        if (iaftfc$isTfcGravel(state)) return IafBlocks.FROZEN_GRAVEL.get().defaultBlockState().setValue(FallingReturningStateBlock.REVERTS, true);
        if (state.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock
            && state.getFluidState().is(FRESH_WATER)) return Blocks.ICE.defaultBlockState();
        return IafDragonDestructionManager.transformBlockIce(state);
    }

    @Redirect(method = "attackBlock(Lnet/minecraft/world/level/Level;Lcom/iafenvoy/iceandfire/entity/DragonBaseEntity;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V",
        at = @At(value = "INVOKE", target = "Lcom/iafenvoy/iceandfire/entity/util/dragon/IafDragonDestructionManager;transformBlockLightning(Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private static BlockState iaftfc$lightningTerrain(BlockState state) {
        if (state.is(GRASS)) return IafBlocks.CRACKLED_GRASS.get().defaultBlockState().setValue(ReturningStateBlock.REVERTS, true);
        if (state.is(DIRT)) return IafBlocks.CRACKLED_DIRT.get().defaultBlockState().setValue(ReturningStateBlock.REVERTS, true);
        if (iaftfc$isTfcGravel(state)) return IafBlocks.CRACKLED_GRAVEL.get().defaultBlockState().setValue(FallingReturningStateBlock.REVERTS, true);
        return IafDragonDestructionManager.transformBlockLightning(state);
    }

    private static boolean iaftfc$isTfcGravel(BlockState state) {
        return state.is(GRAVELS) && BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace().equals("tfc");
    }
}
