package net.claustra01.iaftfc_test.gametest;

import com.iafenvoy.iceandfire.data.DragonArmorPart;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import java.util.List;
import net.claustra01.iaftfc.dragonarmor.DragonArmor;
import net.claustra01.iaftfc.metal.Dragonsteel;
import net.dries007.tfc.common.blockentities.TFCChestBlockEntity;
import net.dries007.tfc.common.component.size.ItemSizeManager;
import net.dries007.tfc.common.component.size.Size;
import net.dries007.tfc.common.component.size.Weight;
import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.util.Metal;
import net.dries007.tfc.util.PhysicalDamage;
import net.dries007.tfc.util.PhysicalDamageType;
import net.dries007.tfc.util.data.EntityDamageResistance;
import net.dries007.tfc.util.data.ItemDamageResistance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("iaftfc")
@PrefixGameTestTemplate(false)
public final class TfcCompatGameTests {
    private static final String EMPTY_TEMPLATE = "empty";
    private static final List<String> ELEMENTS = List.of("fire", "ice", "lightning");
    private static final List<String> PLAYER_ARMOR_PARTS = List.of(
        "helmet", "chestplate", "leggings", "boots");

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 200)
    public static void dragonPhysicalResistanceUsesTfcOnce(GameTestHelper helper) {
        for (String color : List.of(
            "amethyst", "black", "blue", "bronze", "copper", "electric",
            "gray", "green", "red", "sapphire", "silver", "white")) {
            for (String part : PLAYER_ARMOR_PARTS) {
                assertItemResistance(helper, "iceandfire:armor_" + color + "_" + part,
                    45, 33, 50);
            }
        }
        for (String element : ELEMENTS) {
            for (String part : PLAYER_ARMOR_PARTS) {
                assertItemResistance(helper, "iceandfire:dragonsteel_" + element + "_" + part,
                    70, 60, 70);
            }
            for (DragonArmorPart part : DragonArmorPart.values()) {
                assertItemResistance(helper,
                    "iceandfire:" + DragonArmor.id("dragon_steel_" + element, part),
                    70, 60, 70);
            }
        }
        assertDragonArmorResistance(helper, "steel", 30, 16.5F, 25);
        assertDragonArmorResistance(helper, "black_steel", 45, 33, 50);
        assertDragonArmorResistance(helper, "red_steel", 62.5F, 50, 50);
        assertDragonArmorResistance(helper, "blue_steel", 50, 50, 62.5F);

        assertDamageType(helper, "iceandfire:dragonbone_pickaxe", PhysicalDamageType.PIERCING);
        assertDamageType(helper, "iceandfire:dragonbone_bow", PhysicalDamageType.PIERCING);
        assertDamageType(helper, "iceandfire:dragonbone_axe", PhysicalDamageType.SLASHING);
        assertDamageType(helper, "iceandfire:dragonbone_sword_fire", PhysicalDamageType.SLASHING);
        assertDamageType(helper, "iceandfire:dragonbone_shovel", PhysicalDamageType.CRUSHING);
        for (String element : ELEMENTS) {
            assertDamageType(helper, "iceandfire:dragonsteel_" + element + "_pickaxe",
                PhysicalDamageType.PIERCING);
            assertDamageType(helper, "iceandfire:dragonsteel_" + element + "_sword",
                PhysicalDamageType.SLASHING);
            assertDamageType(helper, "iceandfire:dragonsteel_" + element + "_shovel",
                PhysicalDamageType.CRUSHING);
        }

        var attacker = helper.makeMockPlayer(GameType.SURVIVAL);
        attacker.setItemInHand(InteractionHand.MAIN_HAND, item("iceandfire:dragonbone_pickaxe"));
        var source = helper.getLevel().damageSources().playerAttack(attacker);
        helper.assertValueEqual(PhysicalDamageType.getTypeForSource(source), PhysicalDamageType.PIERCING,
            "A tagged Dragonbone tool must create a piercing TFC attack");
        for (String element : ELEMENTS) {
            DragonBaseEntity target = dragon(helper, element, new BlockPos(1, 2, 3));
            EntityDamageResistance resistance = EntityDamageResistance.get(target);
            helper.assertTrue(resistance != null, "Every elemental dragon must have entity resistance");
            assertDamage(helper, resistance.damages(), 30, 30, 30,
                "Elemental dragon resistance must be 30 for all physical damage types");
            attackAndCheck(helper, source, target, 0, "Unarmored " + element + " dragon");
        }
        DragonBaseEntity armoredDragon = dragon(helper, "fire", new BlockPos(3, 2, 3));
        armoredDragon.setItemSlot(EquipmentSlot.HEAD,
            item("iceandfire:dragonarmor_dragon_steel_fire_head"));
        attackAndCheck(helper, source, armoredDragon, 70, "Dragonsteel armored dragon");
        for (String metal : List.of("steel", "black_steel", "red_steel", "blue_steel")) {
            DragonBaseEntity target = dragon(helper, "fire", new BlockPos(5, 2, 3));
            target.setItemSlot(EquipmentSlot.HEAD, item("iaftfc:dragonarmor_" + metal + "_head"));
            float resistance = switch (metal) {
                case "steel" -> 30;
                case "black_steel" -> 45;
                case "red_steel" -> 62.5F;
                default -> 50;
            };
            attackAndCheck(helper, source, target, resistance, metal + " Dragon Armor");
        }
        attackAndCheck(helper, source, armoredZombie(helper, "iceandfire:armor_red_helmet"),
            45, "Dragon Scale Armor");
        attackAndCheck(helper, source, armoredZombie(helper, "iceandfire:dragonsteel_fire_helmet"),
            70, "Player Dragonsteel Armor");
        attackAndCheck(helper, source, armoredZombie(helper, "iceandfire:armor_silver_metal_helmet"),
            0, "Unrelated CE armor");

        helper.assertTrue(ItemDamageResistance.get(item("iceandfire:armor_silver_metal_helmet")) == null,
            "Non-dragon CE armor must not receive a new physical resistance");
        helper.assertTrue(ItemDamageResistance.get(item("iceandfire:dragonarmor_iron_head")) == null,
            "Unspecified CE Dragon Armor must keep its existing behavior");
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 200)
    public static void dragonItemsFollowTfcCarryLimits(GameTestHelper helper) {
        for (String color : List.of(
            "amethyst", "black", "blue", "bronze", "copper", "electric",
            "gray", "green", "red", "sapphire", "silver", "white")) {
            assertSize(helper, "iceandfire:dragonegg_" + color, Size.VERY_LARGE, Weight.VERY_HEAVY);
        }
        for (String element : ELEMENTS) {
            assertSize(helper, "iceandfire:dragon_skull_" + element, Size.HUGE, Weight.VERY_HEAVY);
            for (String part : PLAYER_ARMOR_PARTS) {
                assertSize(helper, "iceandfire:dragonsteel_" + element + "_" + part,
                    Size.LARGE, Weight.VERY_HEAVY);
            }
            for (String block : List.of("brick", "core", "core_disabled", "input")) {
                assertSize(helper, "iceandfire:dragonforge_" + element + "_" + block,
                    Size.SMALL, Weight.LIGHT);
            }
        }

        for (String metal : List.of(
            "copper", "gold", "silver", "diamond", "netherite", "iron",
            "dragon_steel_fire", "dragon_steel_ice", "dragon_steel_lightning")) {
            for (DragonArmorPart part : DragonArmorPart.values()) {
                assertSize(helper, "iceandfire:" + DragonArmor.id(metal, part),
                    Size.VERY_LARGE, Weight.VERY_HEAVY);
            }
        }
        for (DragonArmor.Metal metal : DragonArmor.METALS) {
            for (DragonArmorPart part : DragonArmorPart.values()) {
                assertSize(helper, "iaftfc:" + DragonArmor.id(metal.id(), part),
                    Size.VERY_LARGE, Weight.VERY_HEAVY);
            }
        }

        for (Dragonsteel metal : Dragonsteel.values()) {
            for (Metal.ItemType type : List.of(
                Metal.ItemType.INGOT, Metal.ItemType.DOUBLE_INGOT,
                Metal.ItemType.SHEET, Metal.ItemType.DOUBLE_SHEET)) {
                assertSize(helper, new ItemStack(Dragonsteel.item(metal, type).get()),
                    Size.LARGE, Weight.MEDIUM);
            }
            assertSize(helper, new ItemStack(Dragonsteel.item(metal, Metal.ItemType.ROD).get()),
                Size.NORMAL, Weight.LIGHT);
            for (Metal.ItemType type : List.of(
                Metal.ItemType.AXE_HEAD, Metal.ItemType.HOE_HEAD, Metal.ItemType.PICKAXE_HEAD,
                Metal.ItemType.SHOVEL_HEAD, Metal.ItemType.SWORD_BLADE,
                Metal.ItemType.UNFINISHED_HELMET, Metal.ItemType.UNFINISHED_CHESTPLATE,
                Metal.ItemType.UNFINISHED_GREAVES, Metal.ItemType.UNFINISHED_BOOTS)) {
                assertSize(helper, new ItemStack(Dragonsteel.item(metal, type).get()),
                    Size.VERY_SMALL, Weight.VERY_LIGHT);
            }
            assertSize(helper, "iaftfc:metal/bucket/" + metal.getSerializedName(),
                Size.LARGE, Weight.MEDIUM);
            assertSize(helper, new ItemStack(Dragonsteel.block(metal, Metal.BlockType.BLOCK).get()),
                Size.SMALL, Weight.LIGHT);
            assertSize(helper, new ItemStack(Dragonsteel.block(metal, Metal.BlockType.BLOCK_SLAB).get()),
                Size.SMALL, Weight.VERY_LIGHT);
            assertSize(helper, new ItemStack(Dragonsteel.block(metal, Metal.BlockType.BLOCK_STAIRS).get()),
                Size.SMALL, Weight.LIGHT);
            assertSize(helper, new ItemStack(Dragonsteel.block(metal, Metal.BlockType.ANVIL).get()),
                Size.HUGE, Weight.VERY_HEAVY);
        }

        BlockPos chestPos = new BlockPos(1, 2, 6);
        helper.setBlock(chestPos, block("tfc:wood/chest/chestnut"));
        TFCChestBlockEntity chest = (TFCChestBlockEntity)helper.getBlockEntity(chestPos);
        helper.assertTrue(!chest.canPlaceItem(0, item("iceandfire:dragonegg_red")),
            "Very large eggs must not fit in a TFC chest");
        helper.assertTrue(!chest.canPlaceItem(0, item("iceandfire:dragon_skull_fire")),
            "Huge skulls must not fit in a TFC chest");
        helper.assertTrue(!chest.canPlaceItem(0, item("iaftfc:dragonarmor_steel_head")),
            "Very large Dragon Armor must not fit in a TFC chest");
        helper.assertTrue(chest.canPlaceItem(0, item("iceandfire:dragonsteel_fire_helmet")),
            "Large player Dragonsteel Armor must fit in a TFC chest");
        helper.assertTrue(chest.canPlaceItem(0, item("iceandfire:dragonforge_fire_core")),
            "Small Dragon Forge blocks must fit in a TFC chest");
        helper.assertTrue(chest.canPlaceItem(0,
            new ItemStack(Dragonsteel.block(Dragonsteel.FIRE, Metal.BlockType.BLOCK_SLAB).get())),
            "Small plated slabs must fit in a TFC chest");

        var carrier = helper.makeMockPlayer(GameType.SURVIVAL);
        carrier.getInventory().add(item("iceandfire:dragon_skull_fire"));
        helper.assertValueEqual(Helpers.getCarryCount(carrier.getInventory()), Helpers.CarryCount.ONE,
            "One huge very-heavy dragon item must trigger TFC's exhausted carry tier");
        carrier.getInventory().add(new ItemStack(
            Dragonsteel.block(Dragonsteel.FIRE, Metal.BlockType.ANVIL).get()));
        helper.assertValueEqual(Helpers.getCarryCount(carrier.getInventory()), Helpers.CarryCount.MORE_THAN_ONE,
            "Two huge very-heavy dragon items must trigger TFC's overburdened carry tier");

        assertSize(helper, "iceandfire:amphithere_feather", Size.VERY_SMALL, Weight.VERY_LIGHT);
        helper.assertTrue(chest.canPlaceItem(0, item("iceandfire:amphithere_feather")),
            "Unrelated CE items must retain their normal storage behavior");
        helper.succeed();
    }

    private static void assertDragonArmorResistance(GameTestHelper helper, String metal,
        float piercing, float slashing, float crushing) {
        for (DragonArmorPart part : DragonArmorPart.values()) {
            assertItemResistance(helper, "iaftfc:" + DragonArmor.id(metal, part),
                piercing, slashing, crushing);
        }
    }

    private static void assertItemResistance(GameTestHelper helper, String id,
        float piercing, float slashing, float crushing) {
        ItemDamageResistance resistance = ItemDamageResistance.get(item(id));
        helper.assertTrue(resistance != null, id + " must have TFC item damage resistance");
        assertDamage(helper, resistance.damages(), piercing, slashing, crushing,
            id + " must use the specified physical resistance");
    }

    private static void assertDamage(GameTestHelper helper, PhysicalDamage actual,
        float piercing, float slashing, float crushing, String message) {
        helper.assertTrue(Math.abs(actual.piercing() - piercing) < 0.00001F
            && Math.abs(actual.slashing() - slashing) < 0.00001F
            && Math.abs(actual.crushing() - crushing) < 0.00001F, message);
    }

    private static void assertDamageType(GameTestHelper helper, String id, PhysicalDamageType expected) {
        helper.assertValueEqual(PhysicalDamageType.getTypeForItem(item(id)), expected,
            id + " must use its TFC physical attack type");
    }

    private static void assertSize(GameTestHelper helper, String id, Size size, Weight weight) {
        assertSize(helper, item(id), size, weight);
    }

    private static void assertSize(GameTestHelper helper, ItemStack stack, Size size, Weight weight) {
        var definition = ItemSizeManager.get(stack);
        helper.assertValueEqual(definition.getSize(stack), size,
            stack.getItem() + " must use the specified TFC size");
        helper.assertValueEqual(definition.getWeight(stack), weight,
            stack.getItem() + " must use the specified TFC weight");
    }

    private static void attackAndCheck(GameTestHelper helper, DamageSource source,
        LivingEntity target, float armorResistance, String name) {
        float damage = 12.0F;
        target.setHealth(target.getMaxHealth());
        float before = target.getHealth();
        EntityDamageResistance innate = EntityDamageResistance.get(target);
        float entityResistance = innate == null ? 0
            : innate.damages().value(PhysicalDamageType.getTypeForSource(source));
        float resisted = damage * (float)Math.exp(-0.01D * (entityResistance + armorResistance));
        float expected = CombatRules.getDamageAfterAbsorb(target, resisted, source,
            (float)target.getArmorValue(), (float)target.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
        helper.assertTrue(target.hurt(source, damage), name + " must take the attack");
        helper.assertTrue(Math.abs(before - target.getHealth() - expected) < 0.02F,
            name + " must apply TFC resistance and normal armor exactly once");
        target.discard();
    }

    private static Zombie armoredZombie(GameTestHelper helper, String armor) {
        Zombie zombie = (Zombie)BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("minecraft:zombie"))
            .create(helper.getLevel());
        zombie.moveTo(helper.absolutePos(new BlockPos(7, 2, 3)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(zombie);
        zombie.setItemSlot(EquipmentSlot.HEAD, item(armor));
        return zombie;
    }

    private static DragonBaseEntity dragon(GameTestHelper helper, String element, BlockPos position) {
        var type = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("iceandfire:" + element + "_dragon"));
        DragonBaseEntity dragon = (DragonBaseEntity)type.create(helper.getLevel());
        dragon.moveTo(helper.absolutePos(position), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(dragon);
        return dragon;
    }

    private static ItemStack item(String id) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
    }

    private static net.minecraft.world.level.block.Block block(String id) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id));
    }

    private TfcCompatGameTests() {
    }
}
