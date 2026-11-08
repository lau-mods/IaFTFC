package net.claustra01.iaftfc_test.gametest;

import com.google.gson.JsonParser;
import com.iafenvoy.iceandfire.config.IafCommonConfig;
import com.iafenvoy.iceandfire.entity.GhostEntity;
import com.iafenvoy.iceandfire.event.handler.ServerEvents;
import com.iafenvoy.iceandfire.registry.IafEntities;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.neoforged.neoforge.common.world.NoneBiomeModifier;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

@GameTestHolder("iaftfc")
@PrefixGameTestTemplate(false)
public final class NonDragonWorldgenGameTests {
    private static final List<String> STRUCTURES = List.of(
        "cyclops_cave", "gorgon_temple", "graveyard", "hydra_cave", "mausoleum", "pixie_village", "siren_island");
    private static final Set<String> MOB_FEATURES = Set.of(
        "spawn_death_worm", "spawn_hippocampus", "spawn_sea_serpent", "spawn_stymphalian_bird",
        "spawn_wandering_cyclops");
    private static final Set<String> DRAGONS = Set.of("fire_dragon", "ice_dragon", "lightning_dragon");

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void nonDragonNaturalGenerationIsDisabled(GameTestHelper helper) throws IOException {
        var level = helper.getLevel();
        var modifiers = level.registryAccess().registryOrThrow(NeoForgeRegistries.Keys.BIOME_MODIFIERS);
        Set<String> overridden = new HashSet<>();
        Set<String> disabledModifiers = new HashSet<>(MOB_FEATURES);
        disabledModifiers.addAll(List.of("amphithere", "cockatrice", "hippogryphs", "lich", "siren_beach_night", "troll"));
        for (String id : disabledModifiers) {
            helper.assertTrue(modifiers.get(ce(id)) instanceof NoneBiomeModifier, "Active mob modifier: " + id);
            overridden.add("iceandfire:neoforge/biome_modifier/" + id + ".json");
        }

        var sets = level.registryAccess().registryOrThrow(Registries.STRUCTURE_SET);
        var state = level.getChunkSource().getGeneratorState();
        for (String id : STRUCTURES) {
            var placement = (RandomSpreadStructurePlacement) sets.get(ce(id)).placement();
            // Test actual random-spread candidates: arbitrary chunks could fail even at frequency=1.
            for (int x = -4; x <= 4; x++) {
                for (int z = -4; z <= 4; z++) {
                    var candidate = placement.getPotentialStructureChunk(state.getLevelSeed(),
                        x * placement.spacing(), z * placement.spacing());
                    helper.assertTrue(!placement.isStructureChunk(state, candidate.x, candidate.z),
                        "Non-dragon structure accepted a placement candidate: " + id);
                }
            }
            overridden.add("iceandfire:worldgen/structure_set/" + id + ".json");
        }
        for (String id : List.of("iceandfire:dragon_roost", "iceandfire:dragon_cave",
            "iaftfc:ice_dragon_roost", "iaftfc:ice_dragon_cave",
            "iaftfc:lightning_dragon_roost", "iaftfc:lightning_dragon_cave")) {
            var placement = (RandomSpreadStructurePlacement) sets.get(ResourceLocation.parse(id)).placement();
            boolean available = false;
            for (int x = 100; x < 120; x++) {
                var candidate = placement.getPotentialStructureChunk(state.getLevelSeed(), x * placement.spacing(), 0);
                available |= placement.isStructureChunk(state, candidate.x, candidate.z);
            }
            helper.assertTrue(available, "Dragon placement was disabled: " + id);
        }

        boolean otherMobSpawns = false;
        Set<String> preservedSkeletons = new HashSet<>();
        for (var biome : level.registryAccess().registryOrThrow(Registries.BIOME)) {
            for (MobCategory category : MobCategory.values()) {
                for (var spawn : biome.getMobSettings().getMobs(category).unwrap()) {
                    var id = BuiltInRegistries.ENTITY_TYPE.getKey(spawn.type);
                    helper.assertTrue(!id.getNamespace().equals("iceandfire") || DRAGONS.contains(id.getPath()),
                        "Non-dragon CE mob remains in a biome spawn table: " + id);
                    otherMobSpawns |= !id.getNamespace().equals("iceandfire");
                }
            }
            for (var step : biome.getGenerationSettings().features()) {
                for (var feature : step) {
                    feature.unwrapKey().ifPresent(key -> {
                        var id = key.location();
                        if (id.getNamespace().equals("iceandfire")) {
                            helper.assertTrue(!MOB_FEATURES.contains(id.getPath()), "Mob spawn feature remains: " + id);
                            if (id.getPath().startsWith("spawn_dragon_skeleton_")) preservedSkeletons.add(id.getPath());
                        }
                    });
                }
            }
        }
        helper.assertTrue(otherMobSpawns, "Other mods' and vanilla spawn tables must remain available");
        helper.assertValueEqual(preservedSkeletons, Set.of("spawn_dragon_skeleton_fire", "spawn_dragon_skeleton_ice",
            "spawn_dragon_skeleton_lightning"), "All dragon skeleton features must remain available");

        try (var stream = NonDragonWorldgenGameTests.class.getResourceAsStream("/iaftfc/gametest/override_manifest.json")) {
            var manifest = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            Set<String> allowed = new HashSet<>();
            manifest.getAsJsonArray("allowed_overrides").forEach(value -> allowed.add(value.getAsString()));
            helper.assertTrue(allowed.containsAll(overridden), "Missing natural generation overrides in the manifest");
        }
        for (String id : overridden) {
            var resource = level.getServer().getResourceManager().getResourceOrThrow(ResourceLocation.parse(id));
            helper.assertTrue(resource.sourcePackId().contains("iaftfc"), "Override is not supplied by IaFTFC: " + id);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void playerDeathsDoNotCreateGhosts(GameTestHelper helper) {
        var level = helper.getLevel();
        var victim = helper.makeMockPlayer(GameType.SURVIVAL);
        var attacker = helper.makeMockPlayer(GameType.SURVIVAL);
        var pos = helper.absolutePos(new net.minecraft.core.BlockPos(1, 2, 1));
        victim.setPos(pos.getX(), pos.getY(), pos.getZ());
        victim.setLastHurtByMob(attacker);
        victim.addEffect(new MobEffectInstance(MobEffects.POISON, 200));
        victim.getRandom().setSeed(0L);
        helper.assertValueEqual(victim.getRandom().nextInt(3), 0, "The CE ghost roll must pass in this fixture");
        victim.getRandom().setSeed(0L);
        boolean originalSetting = IafCommonConfig.INSTANCE.ghost.fromPlayerDeaths.getValue();
        try {
            IafCommonConfig.INSTANCE.ghost.fromPlayerDeaths.setValue(true);
            ServerEvents.onEntityDie(new LivingDeathEvent(victim, level.damageSources().playerAttack(attacker)));
            helper.assertTrue(level.getEntitiesOfClass(GhostEntity.class, victim.getBoundingBox().inflate(2)).isEmpty(),
                "Player death must not create a Ghost even when CE enables it");
            var manual = IafEntities.GHOST.get().create(level);
            manual.copyPosition(victim);
            helper.assertTrue(level.addFreshEntity(manual), "Explicit entity creation must remain possible");
            manual.discard();
            helper.assertTrue(IafCommonConfig.INSTANCE.ghost.fromPlayerDeaths.getValue(), "Do not rewrite CE's config");
        } finally {
            IafCommonConfig.INSTANCE.ghost.fromPlayerDeaths.setValue(originalSetting);
        }
        helper.succeed();
    }

    private static ResourceLocation ce(String path) {
        return ResourceLocation.fromNamespaceAndPath("iceandfire", path);
    }
}
