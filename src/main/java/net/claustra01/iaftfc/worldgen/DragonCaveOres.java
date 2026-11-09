package net.claustra01.iaftfc.worldgen;

import com.iafenvoy.iceandfire.config.IafCommonConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class DragonCaveOres {
    public enum Category {
        GEM,
        METAL
    }

    public static void replaceShell(LevelAccessor level, RandomSource random, Set<BlockPos> shell,
        DragonClimate.Element element, Function<RandomSource, BlockState> palette) {
        Map<BlockPos, Optional<String>> rocks = new HashMap<>();
        for (BlockPos pos : shell) {
            rocks.put(pos, findRock(level, pos));
        }
        for (BlockPos pos : shell) {
            BlockState existing = level.getBlockState(pos);
            if (existing.getBlock() instanceof BaseEntityBlock || existing.getDestroySpeed(level, pos) < 0) {
                continue;
            }
            Optional<BlockState> replacement = Optional.empty();
            if (random.nextDouble() < IafCommonConfig.INSTANCE.dragon.generateOreRatio.getValue()) {
                Optional<String> rock = rocks.get(pos);
                if (rock.isPresent()) {
                    replacement = select(rock.get(), element,
                        random.nextBoolean() ? Category.GEM : Category.METAL);
                }
            }
            level.setBlock(pos, replacement.orElseGet(() -> palette.apply(random)), 2);
        }
    }

    public static Optional<String> findRock(LevelAccessor level, BlockPos origin) {
        for (int radius = 0; radius <= 4; radius++) {
            for (int x = -radius; x <= radius; x++) {
                for (int y = -radius; y <= radius; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        if (Math.max(Math.max(Math.abs(x), Math.abs(y)), Math.abs(z)) != radius) {
                            continue;
                        }
                        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(
                            level.getBlockState(origin.offset(x, y, z)).getBlock());
                        if (id.getNamespace().equals("tfc") && id.getPath().startsWith("rock/raw/")) {
                            return Optional.of(id.getPath().substring("rock/raw/".length()));
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }

    public static Optional<BlockState> select(String rock, DragonClimate.Element element, Category category) {
        String ore = category == Category.GEM
            ? switch (element) {
                case FIRE -> "ruby";
                case ICE -> "sapphire";
                case LIGHTNING -> "topaz";
            }
            : switch (element) {
                case FIRE -> "rich_native_gold";
                case ICE -> "rich_native_silver";
                case LIGHTNING -> "rich_native_copper";
            };
        return block("ore/" + ore + "/" + rock);
    }

    private static Optional<BlockState> block(String path) {
        return BuiltInRegistries.BLOCK.getOptional(ResourceLocation.fromNamespaceAndPath("tfc", path))
            .map(block -> block.defaultBlockState());
    }

    private DragonCaveOres() {
    }
}
