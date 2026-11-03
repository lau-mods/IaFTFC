package net.claustra01.iaftfc.mixin;

import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GameTestServer.class)
public abstract class GameTestWorldPresetMixin {
    @Redirect(method = "lambda$create$1", at = @At(value = "FIELD",
        target = "Lnet/minecraft/world/level/levelgen/presets/WorldPresets;FLAT:Lnet/minecraft/resources/ResourceKey;"))
    private static ResourceKey<WorldPreset> iaftfc$tfcGameTests() {
        return ResourceKey.create(Registries.WORLD_PRESET, ResourceLocation.parse("tfc:overworld"));
    }
}
