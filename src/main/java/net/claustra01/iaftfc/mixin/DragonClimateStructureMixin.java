package net.claustra01.iaftfc.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import java.util.List;
import net.claustra01.iaftfc.worldgen.DragonClimate;
import net.dries007.tfc.world.chunkdata.ChunkData;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ChunkGenerator.class)
public abstract class DragonClimateStructureMixin {
    @ModifyExpressionValue(
        method = "lambda$createStructures$14",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/structure/StructureSet;structures()Ljava/util/List;")
    )
    private List<StructureSet.StructureSelectionEntry> iaftfc$rejectWrongClimate(
        List<StructureSet.StructureSelectionEntry> entries, StructureManager structureManager,
        SectionPos sectionPos, ChunkAccess chunk) {
        int x = chunk.getPos().getMiddleBlockX();
        int z = chunk.getPos().getMiddleBlockZ();
        ChunkData data = ChunkData.get(chunk);
        var climate = DragonClimate.select(data.getAverageSeaLevelTemp(x, z), data.getAverageGroundwater(x, z));
        return entries.stream().filter(entry -> {
            ResourceLocation id = entry.structure().unwrapKey().map(key -> key.location()).orElse(null);
            DragonClimate.Element element = element(id);
            return element == null || climate.filter(element::equals).isPresent();
        }).toList();
    }

    private static DragonClimate.Element element(ResourceLocation id) {
        if (id == null || !id.getNamespace().equals("iceandfire")) {
            return null;
        }
        String path = id.getPath();
        if (path.equals("fire_dragon_roost") || path.equals("fire_dragon_cave")) {
            return DragonClimate.Element.FIRE;
        }
        if (path.equals("ice_dragon_roost") || path.equals("ice_dragon_cave")) {
            return DragonClimate.Element.ICE;
        }
        if (path.equals("lightning_dragon_roost") || path.equals("lightning_dragon_cave")) {
            return DragonClimate.Element.LIGHTNING;
        }
        return null;
    }
}
