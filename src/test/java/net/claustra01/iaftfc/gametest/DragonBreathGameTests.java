package net.claustra01.iaftfc.gametest;

import com.iafenvoy.iceandfire.config.IafCommonConfig;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.util.dragon.DragonUtils;
import com.iafenvoy.iceandfire.entity.util.dragon.IafDragonDestructionManager;
import com.iafenvoy.iceandfire.item.block.entity.DragonForgeBlockEntity;
import com.iafenvoy.iceandfire.item.block.entity.DragonForgeInputBlockEntity;
import com.iafenvoy.iceandfire.registry.tag.IafBlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("iaftfc")
@PrefixGameTestTemplate(false)
public final class DragonBreathGameTests {
    private static final String[] ELEMENTS = {"fire", "ice", "lightning"};
    private static final String[] STONES = {"chared", "frozen", "crackled"};
    private static final BlockPos TERRAIN_POS = new BlockPos(2, 300, 2);

    @GameTest(template = "empty")
    public static void breathConvertsTfcTerrain(GameTestHelper helper) {
        for (int index = 0; index < ELEMENTS.length; index++) {
            DragonBaseEntity dragon = dragon(helper, index);
            String stone = STONES[index];
            breathChanges(helper, dragon, "tfc:rock/raw/granite", "iceandfire:" + stone + "_stone");
            breathChanges(helper, dragon, "tfc:grass/entisol", "iceandfire:" + stone + "_grass");
            breathChanges(helper, dragon, "tfc:dirt/entisol", "iceandfire:" + stone + "_dirt");
            breathChanges(helper, dragon, "tfc:rock/gravel/granite", "iceandfire:" + stone + "_gravel");
            breathChanges(helper, dragon, "tfc:wood/log/oak", index == 1
                ? "iceandfire:frozen_splinters" : "iceandfire:ash");
            breathChanges(helper, dragon, "tfc:crop/wheat", "minecraft:air");
            for (String water : new String[] {"tfc:fluid/river_water", "minecraft:water", "tfc:fluid/salt_water"}) {
                if (index == 1 && !water.equals("tfc:fluid/salt_water")) {
                    breathChanges(helper, dragon, water, "minecraft:ice");
                } else {
                    helper.setBlock(TERRAIN_POS, block(water));
                    for (int pulse = 0; pulse < 20; pulse++) breathe(helper, dragon, TERRAIN_POS);
                    helper.assertBlockPresent(block(water), TERRAIN_POS);
                }
            }
            dragon.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "dragon_forge")
    public static void breathFeedsForgeAndDamagesEntity(GameTestHelper helper) {
        BlockPos center = new BlockPos(3, 3, 13);
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dy == 0 && dx == 0 && dz == 0) continue;
                    String id = dy == 0 && dx == 0 && dz == -1 ? "iceandfire:dragonforge_fire_input"
                        : dy != 0 && dx != 0 && dz != 0 ? "iceandfire:dragon_bone_block"
                        : "iceandfire:dragonforge_fire_brick";
                    helper.setBlock(center.offset(dx, dy, dz), block(id));
                }
            }
        }
        helper.setBlock(center, block("iceandfire:dragonforge_fire_core_disabled"));
        BlockPos inputPos = center.north();
        helper.runAfterDelay(3, () -> {
            DragonForgeBlockEntity core = (DragonForgeBlockEntity) helper.getBlockEntity(center);
            DragonForgeInputBlockEntity input = (DragonForgeInputBlockEntity) helper.getBlockEntity(inputPos);
            BlockPos absoluteInput = helper.absolutePos(inputPos);
            DragonForgeInputBlockEntity.tick(helper.getLevel(), absoluteInput,
                helper.getLevel().getBlockState(absoluteInput), input);
            core.setItem(0, new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("tfc:metal/ingot/red_steel"))));
            core.setItem(1, new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("iceandfire:fire_dragon_blood"))));
            helper.assertTrue(core.assembled() && input.isAssembled(), "Forge is not assembled");
            helper.setBlock(inputPos.north().east(), block("tfc:rock/raw/granite"));
            for (int distance = 2; distance <= 4; distance++) {
                for (int height = 0; height <= 3; height++) {
                    helper.setBlock(inputPos.north(distance).above(height), Blocks.AIR);
                }
            }
            DragonBaseEntity dragon = dragon(helper, 0);
            BlockPos dragonPos = helper.absolutePos(inputPos.north(4));
            dragon.setPos(dragonPos.getX() + 0.5, dragonPos.getY(), dragonPos.getZ() + 0.5);
            Cow target = EntityType.COW.create(helper.getLevel());
            BlockPos targetPos = helper.absolutePos(inputPos.north(2));
            target.setPos(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5);
            target.setNoAi(true);
            helper.getLevel().addFreshEntity(target);
            helper.assertTrue(dragon.hasLineOfSight(target), "Dragon cannot see the target");
            float health = target.getHealth();
            for (int pulse = 0; pulse < 30; pulse++) breathe(helper, dragon, inputPos);
            helper.assertBlockPresent(block("iceandfire:chared_stone"), inputPos.north().east());
            helper.assertTrue(core.isBurning(), "Breath did not power the forge input");
            helper.assertTrue(target.getHealth() < health || !target.isAlive(), "Breath did not damage the target");
            dragon.discard();
            target.discard();
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void griefingKeepsIafProtection(GameTestHelper helper) {
        DragonBaseEntity dragon = dragon(helper, 0);
        BlockPos pos = TERRAIN_POS;
        String[] targets = {"tfc:metal/anvil/steel", "tfc:crucible", "tfc:bloomery",
            "tfc:ore/normal_native_copper/granite", "tfc:wood/chest/oak"};
        boolean originalGriefing = IafCommonConfig.INSTANCE.dragon.griefing.getValue();
        boolean originalMobGriefing = helper.getLevel().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
        try {
            IafCommonConfig.INSTANCE.dragon.griefing.setValue(true);
            helper.getLevel().getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(true,
                helper.getLevel().getServer());
            for (String id : targets) {
                if (id.equals("tfc:wood/chest/oak")) continue;
                Block target = block(id);
                helper.assertTrue(DragonUtils.canDragonBreak(target.defaultBlockState(), dragon),
                    "IaF unexpectedly protects " + id);
                helper.setBlock(pos, target);
                IafDragonDestructionManager.destroyAreaCharge(helper.getLevel(), helper.absolutePos(pos), dragon);
                helper.assertTrue(!helper.getBlockState(pos).is(target), "Charge could not destroy " + id);
            }
            helper.assertTrue(block("tfc:wood/chest/oak").defaultBlockState().is(IafBlockTags.DRAGON_BLOCK_BREAK_BLACKLIST),
                "TFC chest is missing IaF's chest blacklist");
            helper.setBlock(pos, block("tfc:wood/chest/oak"));
            IafDragonDestructionManager.destroyAreaCharge(helper.getLevel(), helper.absolutePos(pos), dragon);
            for (int pulse = 0; pulse < 8; pulse++) breathe(helper, dragon, pos);
            helper.assertBlockPresent(block("tfc:wood/chest/oak"), pos);

            IafCommonConfig.INSTANCE.dragon.griefing.setValue(false);
            for (String id : targets) {
                helper.setBlock(pos, block(id));
                IafDragonDestructionManager.destroyAreaCharge(helper.getLevel(), helper.absolutePos(pos), dragon);
                for (int pulse = 0; pulse < 8; pulse++) breathe(helper, dragon, pos);
                helper.assertBlockPresent(block(id), pos);
            }
            IafCommonConfig.INSTANCE.dragon.griefing.setValue(true);
            helper.getLevel().getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(false,
                helper.getLevel().getServer());
            for (String id : targets) {
                helper.setBlock(pos, block(id));
                IafDragonDestructionManager.destroyAreaCharge(helper.getLevel(), helper.absolutePos(pos), dragon);
                for (int pulse = 0; pulse < 8; pulse++) breathe(helper, dragon, pos);
                helper.assertBlockPresent(block(id), pos);
            }
        } finally {
            IafCommonConfig.INSTANCE.dragon.griefing.setValue(originalGriefing);
            helper.getLevel().getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(originalMobGriefing,
                helper.getLevel().getServer());
            dragon.discard();
        }
        helper.succeed();
    }

    private static void breathChanges(GameTestHelper helper, DragonBaseEntity dragon, String source, String result) {
        BlockPos pos = TERRAIN_POS;
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    helper.setBlock(pos.offset(x, y, z), Blocks.AIR);
                }
            }
        }
        boolean crop = source.startsWith("tfc:crop/");
        helper.setBlock(pos.below(), crop ? block("tfc:farmland/entisol") : Blocks.OBSIDIAN);
        helper.setBlock(pos, block(source));
        helper.assertBlockPresent(block(source), pos);
        for (int pulse = 0; pulse < 30 && !helper.getBlockState(pos).is(block(result)); pulse++) {
            breathe(helper, dragon, crop ? pos.above() : pos);
        }
        helper.assertTrue(helper.getBlockState(pos).is(block(result)),
            source + " expected " + result + " but got "
                + BuiltInRegistries.BLOCK.getKey(helper.getBlockState(pos).getBlock()));
    }

    private static void breathe(GameTestHelper helper, DragonBaseEntity dragon, BlockPos pos) {
        IafDragonDestructionManager.destroyAreaBreath(helper.getLevel(), helper.absolutePos(pos), dragon);
    }

    private static DragonBaseEntity dragon(GameTestHelper helper, int index) {
        DragonBaseEntity dragon = (DragonBaseEntity) BuiltInRegistries.ENTITY_TYPE.get(
            ResourceLocation.parse("iceandfire:" + ELEMENTS[index] + "_dragon")).create(helper.getLevel());
        dragon.setAgeInDays(50);
        dragon.setNoAi(true);
        dragon.setNoGravity(true);
        BlockPos pos = helper.absolutePos(new BlockPos(2, 300, 8));
        dragon.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        helper.getLevel().addFreshEntity(dragon);
        return dragon;
    }

    private static Block block(String id) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id));
    }
}
