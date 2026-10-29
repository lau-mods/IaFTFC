package net.claustra01.iaftfc.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;
import java.util.stream.StreamSupport;
import net.dries007.tfc.common.blockentities.LargeVesselBlockEntity;
import net.dries007.tfc.common.component.food.FoodCapability;
import net.dries007.tfc.common.component.food.FoodData;
import net.dries007.tfc.common.component.food.Nutrient;
import net.dries007.tfc.common.component.mold.Vessel;
import net.dries007.tfc.common.component.size.ItemSizeManager;
import net.dries007.tfc.common.component.size.Size;
import net.dries007.tfc.common.component.size.Weight;
import net.dries007.tfc.common.player.IPlayerInfo;
import net.dries007.tfc.util.PhysicalDamageType;
import net.dries007.tfc.util.data.EntityDamageResistance;
import net.dries007.tfc.util.data.ItemDamageResistance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;

@GameTestHolder("iaftfc")
public final class FoodSizeAndDamageTests {
    static {
        GameTestReports.configure();
    }

    private record SizeCase(String name, Size size, Weight weight) {}
    private record ResistanceCase(String name, float piercing, float slashing, float crushing) {}

    private static final SizeCase[] SIZES = {
        new SizeCase("dragon_armor", Size.VERY_LARGE, Weight.VERY_HEAVY),
        new SizeCase("iaf_armor", Size.LARGE, Weight.VERY_HEAVY),
        new SizeCase("iaf_blocks", Size.SMALL, Weight.LIGHT),
        new SizeCase("iaf_bottles", Size.NORMAL, Weight.MEDIUM),
        new SizeCase("iaf_chains", Size.NORMAL, Weight.HEAVY),
        new SizeCase("iaf_chests", Size.LARGE, Weight.LIGHT),
        new SizeCase("iaf_cloth", Size.SMALL, Weight.LIGHT),
        new SizeCase("iaf_default", Size.VERY_SMALL, Weight.VERY_LIGHT),
        new SizeCase("iaf_dragon_armor", Size.VERY_LARGE, Weight.VERY_HEAVY),
        new SizeCase("iaf_dragon_bones", Size.VERY_LARGE, Weight.MEDIUM),
        new SizeCase("iaf_dragon_eggs", Size.VERY_LARGE, Weight.VERY_HEAVY),
        new SizeCase("iaf_dragon_skulls", Size.HUGE, Weight.VERY_HEAVY),
        new SizeCase("iaf_dragonsteel_armor", Size.LARGE, Weight.VERY_HEAVY),
        new SizeCase("iaf_empty_jars", Size.NORMAL, Weight.MEDIUM),
        new SizeCase("iaf_filled_jars", Size.NORMAL, Weight.HEAVY),
        new SizeCase("iaf_foods", Size.SMALL, Weight.LIGHT),
        new SizeCase("iaf_furniture", Size.LARGE, Weight.LIGHT),
        new SizeCase("iaf_ingots", Size.LARGE, Weight.MEDIUM),
        new SizeCase("iaf_large_vessels", Size.HUGE, Weight.HEAVY),
        new SizeCase("iaf_logs", Size.VERY_LARGE, Weight.MEDIUM),
        new SizeCase("iaf_ores", Size.SMALL, Weight.MEDIUM),
        new SizeCase("iaf_plants", Size.TINY, Weight.VERY_LIGHT),
        new SizeCase("iaf_rods", Size.NORMAL, Weight.LIGHT),
        new SizeCase("iaf_sheets", Size.LARGE, Weight.MEDIUM),
        new SizeCase("iaf_slabs", Size.SMALL, Weight.VERY_LIGHT),
        new SizeCase("iaf_small_light", Size.SMALL, Weight.LIGHT),
        new SizeCase("iaf_small_tools", Size.LARGE, Weight.MEDIUM),
        new SizeCase("iaf_stairs", Size.SMALL, Weight.LIGHT),
        new SizeCase("iaf_tools", Size.VERY_LARGE, Weight.VERY_HEAVY),
        new SizeCase("iaftfc_dragonbone_parts", Size.LARGE, Weight.MEDIUM),
        new SizeCase("iaftfc_dragonsteel_parts", Size.LARGE, Weight.MEDIUM),
        new SizeCase("iaftfc_metal_blocks", Size.SMALL, Weight.LIGHT),
        new SizeCase("iaftfc_metal_slabs", Size.SMALL, Weight.VERY_LIGHT),
        new SizeCase("iaftfc_metal_stairs", Size.SMALL, Weight.LIGHT)
    };
    private static final ResistanceCase[] RESISTANCES = {
        new ResistanceCase("iaf_copper_metal", 10.0f, 6.25f, 10.0f),
        new ResistanceCase("iaf_dragon_armor_copper", 10.0f, 6.25f, 10.0f),
        new ResistanceCase("iaf_dragon_armor_diamond", 30.0f, 16.5f, 25.0f),
        new ResistanceCase("iaf_dragon_armor_dragonsteel_fire", 70.0f, 60.0f, 70.0f),
        new ResistanceCase("iaf_dragon_armor_dragonsteel_ice", 70.0f, 60.0f, 70.0f),
        new ResistanceCase("iaf_dragon_armor_dragonsteel_lightning", 70.0f, 60.0f, 70.0f),
        new ResistanceCase("iaf_dragon_armor_gold", 12.5f, 8.25f, 12.5f),
        new ResistanceCase("iaf_dragon_armor_iron", 20.0f, 13.2f, 20.0f),
        new ResistanceCase("iaf_dragon_armor_netherite", 45.0f, 33.0f, 50.0f),
        new ResistanceCase("iaf_dragon_armor_silver", 12.5f, 8.25f, 12.5f),
        new ResistanceCase("iaf_dragon_scale", 45.0f, 33.0f, 50.0f),
        new ResistanceCase("iaf_dragonsteel", 70.0f, 60.0f, 70.0f),
        new ResistanceCase("iaf_hippogryph_diamond", 30.0f, 16.5f, 25.0f),
        new ResistanceCase("iaf_hippogryph_gold", 12.5f, 8.25f, 12.5f),
        new ResistanceCase("iaf_hippogryph_iron", 20.0f, 13.2f, 20.0f),
        new ResistanceCase("iaf_hippogryph_netherite", 45.0f, 33.0f, 50.0f),
        new ResistanceCase("iaf_iaftfc_dragon_armor_black_steel", 45.0f, 33.0f, 50.0f),
        new ResistanceCase("iaf_iaftfc_dragon_armor_blue_steel", 50.0f, 50.0f, 62.5f),
        new ResistanceCase("iaf_iaftfc_dragon_armor_red_steel", 62.5f, 50.0f, 50.0f),
        new ResistanceCase("iaf_iaftfc_dragon_armor_steel", 30.0f, 16.5f, 25.0f),
        new ResistanceCase("iaf_leather_like", 0.0f, 3.0f, 0.0f),
        new ResistanceCase("iaf_robust_hide", 20.0f, 13.2f, 20.0f),
        new ResistanceCase("iaf_silver_metal", 12.5f, 8.25f, 12.5f)
    };

    @GameTest(template = "empty")
    public static void foodNutritionAndDecay(GameTestHelper test) {
        String[] ids = {"ambrosia", "cannoli", "cooked_rice_with_fire_dragon_meat", "cooked_rice_with_ice_dragon_meat", "cooked_rice_with_lightning_dragon_meat", "fire_dragon_flesh", "ghost_cream", "ice_dragon_flesh", "lightning_dragon_flesh", "pixie_dust", "pixie_dust_milky_tea"};
        float[][] expected = {
            {5, 1.5f, 0, 0, .8f, 0, 0, .3f, 2.5f},
            {20, 5, 0, 1.5f, 0, 0, 0, 3, 2.5f},
            {4, 1.5f, 20, 1.7f, 0, 2, 3, 0, 3.5f},
            {4, 1.5f, 20, 1.7f, 0, 2, 3, 0, 3.5f},
            {4, 1.5f, 20, 1.7f, 0, 2, 3, 0, 3.5f},
            {8, 2, 0, 0, 0, 0, 3, 0, 2},
            {4, 1.5f, 10, 0, 0, 0, 0, 2, 2.5f},
            {8, 2, 0, 0, 0, 0, 3, 0, 2},
            {8, 2, 0, 0, 0, 0, 3, 0, 2},
            {1, .8f, 0, 0, 0, 0, 0, 0, .5f},
            {4, 1.5f, 20, 0, 0, 0, 0, 2, 2.5f}
        };
        for (int i = 0; i < ids.length; i++) {
            ItemStack fresh = FoodCapability.setCreatedNow(item("iceandfire:" + ids[i]));
            if (!FoodCapability.has(fresh) || FoodCapability.isRotten(fresh)) {
                test.fail("Fresh food missing " + ids[i]);
            }
            FoodData food = FoodCapability.get(fresh).getData();
            float[] actual = {food.hunger(), food.saturation(), food.water(), food.nutrient(Nutrient.GRAIN), food.nutrient(Nutrient.FRUIT), food.nutrient(Nutrient.VEGETABLES), food.nutrient(Nutrient.PROTEIN), food.nutrient(Nutrient.DAIRY), food.decayModifier()};
            for (int value = 0; value < actual.length; value++) {
                if (Math.abs(actual[value] - expected[i][value]) > .001f) {
                    test.fail("Food value " + ids[i] + " at " + value + ": " + actual[value]);
                }
            }
            if (!fresh.has(DataComponents.FOOD)) {
                test.fail("IaF food effect removed from " + ids[i]);
            }
            var player = test.makeMockPlayer(GameType.SURVIVAL);
            player.getFoodData().setFoodLevel(0);
            IPlayerInfo.get(player).eat(fresh);
            if (player.getFoodData().getFoodLevel() <= 0) {
                test.fail("TFC did not apply eaten food " + ids[i]);
            }
            ItemStack rotten = FoodCapability.setRotten(fresh.copy());
            ItemStack saved = ItemStack.parseOptional(test.getLevel().registryAccess(), (CompoundTag) rotten.save(test.getLevel().registryAccess()));
            if (!FoodCapability.isRotten(saved)) {
                test.fail("Rotten food became fresh " + ids[i]);
            }
        }
        test.succeed();
    }

    @GameTest(template = "empty")
    public static void foodInventoryPersistence(GameTestHelper test) {
        var player = test.makeMockPlayer(GameType.SURVIVAL);
        ItemStack fresh = FoodCapability.setCreatedNow(item("iceandfire:fire_dragon_flesh"));
        ItemStack rotten = FoodCapability.setRotten(item("iceandfire:ice_dragon_flesh"));
        player.getInventory().setItem(0, fresh);
        player.getInventory().setItem(1, rotten);
        ListTag saved = player.getInventory().save(new ListTag());
        player.getInventory().clearContent();
        player.getInventory().load(saved);
        if (FoodCapability.isRotten(player.getInventory().getItem(0)) || !FoodCapability.isRotten(player.getInventory().getItem(1))) {
            test.fail("Food freshness changed after inventory save and load");
        }
        test.succeed();
    }

    @GameTest(template = "empty")
    public static void allItemSizes(GameTestHelper test) {
        for (SizeCase definition : SIZES) {
            int present = 0;
            for (String id : sizeIds(definition.name)) {
                ItemStack stack = item(id);
                if (stack.isEmpty()) {
                    continue;
                }
                present++;
                if (ItemSizeManager.get(stack).getSize(stack) != definition.size || ItemSizeManager.get(stack).getWeight(stack) != definition.weight) {
                    test.fail("Wrong size or weight " + id);
                }
            }
            if (present == 0) {
                test.fail("No registered member for " + definition.name);
            }
        }
        ItemStack small = item("iceandfire:pixie_dust");
        ItemStack egg = item("iceandfire:dragonegg_red");
        ItemStack skull = item("iceandfire:dragon_skull_fire");
        ItemStack armor = item("iaftfc:dragonarmor_steel_head");
        Vessel vessel = Vessel.get(item("tfc:ceramic/vessel"));
        if (!vessel.insertItem(0, small, false).isEmpty()) {
            test.fail("Small material rejected by TFC vessel");
        }
        for (ItemStack large : new ItemStack[] {egg, skull, armor}) {
            if (vessel.insertItem(1, large, false).isEmpty()) {
                test.fail("Oversized item entered TFC vessel " + BuiltInRegistries.ITEM.getKey(large.getItem()));
            }
        }
        BlockPos position = new BlockPos(0, 1, 0);
        test.setBlock(position, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("tfc:ceramic/large_vessel")));
        if (!(test.getLevel().getBlockEntity(test.absolutePos(position)) instanceof LargeVesselBlockEntity largeVessel)) {
            test.fail("TFC large vessel unavailable");
            return;
        }
        if (!largeVessel.getInventory().insertItem(0, small, false).isEmpty()) {
            test.fail("Small material rejected by large TFC vessel");
        }
        for (ItemStack oversized : new ItemStack[] {egg, skull, armor}) {
            if (largeVessel.getInventory().insertItem(1, oversized, false).isEmpty()) {
                test.fail("Oversized item entered large TFC vessel");
            }
        }
        test.succeed();
    }

    @GameTest(template = "empty")
    public static void optionalIdsDoNotCreateItems(GameTestHelper test) {
        for (SizeCase definition : SIZES) {
            if (!definition.name.startsWith("iaf_")) {
                continue;
            }
            TagKey<Item> tag = TagKey.create(Registries.ITEM, ResourceLocation.parse("iaftfc:item_size/" + definition.name));
            int present = 0;
            for (String id : sizeIds(definition.name)) {
                ResourceLocation key = ResourceLocation.parse(id);
                var registered = BuiltInRegistries.ITEM.getOptional(key);
                if (registered.isEmpty()) {
                    if (BuiltInRegistries.ITEM.containsKey(key)) {
                        test.fail("Optional ID unexpectedly registered " + id);
                    }
                } else {
                    present++;
                    ItemStack stack = new ItemStack(registered.get());
                    if (!stack.is(tag) || ItemSizeManager.getDefinition(stack) == null) {
                        test.fail("Registered item lost size tag " + id);
                    }
                }
            }
            if (present == 0) {
                test.fail("Empty loaded size tag " + definition.name);
            }
        }
        test.succeed();
    }

    @GameTest(template = "empty")
    public static void armorResistances(GameTestHelper test) {
        var target = EntityType.COW.create(test.getLevel());
        Zombie attacker = EntityType.ZOMBIE.create(test.getLevel());
        if (target == null || attacker == null) {
            test.fail("Attack entities unavailable");
            return;
        }
        target.setPos(test.absolutePos(new BlockPos(0, 1, 0)).getCenter());
        attacker.setPos(test.absolutePos(new BlockPos(0, 1, 1)).getCenter());
        test.getLevel().addFreshEntity(target);
        test.getLevel().addFreshEntity(attacker);
        String[] weapons = {"iceandfire:copper_pickaxe", "iceandfire:copper_sword", "iceandfire:copper_shovel"};
        for (ResistanceCase definition : RESISTANCES) {
            ItemStack representative = ItemStack.EMPTY;
            for (String id : tagIds("item_damage_resistance", definition.name)) {
                ItemStack stack = item(id);
                if (stack.isEmpty()) {
                    continue;
                }
                if (representative.isEmpty()) {
                    representative = stack;
                }
                ItemDamageResistance resistance = ItemDamageResistance.get(stack);
                if (resistance == null || resistance.damages().piercing() != definition.piercing || resistance.damages().slashing() != definition.slashing || resistance.damages().crushing() != definition.crushing) {
                    test.fail("Wrong resistance " + id);
                }
            }
            if (representative.isEmpty()) {
                test.fail("No registered armor for " + definition.name);
                continue;
            }
            target.setItemSlot(EquipmentSlot.HEAD, representative);
            float[] expected = {definition.piercing, definition.slashing, definition.crushing};
            for (int i = 0; i < weapons.length; i++) {
                attacker.setItemSlot(EquipmentSlot.MAINHAND, item(weapons[i]));
                var source = test.getLevel().damageSources().mobAttack(attacker);
                float multiplier = PhysicalDamageType.calculateMultiplier(source, target);
                if (Math.abs(multiplier - Math.exp(-.01f * expected[i])) > .001f) {
                    test.fail("Wrong physical multiplier for " + definition.name + " and " + weapons[i]);
                }
                target.setHealth(target.getMaxHealth());
                target.invulnerableTime = 0;
                if (!target.hurt(source, 2f) || target.getHealth() >= target.getMaxHealth()) {
                    test.fail("Physical attack did not damage " + definition.name);
                }
            }
        }
        test.succeed();
    }

    @GameTest(template = "empty")
    public static void dragonAndWeaponDamageTypes(GameTestHelper test) {
        for (String kind : new String[] {"fire", "ice", "lightning"}) {
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("iceandfire:" + kind + "_dragon"));
            Entity dragon = type.create(test.getLevel());
            if (dragon == null) {
                test.fail("Dragon missing " + kind);
                return;
            }
            EntityDamageResistance resistance = EntityDamageResistance.get(dragon);
            if (resistance == null || resistance.damages().piercing() != 30 || resistance.damages().slashing() != 30 || resistance.damages().crushing() != 30) {
                test.fail("Dragon resistance " + kind);
            }
            if (!type.is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("tfc:deals_slashing_damage")))) {
                test.fail("Dragon slashing tag " + kind);
            }
            if (dragon instanceof LivingEntity living) {
                Zombie attacker = EntityType.ZOMBIE.create(test.getLevel());
                if (attacker == null) {
                    test.fail("Attacker unavailable");
                    return;
                }
                attacker.setItemSlot(EquipmentSlot.MAINHAND, item("iceandfire:copper_sword"));
                var source = test.getLevel().damageSources().mobAttack(attacker);
                if (Math.abs(PhysicalDamageType.calculateMultiplier(source, living) - Math.exp(-.3f)) > .001f) {
                    test.fail("Dragon physical multiplier " + kind);
                }
                living.setPos(test.absolutePos(new BlockPos(0, 1, 0)).getCenter());
                test.getLevel().addFreshEntity(living);
                float before = living.getHealth();
                living.hurt(source, 10f);
                if (living.getHealth() >= before) {
                    test.fail("Physical attack did not damage dragon " + kind);
                }
            }
        }
        var cow = EntityType.COW.create(test.getLevel());
        if (cow == null) {
            test.fail("Cow unavailable");
            return;
        }
        if (EntityDamageResistance.get(cow) != null) {
            test.fail("Other entity gained dragon resistance");
        }
        Zombie cowAttacker = EntityType.ZOMBIE.create(test.getLevel());
        if (cowAttacker == null) {
            test.fail("Cow attacker unavailable");
            return;
        }
        cowAttacker.setItemSlot(EquipmentSlot.MAINHAND, item("iceandfire:copper_sword"));
        var cowSource = test.getLevel().damageSources().mobAttack(cowAttacker);
        if (Math.abs(PhysicalDamageType.calculateMultiplier(cowSource, cow) - 1f) > .001f) {
            test.fail("Cow gained dragon physical resistance");
        }
        cow.setPos(test.absolutePos(new BlockPos(0, 1, 0)).getCenter());
        test.getLevel().addFreshEntity(cow);
        float cowHealth = cow.getHealth();
        cow.hurt(cowSource, 2f);
        if (cow.getHealth() >= cowHealth) {
            test.fail("Cow physical attack failed");
        }
        for (String type : new String[] {"piercing", "slashing", "crushing"}) {
            TagKey<Item> tag = TagKey.create(Registries.ITEM, ResourceLocation.parse("tfc:deals_" + type + "_damage"));
            int present = 0;
            for (String id : StreamSupport.stream(resource("data/tfc/tags/item/deals_" + type + "_damage.json")
                .getAsJsonArray("values").spliterator(), false)
                .map(value -> value.getAsJsonObject().get("id").getAsString()).toList()) {
                ItemStack weapon = item(id);
                if (!weapon.isEmpty()) {
                    present++;
                    if (!weapon.is(tag) || PhysicalDamageType.getTypeForItem(weapon) != PhysicalDamageType.valueOf(type.toUpperCase(Locale.ROOT))) {
                        test.fail("Weapon damage type " + id);
                    }
                }
            }
            if (present == 0) {
                test.fail("No registered IaF weapon for " + type);
            }
        }
        test.succeed();
    }

    private static String[] sizeIds(String name) {
        if (name.startsWith("iaf_")) {
            return tagIds("item_size", name);
        }
        return StreamSupport.stream(resource("data/iaftfc/tfc/item_size/" + name + ".json").getAsJsonArray("ingredient").spliterator(), false)
            .map(value -> value.getAsJsonObject().get("item").getAsString()).toArray(String[]::new);
    }

    private static String[] tagIds(String kind, String name) {
        return StreamSupport.stream(resource("data/iaftfc/tags/item/" + kind + "/" + name + ".json").getAsJsonArray("values").spliterator(), false)
            .map(value -> value.isJsonObject() ? value.getAsJsonObject().get("id").getAsString() : value.getAsString()).toArray(String[]::new);
    }

    private static JsonObject resource(String name) {
        try (var input = Files.newInputStream(ModList.get().getModFileById("iaftfc").getFile().findResource(name.split("/")))) {
            return JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException exception) {
            throw new AssertionError("Missing resource " + name, exception);
        }
    }

    private static ItemStack item(String id) {
        Item found = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(id)).orElse(null);
        return found == null ? ItemStack.EMPTY : new ItemStack(found);
    }
}
