package net.claustra01.iaftfc.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Locale;
import java.util.stream.Stream;
import net.dries007.tfc.world.TFCChunkGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

public final class ElementalPlacement extends PlacementModifier {
    private static final Codec<ClimateSelection.Element> ELEMENT_CODEC = Codec.STRING.comapFlatMap(value -> {
        try {
            return DataResult.success(ClimateSelection.Element.valueOf(value.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException exception) {
            return DataResult.error(() -> "Unknown element: " + value);
        }
    }, value -> value.name().toLowerCase(Locale.ROOT));
    public static final MapCodec<ElementalPlacement> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        ELEMENT_CODEC.fieldOf("element").forGetter(placement -> placement.element),
        Codec.BOOL.fieldOf("lily").forGetter(placement -> placement.lily)
    ).apply(instance, ElementalPlacement::new));

    private final ClimateSelection.Element element;
    private final boolean lily;
    private final TagKey<Block> ground;

    public ElementalPlacement(ClimateSelection.Element element, boolean lily) {
        this.element = element;
        this.lily = lily;
        this.ground = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath("iaftfc", "lily_ground/" + element.name().toLowerCase(Locale.ROOT)));
    }

    @Override
    public Stream<BlockPos> getPositions(PlacementContext context, RandomSource random, BlockPos pos) {
        if (!(context.generator() instanceof TFCChunkGenerator generator)
            || context.getLevel().getLevel().dimension() != Level.OVERWORLD) {
            return Stream.of(pos);
        }
        if (lily) {
            return context.getBlockState(pos.below()).is(ground) ? Stream.of(pos) : Stream.empty();
        }
        long seed = context.getLevel().getSeed();
        ClimateSelection selection = DragonClimateWorldgen.selection(seed, generator);
        double[] scores = selection.scores(DragonClimateWorldgen.climate(seed, generator,
            pos.getX() >> 4, pos.getZ() >> 4));
        int index = element.ordinal();
        return scores[index] >= scores[0] && scores[index] >= scores[1] && scores[index] >= scores[2]
            ? Stream.of(pos) : Stream.empty();
    }

    @Override
    public PlacementModifierType<?> type() {
        return ElementalPlacements.CLIMATE.get();
    }
}
