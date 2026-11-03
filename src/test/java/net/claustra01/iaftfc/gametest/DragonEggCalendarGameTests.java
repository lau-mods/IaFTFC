package net.claustra01.iaftfc.gametest;

import com.iafenvoy.iceandfire.config.IafCommonConfig;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.DragonEggEntity;
import com.iafenvoy.iceandfire.item.block.entity.EggInIceBlockEntity;
import com.iafenvoy.iceandfire.item.DragonHornItem;
import com.iafenvoy.iceandfire.item.component.DragonHornComponent;
import com.iafenvoy.iceandfire.registry.IafDragonColors;
import com.iafenvoy.iceandfire.registry.IafBlocks;
import com.iafenvoy.iceandfire.registry.IafEntities;
import com.iafenvoy.iceandfire.registry.IafDataComponents;
import com.iafenvoy.iceandfire.registry.IafItems;
import net.claustra01.iaftfc.dragon.CalendarDragonGrowth;
import net.claustra01.iaftfc.worldgen.DragonClimateWorldgen;
import net.dries007.tfc.common.blockentities.CrucibleBlockEntity;
import net.dries007.tfc.util.calendar.Calendars;
import net.dries007.tfc.util.climate.OverworldClimateModel;
import net.dries007.tfc.util.tracker.WeatherHelpers;
import net.dries007.tfc.util.tracker.WorldTracker;
import net.dries007.tfc.world.TFCChunkGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("iaftfc")
@PrefixGameTestTemplate(false)
public final class DragonEggCalendarGameTests {
    private static final BlockPos POS = new BlockPos(2, 300, 2);

    @GameTest(template = "empty")
    public static void fireEggDirectHeat(GameTestHelper helper) {
        DragonEggEntity egg = egg(helper, IafDragonColors.RED);
        helper.setBlock(POS, Blocks.AIR);
        helper.setBlock(POS.below(), block("tfc:crucible"));
        CrucibleBlockEntity crucible = (CrucibleBlockEntity) helper.getBlockEntity(POS.below());
        for (int temperature : new int[] {1499, 1500, 2000}) {
            crucible.getSyncableData().set(0, temperature);
            helper.assertTrue(crucible.getTemperature() == temperature, "Crucible temperature was not set");
            int before = egg.getDragonAge();
            egg.updateEggCondition();
            helper.assertTrue(egg.getDragonAge() == before + (temperature >= 1500 ? 1 : 0),
                "Fire egg advanced at the wrong temperature: " + temperature);
        }
        helper.setBlock(POS.below(), Blocks.AIR);
        for (BlockPos offset : new BlockPos[] {POS.below().east(), POS.east()}) {
            helper.setBlock(offset, block("tfc:crucible"));
            ((CrucibleBlockEntity) helper.getBlockEntity(offset)).getSyncableData().set(0, 2000);
            int before = egg.getDragonAge();
            egg.updateEggCondition();
            helper.assertTrue(egg.getDragonAge() == before, "Remote heat advanced a fire egg");
            helper.setBlock(offset, Blocks.AIR);
        }
        helper.setBlock(POS.below(), Blocks.NETHERRACK);
        helper.setBlock(POS, Blocks.FIRE);
        int before = egg.getDragonAge();
        egg.updateEggCondition();
        helper.assertTrue(egg.getDragonAge() == before + 1, "Original IaF fire condition stopped working");
        egg.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void iceEggFreshWater(GameTestHelper helper) {
        long successSeed = seedFor(0);
        long failureSeed = seedFor(1);
        for (String water : new String[] {"tfc:fluid/river_water", "minecraft:water", "tfc:fluid/salt_water",
            "tfc:fluid/spring_water"}) {
            helper.setBlock(POS, block(water));
            boolean fresh = water.equals("tfc:fluid/river_water") || water.equals("minecraft:water")
                || water.equals("tfc:fluid/spring_water") && helper.getLevel()
                    .getFluidState(helper.absolutePos(POS)).is(net.minecraft.tags.TagKey.create(
                        net.minecraft.core.registries.Registries.FLUID,
                        ResourceLocation.fromNamespaceAndPath("tfc", "any_fresh_water")));
            DragonEggEntity egg = egg(helper, IafDragonColors.BLUE);
            egg.getRandom().setSeed(successSeed);
            egg.updateEggCondition();
            helper.assertTrue(helper.getBlockState(POS).is(fresh ? IafBlocks.EGG_IN_ICE.get() : block(water)),
                "Ice egg water classification differs for " + water + ": actual="
                    + BuiltInRegistries.BLOCK.getKey(helper.getBlockState(POS).getBlock()) + ", fluid="
                    + BuiltInRegistries.FLUID.getKey(helper.getLevel().getFluidState(helper.absolutePos(POS)).getType()));
            if (fresh) {
                EggInIceBlockEntity ice = (EggInIceBlockEntity) helper.getBlockEntity(POS);
                helper.assertTrue(ice.type == IafDragonColors.BLUE, "Egg color was lost in ice");
            }
            egg.discard();
        }
        helper.setBlock(POS, block("tfc:fluid/river_water"));
        DragonEggEntity egg = egg(helper, IafDragonColors.BLUE);
        egg.getRandom().setSeed(failureSeed);
        egg.updateEggCondition();
        helper.assertBlockPresent(block("tfc:fluid/river_water"), POS);
        egg.discard();

        helper.setBlock(POS, IafBlocks.EGG_IN_ICE.get());
        EggInIceBlockEntity ice = (EggInIceBlockEntity) helper.getBlockEntity(POS);
        ice.type = IafDragonColors.BLUE;
        int bornTime = IafCommonConfig.INSTANCE.dragon.eggBornTime.getValue();
        ice.age = bornTime - 2;
        BlockPos absolute = helper.absolutePos(POS);
        EggInIceBlockEntity.tickEgg(helper.getLevel(), absolute, helper.getBlockState(POS), ice);
        helper.assertTrue(ice.age == bornTime - 1 && helper.getBlockState(POS).is(IafBlocks.EGG_IN_ICE.get()),
            "Ice timer advanced by more than one game tick");
        EggInIceBlockEntity.tickEgg(helper.getLevel(), absolute, helper.getBlockState(POS), ice);
        helper.assertTrue(ice.age == bornTime && helper.getBlockState(POS).is(Blocks.WATER),
            "Ice timer did not hatch at IaF's eggBornTime");
        DragonBaseEntity newborn = helper.getLevel().getEntitiesOfClass(DragonBaseEntity.class,
            new AABB(absolute).inflate(2)).stream().findFirst().orElse(null);
        helper.assertTrue(newborn != null && newborn.getAgeInDays() == 0 && newborn.isBaby(),
            "Egg in Ice did not spawn a newborn dragon");
        newborn.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void lightningEggRainAndSky(GameTestHelper helper) {
        var level = helper.getLevel();
        boolean wasRaining = level.getLevelData().isRaining();
        boolean wasThundering = level.getLevelData().isThundering();
        CompoundTag originalCalendar = Calendars.SERVER.write();
        var climate = WorldTracker.get(level).getClimateModel();
        helper.assertTrue(climate instanceof OverworldClimateModel, "GameTest requires TFC Overworld climate");
        var generator = (TFCChunkGenerator) level.getChunkSource().getGenerator();
        BlockPos origin = helper.absolutePos(POS);
        BlockPos rainyPos = null;
        double bestClimate = Double.NEGATIVE_INFINITY;
        for (int dx = -32; dx <= 32; dx++) {
            for (int dz = -32; dz <= 32; dz++) {
                int x = origin.getX() + dx * 256;
                int z = origin.getZ() + dz * 256;
                var point = DragonClimateWorldgen.climate(level.getSeed(), generator, x >> 4, z >> 4);
                double suitability = point.rainfall() + (point.temperature() > 0 ? 1000 : 0);
                if (suitability > bestClimate) {
                    bestClimate = suitability;
                    rainyPos = new BlockPos(x, 80, z);
                }
            }
        }
        helper.assertTrue(rainyPos != null, "No TFC climate position near the test");
        level.getChunk(rainyPos.getX() >> 4, rainyPos.getZ() >> 4);
        int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE, rainyPos.getX(), rainyPos.getZ());
        BlockPos eggPos = null;
        for (int y = surface + 2; y < level.getMaxBuildHeight() - 3; y++) {
            BlockPos candidate = rainyPos.atY(y);
            level.getBlockState(candidate.above(2));
            if (level.canSeeSky(candidate.above())) {
                eggPos = candidate;
                break;
            }
        }
        helper.assertTrue(eggPos != null, "No open sky above the TFC rain position");
        BlockPos openEggPos = eggPos;
        long now = Calendars.SERVER.getCalendarTicks();
        long rainyTick = -1;
        for (int step = 1; step <= 240; step++) {
            long tick = now + step * 6000L;
            if (WeatherHelpers.isPrecipitating(climate.getRain(tick),
                climate.getInstantRainfall(level, openEggPos, tick, Calendars.SERVER.getCalendarDaysInMonth()))
                && climate.getInstantTemperature(level, openEggPos, tick,
                    Calendars.SERVER.getCalendarDaysInMonth()) > 0) {
                rainyTick = tick;
                break;
            }
        }
        helper.assertTrue(rainyTick >= 0, "No TFC rainfall at the generated egg biome");
        Calendars.SERVER.skipForwardBy(rainyTick - now);
        DragonEggEntity egg = IafEntities.DRAGON_EGG.get().create(level);
        egg.setEggType(IafDragonColors.ELECTRIC);
        egg.setNoGravity(true);
        egg.setPos(openEggPos.getX() + 0.5, openEggPos.getY(), openEggPos.getZ() + 0.5);
        level.addFreshEntity(egg);
        var cover = level.getBlockState(openEggPos.above(2));
        Runnable restore = () -> {
            level.setBlockAndUpdate(openEggPos.above(2), cover);
            Calendars.SERVER.read(originalCalendar);
            level.setWeatherParameters(0, 1200, wasRaining, wasThundering);
            level.setRainLevel(wasRaining ? 1 : 0);
            level.setThunderLevel(wasThundering ? 1 : 0);
            egg.discard();
        };
        try {
            checkLightning(helper, egg, openEggPos, false, true);
            checkLightning(helper, egg, openEggPos, true, true);
            level.setBlockAndUpdate(openEggPos.above(2), Blocks.STONE.defaultBlockState());
        } catch (RuntimeException failure) {
            restore.run();
            throw failure;
        }
        helper.runAfterDelay(10, () -> {
            try {
                checkLightning(helper, egg, openEggPos, false, false);
                checkLightning(helper, egg, openEggPos, true, false);
            } finally {
                restore.run();
            }
            helper.succeed();
        });
    }

    private static void checkLightning(GameTestHelper helper, DragonEggEntity egg, BlockPos absolute,
                                       boolean raining, boolean sky) {
        var level = helper.getLevel();
        level.setWeatherParameters(0, 1200, raining, false);
        level.setRainLevel(raining ? 1 : 0);
        level.setThunderLevel(0);
        helper.assertTrue(level.canSeeSky(absolute.above()) == sky,
            "Sky precondition does not hold: raining=" + raining + ", expected=" + sky + ", actual="
                + level.canSeeSky(absolute.above()) + ", pos=" + absolute);
        helper.assertTrue(level.isRainingAt(absolute) == (raining && sky),
            "TFC rain precondition does not hold: raining=" + raining + ", sky=" + sky
                + ", precipitation=" + level.getBiome(absolute).value().getPrecipitationAt(absolute));
        int before = egg.getDragonAge();
        egg.updateEggCondition();
        helper.assertTrue(egg.getDragonAge() == before + (raining && sky ? 1 : 0),
            "Lightning egg ignored the rain and sky combination");
    }

    @GameTest(template = "empty")
    public static void eggTicksAndCalendarGrowth(GameTestHelper helper) {
        CompoundTag originalCalendar = Calendars.SERVER.write();
        int bornTime = IafCommonConfig.INSTANCE.dragon.eggBornTime.getValue();
        helper.setBlock(POS.below(), Blocks.NETHERRACK);
        helper.setBlock(POS, Blocks.FIRE);
        try {
            for (float rate : new float[] {1, 2}) {
                Calendars.SERVER.setCalendarTickRate(rate);
                DragonEggEntity egg = egg(helper, IafDragonColors.RED);
                egg.setDragonAge(bornTime - 1);
                egg.updateEggCondition();
                helper.assertTrue(egg.getDragonAge() == bornTime, "Egg timer changed with calendar rate " + rate);
                egg.discard();
                helper.assertTrue(Calendars.SERVER.getCalendarTickFromOffset(12000)
                    - Calendars.SERVER.getCalendarTicks() == (long) (12000 * rate),
                    "Calendar day length did not change with configured rate");
                DragonBaseEntity rateDragon = IafEntities.FIRE_DRAGON.get().create(helper.getLevel());
                rateDragon.setNoAi(true);
                rateDragon.setAgeInDays(1);
                rateDragon.moveTo(helper.absolutePos(POS.east(5)), 0, 0);
                helper.getLevel().addFreshEntity(rateDragon);
                CalendarDragonGrowth rateGrowth = (CalendarDragonGrowth) rateDragon;
                rateGrowth.iaftfc$resetCalendar(Calendars.SERVER.getCalendarTicks());
                rateDragon.logic.updateDragonServer();
                helper.assertTrue(rateDragon.getAgeInTicks() == 24000,
                    "Original IaF game-tick growth was not suppressed");
                long playerTicks = Calendars.SERVER.getTicks();
                Calendars.SERVER.skipForwardBy((long) (12000 * rate));
                helper.assertTrue(Calendars.SERVER.getTicks() == playerTicks + 12000,
                    "The configured rate did not advance 12000 game ticks");
                rateDragon.logic.updateDragonServer();
                helper.assertTrue(rateDragon.getAgeInDays() == (int) rate,
                    "The same 12000 game ticks did not follow the calendar rate " + rate);
                Calendars.SERVER.skipForwardBy((long) (12000 * rate));
                helper.assertTrue(Calendars.SERVER.getTicks() == playerTicks + 24000,
                    "The configured rate did not advance 24000 game ticks");
                rateDragon.logic.updateDragonServer();
                helper.assertTrue(rateDragon.getAgeInDays() == 1 + (int) rate,
                    "The same 24000 game ticks did not follow the calendar rate " + rate);
                rateDragon.discard();
            }

            DragonBaseEntity dragon = IafEntities.FIRE_DRAGON.get().create(helper.getLevel());
            dragon.setNoAi(true);
            dragon.setAgeInDays(1);
            dragon.moveTo(helper.absolutePos(POS.east(5)), 0, 0);
            helper.getLevel().addFreshEntity(dragon);
            CalendarDragonGrowth growth = (CalendarDragonGrowth) dragon;
            long now = Calendars.SERVER.getCalendarTicks();
            growth.iaftfc$resetCalendar(now);
            growth.iaftfc$advanceCalendar(now + 12000);
            helper.assertTrue(dragon.getAgeInDays() == 1, "Half a calendar day grew the dragon");
            growth.iaftfc$advanceCalendar(now + 24000);
            helper.assertTrue(dragon.getAgeInDays() == 2, "One calendar day did not grow the dragon");
            growth.iaftfc$advanceCalendar(now + 36000);
            CompoundTag saved = dragon.saveWithoutId(new CompoundTag());
            DragonBaseEntity restored = IafEntities.FIRE_DRAGON.get().create(helper.getLevel());
            restored.load(saved);
            CalendarDragonGrowth loadedGrowth = (CalendarDragonGrowth) restored;
            loadedGrowth.iaftfc$advanceCalendar(now + 48000);
            helper.assertTrue(restored.getAgeInDays() == 3, "Saved remainder or unloaded time was lost");
            loadedGrowth.iaftfc$advanceCalendar(now + 48000);
            helper.assertTrue(restored.getAgeInDays() == 3, "Calendar time was counted twice");
            restored.growDragon(1);
            loadedGrowth.iaftfc$advanceCalendar(now + 72000);
            helper.assertTrue(restored.getAgeInDays() == 5, "Dragon Meal age addition was replaced");
            restored.setAgingDisabled(true);
            loadedGrowth.iaftfc$advanceCalendar(now + 96000);
            helper.assertTrue(restored.getAgeInDays() == 5, "Aging-disabled dragon grew");
            restored.setAgingDisabled(false);
            loadedGrowth.iaftfc$resetCalendar(now + 120000);
            loadedGrowth.iaftfc$advanceCalendar(now + 144000);
            helper.assertTrue(restored.getAgeInDays() == 6, "Horn-style reset counted stored time");
            dragon.discard();
            restored.discard();
        } finally {
            Calendars.SERVER.read(originalCalendar);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void hornStorageSkipsCalendar(GameTestHelper helper) {
        CompoundTag originalCalendar = Calendars.SERVER.write();
        var level = helper.getLevel();
        var player = FakePlayerFactory.getMinecraft(level);
        BlockPos absolute = helper.absolutePos(POS);
        helper.setBlock(POS.below(), Blocks.STONE);
        DragonBaseEntity dragon = IafEntities.FIRE_DRAGON.get().create(level);
        dragon.setAgeInDays(10);
        dragon.setTame(true, false);
        dragon.setOwnerUUID(player.getUUID());
        dragon.moveTo(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5);
        level.addFreshEntity(dragon);
        ((CalendarDragonGrowth) dragon).iaftfc$resetCalendar(Calendars.SERVER.getCalendarTicks());
        CompoundTag stored = new CompoundTag();
        dragon.save(stored);
        ItemStack horn = new ItemStack(IafItems.DRAGON_HORN.get());
        horn.set(IafDataComponents.DRAGON_HORN.get(), new DragonHornComponent(
            BuiltInRegistries.ENTITY_TYPE.getKey(dragon.getType()), dragon.getUUID(), stored));
        dragon.discard();
        player.setItemInHand(InteractionHand.MAIN_HAND, horn);
        try {
            ItemStack heldHorn = player.getItemInHand(InteractionHand.MAIN_HAND);
            helper.assertTrue(heldHorn.has(IafDataComponents.DRAGON_HORN.get()),
                "Horn does not contain the stored dragon");
            Calendars.SERVER.skipForwardBy(48000);
            ((DragonHornItem) heldHorn.getItem()).useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(absolute.below()), Direction.UP, absolute.below(), false)));
            DragonBaseEntity released = level.getEntitiesOfClass(DragonBaseEntity.class,
                new AABB(absolute).inflate(3), entity -> entity.getUUID().equals(dragon.getUUID()))
                .stream().findFirst().orElse(null);
            helper.assertTrue(released != null && released.getAgeInDays() == 10,
                "Horn storage incorrectly grew the dragon");
            released.logic.updateDragonServer();
            helper.assertTrue(released.getAgeInDays() == 10, "Released dragon counted stored time on first tick");
            released.discard();
        } finally {
            Calendars.SERVER.read(originalCalendar);
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }
        helper.succeed();
    }

    private static DragonEggEntity egg(GameTestHelper helper, com.iafenvoy.iceandfire.data.DragonColor color) {
        DragonEggEntity egg = IafEntities.DRAGON_EGG.get().create(helper.getLevel());
        egg.setEggType(color);
        egg.setNoGravity(true);
        BlockPos absolute = helper.absolutePos(POS);
        egg.setPos(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5);
        helper.getLevel().addFreshEntity(egg);
        return egg;
    }

    private static Block block(String id) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id));
    }

    private static long seedFor(int expected) {
        for (long seed = 0; seed < 10000; seed++) {
            if ((RandomSource.create(seed).nextInt(500) == 0 ? 0 : 1) == expected) return seed;
        }
        throw new IllegalStateException("No deterministic egg random seed");
    }
}
