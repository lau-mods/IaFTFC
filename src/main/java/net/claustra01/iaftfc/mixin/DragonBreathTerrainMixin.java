package net.claustra01.iaftfc.mixin;

import com.iafenvoy.iceandfire.entity.util.dragon.IafDragonDestructionManager;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.claustra01.iaftfc.dragon.BreathTerrainConversion;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(IafDragonDestructionManager.class)
public abstract class DragonBreathTerrainMixin {
    @ModifyReturnValue(method = "transformBlockFire", at = @At("RETURN"))
    private static BlockState iaftfc$convertFire(BlockState ceResult, BlockState source) {
        return BreathTerrainConversion.convert(source, ceResult, BreathTerrainConversion.Element.FIRE);
    }

    @ModifyReturnValue(method = "transformBlockIce", at = @At("RETURN"))
    private static BlockState iaftfc$convertIce(BlockState ceResult, BlockState source) {
        return BreathTerrainConversion.convert(source, ceResult, BreathTerrainConversion.Element.ICE);
    }

    @ModifyReturnValue(method = "transformBlockLightning", at = @At("RETURN"))
    private static BlockState iaftfc$convertLightning(BlockState ceResult, BlockState source) {
        return BreathTerrainConversion.convert(source, ceResult, BreathTerrainConversion.Element.LIGHTNING);
    }
}
