package net.claustra01.iaftfc.worldgen;

import com.iafenvoy.iceandfire.config.IafCommonConfig;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import net.claustra01.iaftfc.config.IaFTFCConfig;
import net.claustra01.iaftfc.mixin.DragonStructureSpacingAccessor;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;

public final class StructureSpacingResolver {
    public static final int FIRE_ROOST_SALT = 3747893;
    public static final int ICE_ROOST_SALT = 3747894;
    public static final int LIGHTNING_ROOST_SALT = 3747895;
    public static final int FIRE_CAVE_SALT = 9821379;
    public static final int ICE_CAVE_SALT = 9821380;
    public static final int LIGHTNING_CAVE_SALT = 9821381;

    public enum Kind {
        ROOST,
        CAVE
    }

    public static int spacing(Kind kind, Object ceWorldGenConfig) {
        Integer ceSpacing = readCeSpacing(ceWorldGenConfig,
            kind == Kind.ROOST ? "dragonRoostSpacing" : "dragonCaveSpacing");
        if (ceSpacing != null) {
            return ceSpacing;
        }
        return kind == Kind.ROOST
            ? IaFTFCConfig.DRAGON_ROOST_SPACING.get()
            : IaFTFCConfig.DRAGON_CAVE_SPACING.get();
    }

    public static int separation(int configuredSpacing, int originalSeparation) {
        return Math.min(originalSeparation, configuredSpacing - 1);
    }

    public static void apply(MinecraftServer server) {
        int roost = spacing(Kind.ROOST, IafCommonConfig.INSTANCE.worldGen);
        int cave = spacing(Kind.CAVE, IafCommonConfig.INSTANCE.worldGen);
        Registry<StructureSet> sets = server.registryAccess().registryOrThrow(Registries.STRUCTURE_SET);
        for (String id : new String[] {"iceandfire:dragon_roost", "iaftfc:ice_dragon_roost",
            "iaftfc:lightning_dragon_roost"}) {
            update(sets, id, roost, 10);
        }
        for (String id : new String[] {"iceandfire:dragon_cave", "iaftfc:ice_dragon_cave",
            "iaftfc:lightning_dragon_cave"}) {
            update(sets, id, cave, 20);
        }
    }

    private static void update(Registry<StructureSet> sets, String id, int spacing, int originalSeparation) {
        StructureSet set = sets.get(ResourceLocation.parse(id));
        RandomSpreadStructurePlacement placement = (RandomSpreadStructurePlacement) set.placement();
        DragonStructureSpacingAccessor accessor = (DragonStructureSpacingAccessor) placement;
        accessor.iaftfc$setSeparation(separation(spacing, originalSeparation));
        accessor.iaftfc$setSpacing(spacing);
    }

    private static Integer readCeSpacing(Object config, String fieldName) {
        try {
            Field field = config.getClass().getField(fieldName);
            Object entry = field.get(config);
            Method getValue = entry.getClass().getMethod("getValue");
            Object value = getValue.invoke(entry);
            return value instanceof Number number ? number.intValue() : null;
        } catch (NoSuchFieldException exception) {
            return null;
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException exception) {
            throw new IllegalStateException("Unable to read the Ice and Fire spacing setting", exception);
        }
    }

    private StructureSpacingResolver() {
    }
}
