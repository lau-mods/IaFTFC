package net.claustra01.iaftfc.mixin;

import net.claustra01.iaftfc.worldgen.DragonChests;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "com.iafenvoy.iceandfire.world.structure.DragonCaveStructure$DragonCavePiece", remap = false)
public abstract class DragonCaveChestMixin {
    @Redirect(method = "setGoldPile", at = @At(value = "FIELD",
        target = "Lnet/minecraft/world/level/block/Blocks;CHEST:Lnet/minecraft/world/level/block/Block;"))
    private Block iaftfc$useElementChest() {
        return DragonChests.block(DragonChests.element(this));
    }
}
