package net.claustra01.iaftfc_test.gametest;

import com.iafenvoy.iceandfire.entity.FireDragonEntity;
import com.iafenvoy.iceandfire.entity.util.dragon.IafDragonDestructionManager;
import com.iafenvoy.iceandfire.item.block.ReturningStateBlock;
import com.iafenvoy.iceandfire.registry.IafEntities;
import java.util.List;
import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.entities.misc.TFCFallingBlockEntity;
import net.dries007.tfc.common.recipes.CollapseRecipe;
import net.dries007.tfc.common.recipes.LandslideRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("iaftfc")
@PrefixGameTestTemplate(false)
public final class DragonBreathGameTests {
    private static final String EMPTY_TEMPLATE = "empty";

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 200)
    public static void tfcTerrainConvertsAfterCeGriefingChecks(GameTestHelper helper) {
        List<TerrainSample> samples = List.of(
            new TerrainSample("tfc:grass/entisol", "grass"),
            new TerrainSample("tfc:dirt/entisol", "dirt"),
            new TerrainSample("tfc:coarse_dirt/entisol", "dirt"),
            new TerrainSample("tfc:rooted_dirt/entisol", "dirt"),
            new TerrainSample("tfc:mud/entisol", "dirt"),
            new TerrainSample("tfc:farmland/entisol", "dirt"),
            new TerrainSample("tfc:grass_path/entisol", "dirt_path"),
            new TerrainSample("tfc:rock/raw/granite", "stone"),
            new TerrainSample("tfc:rock/hardened/granite", "stone"),
            new TerrainSample("tfc:rock/cobble/granite", "cobblestone"),
            new TerrainSample("tfc:rock/mossy_cobble/granite", "cobblestone"),
            new TerrainSample("tfc:rock/gravel/granite", "gravel"));

        for (Element element : Element.values()) {
            for (TerrainSample sample : samples) {
                BlockState source = block(sample.source()).defaultBlockState();
                BlockState result = element.transform(source);
                helper.assertValueEqual(BuiltInRegistries.BLOCK.getKey(result.getBlock()),
                    ResourceLocation.parse("iceandfire:" + element.prefix + "_" + sample.terrain()),
                    element + " breath must convert " + sample.source());
                Property<?> revert = result.getBlock().getStateDefinition().getProperty("revert");
                helper.assertTrue(revert != null, "Converted terrain must have a revert property");
                helper.assertValueEqual(result.getValue(revert).toString(), "false",
                    "TFC terrain conversion must not revert to a different rock or soil");
            }
        }

        BlockState vanillaResult = IafDragonDestructionManager.transformBlockFire(Blocks.DIRT.defaultBlockState());
        helper.assertTrue(vanillaResult.getValue(ReturningStateBlock.REVERTS),
            "CE terrain conversion must retain its own revert behavior");

        var level = helper.getLevel();
        BlockPos breathPos = new BlockPos(3, 3, 3);
        FireDragonEntity dragon = new FireDragonEntity(IafEntities.FIRE_DRAGON.get(), level);
        dragon.setAgeInDays(75);
        boolean mobGriefing = level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
        try {
            level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(true, level.getServer());
            helper.setBlock(breathPos, block("tfc:rock/raw/granite"));
            IafDragonDestructionManager.destroyAreaBreath(level, helper.absolutePos(breathPos), dragon);
            helper.assertBlockPresent(block("iceandfire:chared_stone"), breathPos);
            helper.assertTrue(!helper.getBlockState(breathPos).getValue(ReturningStateBlock.REVERTS),
                "Allowed breath must create non-reverting terrain");

            helper.setBlock(breathPos, block("tfc:rock/raw/granite"));
            level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(false, level.getServer());
            IafDragonDestructionManager.destroyAreaBreath(level, helper.absolutePos(breathPos), dragon);
            helper.assertBlockPresent(block("tfc:rock/raw/granite"), breathPos);
        } finally {
            level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(mobGriefing, level.getServer());
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 200)
    public static void convertedTerrainUsesTfcPhysics(GameTestHelper helper) {
        for (Element element : Element.values()) {
            BlockState stone = block("iceandfire:" + element.prefix + "_stone").defaultBlockState();
            helper.assertTrue(stone.is(TFCTags.Blocks.CAN_COLLAPSE), "Converted stone must collapse");
            helper.assertTrue(stone.is(TFCTags.Blocks.CAN_TRIGGER_COLLAPSE),
                "Converted stone must trigger collapse checks");
            helper.assertTrue(stone.is(TFCTags.Blocks.CAN_START_COLLAPSE),
                "Converted stone must start a collapse");
            helper.assertTrue(CollapseRecipe.canCollapse(stone),
                "Converted stone must resolve through TFC's collapse recipe");

            for (String terrain : List.of("cobblestone", "gravel")) {
                BlockState loose = block("iceandfire:" + element.prefix + "_" + terrain).defaultBlockState();
                helper.assertTrue(loose.is(TFCTags.Blocks.CAN_LANDSLIDE),
                    "Converted " + terrain + " must landslide");
                helper.assertTrue(LandslideRecipe.canLandslide(loose),
                    "Converted " + terrain + " must resolve through TFC's landslide recipe");
            }

            BlockState path = block("iceandfire:" + element.prefix + "_dirt_path").defaultBlockState();
            helper.assertTrue(path.is(TFCTags.Blocks.SUPPORTS_LANDSLIDE),
                "Converted dirt path must support neighboring loose terrain");
        }

        BlockState stone = block("iceandfire:chared_stone").defaultBlockState();
        BlockPos collapsePos = new BlockPos(3, 5, 3);
        for (int x = 1; x <= 5; x++) {
            for (int z = 1; z <= 5; z++) {
                helper.setBlock(new BlockPos(x, 5, z), stone);
            }
        }
        helper.assertTrue(CollapseRecipe.canStartCollapse(helper.getLevel(), helper.absolutePos(collapsePos)),
            "Unsupported converted stone must pass TFC's collapse start check");
        BlockPos absoluteCollapsePos = helper.absolutePos(collapsePos);
        for (int chunkX = (absoluteCollapsePos.getX() - 32) >> 4;
            chunkX <= (absoluteCollapsePos.getX() + 32) >> 4; chunkX++) {
            for (int chunkZ = (absoluteCollapsePos.getZ() - 32) >> 4;
                chunkZ <= (absoluteCollapsePos.getZ() + 32) >> 4; chunkZ++) {
                helper.getLevel().getChunk(chunkX, chunkZ);
            }
        }
        helper.assertTrue(helper.getLevel().isAreaLoaded(absoluteCollapsePos, 32),
            "TFC collapse needs its surrounding chunks loaded");
        AABB collapseArea = new AABB(helper.absolutePos(collapsePos)).inflate(4);
        for (int attempt = 0; attempt < 200
            && helper.getLevel().getEntitiesOfClass(TFCFallingBlockEntity.class, collapseArea).isEmpty(); attempt++) {
            CollapseRecipe.tryTriggerCollapse(helper.getLevel(), helper.absolutePos(collapsePos));
        }
        helper.assertTrue(!helper.getLevel().getEntitiesOfClass(TFCFallingBlockEntity.class, collapseArea).isEmpty(),
            "Triggered TFC collapse must create a falling converted stone");

        for (String terrain : List.of("cobblestone", "gravel")) {
            BlockState loose = block("iceandfire:frozen_" + terrain).defaultBlockState();
            BlockPos landslidePos = new BlockPos(20, terrain.equals("cobblestone") ? 15 : 18, 3);
            helper.setBlock(landslidePos.below(), Blocks.BEDROCK);
            helper.setBlock(landslidePos.above(), Blocks.AIR);
            for (Direction side : Direction.Plane.HORIZONTAL) {
                helper.setBlock(landslidePos.relative(side), Blocks.AIR);
                helper.setBlock(landslidePos.relative(side).below(), Blocks.AIR);
            }
            helper.setBlock(landslidePos, loose);
            helper.assertTrue(LandslideRecipe.getLandslidePos(helper.getLevel(), helper.absolutePos(landslidePos), loose) != null,
                "Unsupported converted " + terrain + " must have a sideways landslide destination");
            AABB landslideArea = new AABB(helper.absolutePos(landslidePos)).inflate(2);
            int fallingBefore = helper.getLevel().getEntitiesOfClass(TFCFallingBlockEntity.class,
                landslideArea).size();
            helper.assertTrue(LandslideRecipe.tryLandslide(helper.getLevel(), helper.absolutePos(landslidePos), loose),
                "TFC must slide converted " + terrain + " sideways off a ledge");
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(TFCFallingBlockEntity.class,
                landslideArea).size() > fallingBefore, "Landslide must create a TFC falling block");

            helper.setBlock(landslidePos, loose);
            BlockState path = block("iceandfire:frozen_dirt_path").defaultBlockState();
            helper.setBlock(landslidePos.north().below(), Blocks.BEDROCK);
            helper.setBlock(landslidePos.east().below(), Blocks.BEDROCK);
            helper.setBlock(landslidePos.north(), path);
            helper.setBlock(landslidePos.east(), path);
            helper.assertTrue(LandslideRecipe.getLandslidePos(helper.getLevel(), helper.absolutePos(landslidePos), loose) == null,
                "Two converted dirt paths must support neighboring loose terrain");
            helper.assertTrue(!LandslideRecipe.tryLandslide(helper.getLevel(), helper.absolutePos(landslidePos), loose),
                "Supported converted terrain must not landslide");
        }

        BlockState normalGrass = block("iceandfire:chared_grass").defaultBlockState();
        BlockState normalDirt = block("iceandfire:chared_dirt").defaultBlockState();
        for (BlockState stable : List.of(normalGrass, normalDirt)) {
            helper.assertTrue(!stable.is(TFCTags.Blocks.CAN_COLLAPSE)
                && !stable.is(TFCTags.Blocks.CAN_START_COLLAPSE)
                && !stable.is(TFCTags.Blocks.CAN_TRIGGER_COLLAPSE)
                && !stable.is(TFCTags.Blocks.CAN_LANDSLIDE),
                "Converted grass and dirt must not become unstable terrain");
        }
        helper.succeed();
    }

    private static Block block(String id) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id));
    }

    private record TerrainSample(String source, String terrain) {
    }

    private enum Element {
        FIRE("chared") {
            @Override
            BlockState transform(BlockState state) {
                return IafDragonDestructionManager.transformBlockFire(state);
            }
        },
        ICE("frozen") {
            @Override
            BlockState transform(BlockState state) {
                return IafDragonDestructionManager.transformBlockIce(state);
            }
        },
        LIGHTNING("crackled") {
            @Override
            BlockState transform(BlockState state) {
                return IafDragonDestructionManager.transformBlockLightning(state);
            }
        };

        private final String prefix;

        Element(String prefix) {
            this.prefix = prefix;
        }

        abstract BlockState transform(BlockState state);
    }

    private DragonBreathGameTests() {
    }
}
