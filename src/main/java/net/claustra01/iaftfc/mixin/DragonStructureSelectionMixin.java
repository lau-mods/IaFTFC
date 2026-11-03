package net.claustra01.iaftfc.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.iafenvoy.iceandfire.config.IafCommonConfig;
import net.claustra01.iaftfc.worldgen.ClimateSelection;
import net.claustra01.iaftfc.worldgen.DragonClimateWorldgen;
import java.util.List;
import net.dries007.tfc.world.TFCChunkGenerator;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.StructureManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ChunkGenerator.class)
public abstract class DragonStructureSelectionMixin {
    @ModifyExpressionValue(method = "lambda$createStructures$14",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/structure/StructureSet;structures()Ljava/util/List;"))
    private List<StructureSet.StructureSelectionEntry> iaftfc$chooseDragonStructure(
        List<StructureSet.StructureSelectionEntry> entries, StructureManager manager, SectionPos section,
        ChunkAccess chunk, ChunkGeneratorStructureState state, ChunkPos pos, RegistryAccess registries,
        RandomState randomState, StructureTemplateManager templates, Holder<StructureSet> structureSet) {
        if (!((Object) this instanceof TFCChunkGenerator generator) || entries.size() != 3
            || !iaftfc$isTfcOverworld(manager)) return entries;
        ResourceLocation id = structureSet.unwrapKey().orElseThrow().location();
        int salt;
        if (id.equals(ResourceLocation.parse("iceandfire:dragon_roost"))) salt = 3747893;
        else if (id.equals(ResourceLocation.parse("iceandfire:dragon_cave"))) salt = 9821379;
        else return entries;
        var config = IafCommonConfig.INSTANCE.worldGen;
        if (salt == 3747893 && (!((Double) config.generateFireDragonRoostChance.getValue() > 0)
            || !((Double) config.generateIceDragonRoostChance.getValue() > 0)
            || !((Double) config.generateLightningDragonRoostChance.getValue() > 0))) return entries;
        if (salt == 9821379 && (!((Double) config.generateFireDragonCaveChance.getValue() > 0)
            || !((Double) config.generateIceDragonCaveChance.getValue() > 0)
            || !((Double) config.generateLightningDragonCaveChance.getValue() > 0))) return entries;

        if (!structureSet.value().placement().isStructureChunk(state, pos.x, pos.z)) return entries;
        for (var entry : entries) {
            var start = manager.getStartForStructure(section, entry.structure().value(), chunk);
            if (start != null && start.isValid()) return entries;
        }

        long seed = state.getLevelSeed();
        ClimateSelection selection = DragonClimateWorldgen.selection(seed, generator);
        ClimateSelection.Element element = selection.select(
            DragonClimateWorldgen.climate(seed, generator, pos.x, pos.z),
            seed, pos.x, pos.z, salt).element();
        ResourceLocation selected = ResourceLocation.fromNamespaceAndPath("iceandfire",
            element.name().toLowerCase(java.util.Locale.ROOT) + "_dragon_" + (salt == 3747893 ? "roost" : "cave"));
        return entries.stream().filter(entry -> entry.structure().unwrapKey()
            .isPresent() && entry.structure().unwrapKey().orElseThrow().location().equals(selected))
            .findFirst().map(List::of).orElse(entries);
    }

    private boolean iaftfc$isTfcOverworld(StructureManager manager) {
        var world = ((StructureManagerAccessor) manager).iaftfc$getLevel();
        return world instanceof ServerLevel server && server.dimension() == Level.OVERWORLD;
    }
}
