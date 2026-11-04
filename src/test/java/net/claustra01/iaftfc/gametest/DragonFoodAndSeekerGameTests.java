package net.claustra01.iaftfc.gametest;

import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.item.DragonSeekerItem;
import com.iafenvoy.iceandfire.item.DragonFluteItem;
import com.iafenvoy.iceandfire.item.SummoningCrystalItem;
import com.iafenvoy.iceandfire.registry.IafEntities;
import com.mojang.authlib.GameProfile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.dries007.tfc.common.component.food.FoodCapability;
import net.dries007.tfc.common.component.food.Nutrient;
import net.dries007.tfc.common.component.size.ItemSizeManager;
import net.dries007.tfc.common.component.size.Size;
import net.dries007.tfc.common.component.size.Weight;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("iaftfc")
@PrefixGameTestTemplate(false)
public final class DragonFoodAndSeekerGameTests {
    @GameTest(template = "empty")
    public static void freshMeatDragonMeal(GameTestHelper helper) {
        Recipe<CraftingInput> recipe = crafting(helper, "iceandfire:dragon_meal");
        for (String meat : List.of("beef", "venison", "duck", "camelidae")) {
            CraftingInput input = meal(item("tfc:food/" + meat));
            helper.assertTrue(recipe.matches(input, helper.getLevel()), "Fresh " + meat + " did not make Dragon Meal");
            ItemStack result = recipe.assemble(input, helper.getLevel().registryAccess());
            helper.assertTrue(result.is(item("iceandfire:dragon_meal").getItem()) && result.getCount() == 1,
                "Four meat and five bones did not yield one Dragon Meal");
        }
        ItemStack rotten = FoodCapability.setRotten(item("tfc:food/beef"));
        helper.assertTrue(FoodCapability.isRotten(rotten) && !recipe.matches(meal(rotten), helper.getLevel()),
            "Rotten meat made Dragon Meal");
        for (String invalid : List.of("tfc:food/cooked_beef", "tfc:food/cod", "tfc:food/shellfish",
            "iceandfire:fire_dragon_flesh", "minecraft:beef")) {
            helper.assertTrue(!recipe.matches(meal(item(invalid)), helper.getLevel()),
                "Invalid meat made Dragon Meal: " + invalid);
        }
        helper.assertTrue(!recipe.matches(meal(item("tfc:food/beef"), item("minecraft:iron_ingot")),
            helper.getLevel()), "Non-bone filler made Dragon Meal");
        DragonBaseEntity dragon = IafEntities.FIRE_DRAGON.get().create(helper.getLevel());
        dragon.setNoAi(true);
        dragon.setAgeInDays(1);
        dragon.moveTo(helper.absolutePos(new net.minecraft.core.BlockPos(2, 2, 2)), 0, 0);
        var player = player(helper, "iaftfc_meal");
        dragon.setTame(true, false);
        dragon.setOwnerUUID(player.getUUID());
        helper.getLevel().addFreshEntity(dragon);
        helper.assertTrue(dragon.isOwnedBy(player) && !dragon.isModelDead(),
            "Meal dragon is not alive and owned: tame=" + dragon.isTame() + ", owner=" + dragon.getOwnerUUID());
        player.setItemInHand(InteractionHand.MAIN_HAND, item("iceandfire:dragon_meal"));
        var interaction = dragon.mobInteract(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(dragon.getAgeInDays() == 2, "Dragon Meal no longer grows a dragon by one day: age="
            + dragon.getAgeInDays() + ", interaction=" + interaction + ", held=" + player.getMainHandItem());
        dragon.discard();
        helper.getLevel().removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void dragonFoodDataAndDecay(GameTestHelper helper) {
        for (String element : List.of("fire", "ice", "lightning")) {
            checkFood(helper, "iceandfire:" + element + "_dragon_flesh", 0, 2, 0, 0, 2);
            String stew = "iceandfire:" + (element.equals("ice") ? "frost" : element) + "_stew";
            checkFood(helper, stew, 2, 2.5f, 5, 1, 1.5f);
        }
        for (String recipe : List.of("fire_stew", "frost_stew", "lightning_lily_mixture")) {
            helper.assertTrue(helper.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath(
                "iceandfire", recipe)).isPresent(), "Original stew recipe was lost: " + recipe);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void dragonFleshEffectsAndStewRecipes(GameTestHelper helper) {
        var player = player(helper, "iaftfc_stew");
        player.getAbilities().instabuild = false;
        player.moveTo(Vec3.atCenterOf(helper.absolutePos(new net.minecraft.core.BlockPos(2, 2, 2))));
        item("iceandfire:fire_dragon_flesh").getItem().finishUsingItem(
            item("iceandfire:fire_dragon_flesh"), helper.getLevel(), player);
        helper.assertTrue(player.getRemainingFireTicks() > 0, "Fire flesh lost its ignition effect");
        player.clearFire();
        item("iceandfire:ice_dragon_flesh").getItem().finishUsingItem(
            item("iceandfire:ice_dragon_flesh"), helper.getLevel(), player);
        helper.assertTrue(player.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "Ice flesh lost its slowness effect");
        AABB area = AABB.ofSize(player.position(), 4, 4, 4);
        int before = helper.getLevel().getEntitiesOfClass(LightningBolt.class, area).size();
        item("iceandfire:lightning_dragon_flesh").getItem().finishUsingItem(
            item("iceandfire:lightning_dragon_flesh"), helper.getLevel(), player);
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(LightningBolt.class, area).size() == before + 1,
            "Lightning flesh lost its lightning effect");

        String[][] stews = {
            {"fire_stew", "fire_stew", "minecraft:blaze_rod", "iceandfire:fire_lily"},
            {"frost_stew", "frost_stew", "minecraft:prismarine_crystals", "iceandfire:frost_lily"},
            {"lightning_lily_mixture", "lightning_stew", "minecraft:chorus_fruit", "iceandfire:lightning_lily"}
        };
        for (String[] stew : stews) {
            CraftingInput input = CraftingInput.of(3, 1, List.of(new ItemStack(Items.BOWL), item(stew[2]), item(stew[3])));
            Recipe<CraftingInput> recipe = crafting(helper, "iceandfire:" + stew[0]);
            helper.assertTrue(recipe.matches(input, helper.getLevel())
                && recipe.assemble(input, helper.getLevel().registryAccess())
                    .is(item("iceandfire:" + stew[1]).getItem()),
                "Original stew ingredients or output changed: " + stew[0]);
            player.setItemInHand(InteractionHand.MAIN_HAND, item("iceandfire:" + stew[1]));
            ItemStack bowl = player.getMainHandItem().getItem().finishUsingItem(
                player.getMainHandItem(), helper.getLevel(), player);
            helper.assertTrue(bowl.is(Items.BOWL) || player.getInventory().contains(new ItemStack(Items.BOWL)),
                "Stew did not return its bowl: " + stew[1]);
            DragonBaseEntity dragon = (DragonBaseEntity) BuiltInRegistries.ENTITY_TYPE.get(
                ResourceLocation.parse("iceandfire:" + (stew[1].equals("frost_stew") ? "ice" : stew[1].split("_")[0])
                    + "_dragon")).create(helper.getLevel());
            dragon.setAgeInDays(100);
            helper.assertTrue(dragon.isFood(item("iceandfire:" + stew[1])),
                "Stew no longer acts as dragon breeding food: " + stew[1]);
            player.setItemInHand(InteractionHand.MAIN_HAND, item("iceandfire:" + stew[1]));
            dragon.mobInteract(player, InteractionHand.MAIN_HAND);
            helper.assertTrue(dragon.isInLove(), "Stew no longer starts dragon breeding: " + stew[1]);
        }
        helper.getLevel().removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void seekerSmithingAndCrafting(GameTestHelper helper) {
        Recipe<CraftingInput> normal = crafting(helper, "iceandfire:dragonseeker");
        CraftingInput normalInput = seeker(item("tfc:metal/ingot/steel"));
        helper.assertTrue(normal.matches(normalInput, helper.getLevel())
            && normal.assemble(normalInput, helper.getLevel().registryAccess())
                .is(item("iceandfire:dragon_seeker").getItem()), "Skull and two Steel ingots did not make Normal Seeker");
        helper.assertTrue(!normal.matches(seeker(item("minecraft:netherite_ingot")), helper.getLevel()),
            "Netherite still makes Normal Seeker");
        helper.assertTrue(helper.getLevel().getRecipeManager().byKey(ResourceLocation.parse(
            "iceandfire:epic_dragon_seeker")).isEmpty(), "Original Epic recipe still exists");
        var table = new net.minecraft.core.BlockPos(1, 1, 1);
        helper.setBlock(table, Blocks.SMITHING_TABLE);
        for (String color : List.of("red", "blue")) {
            var holder = helper.getLevel().getRecipeManager().byKey(ResourceLocation.parse(
                "iaftfc:epic_dragon_seeker_" + color)).orElseThrow();
            helper.assertTrue(holder.value() instanceof SmithingTransformRecipe, "Epic recipe is not smithing");
            SmithingTransformRecipe recipe = (SmithingTransformRecipe) holder.value();
            String other = color.equals("red") ? "blue" : "red";
            SmithingRecipeInput valid = smithing(color, color);
            helper.assertTrue(recipe.matches(valid, helper.getLevel())
                && recipe.assemble(valid, helper.getLevel().registryAccess())
                    .is(item("iceandfire:epic_dragon_seeker").getItem()),
                color + " Steel smithing did not make Epic Seeker");
            helper.assertTrue(!recipe.matches(smithing(color, other), helper.getLevel())
                && !recipe.matches(smithing(other, color), helper.getLevel())
                && !recipe.matches(new SmithingRecipeInput(item("minecraft:netherite_upgrade_smithing_template"),
                    item("iceandfire:dragon_seeker"), item("minecraft:nether_star")), helper.getLevel()),
                "Mixed steel or old Netherite path matched Epic Seeker");
            helper.assertTrue(smithingResult(helper, table, smithing(color, color))
                .is(item("iceandfire:epic_dragon_seeker").getItem()),
                "Smithing menu did not produce Epic Seeker from " + color + " Steel");
            helper.assertTrue(smithingResult(helper, table, smithing(color, other)).isEmpty(),
                "Smithing menu accepted mixed Steel");
        }
        helper.assertTrue(smithingResult(helper, table, new SmithingRecipeInput(
            item("minecraft:netherite_upgrade_smithing_template"), item("iceandfire:dragon_seeker"),
            item("minecraft:nether_star"))).isEmpty(), "Smithing menu accepted the old Netherite recipe");
        Recipe<CraftingInput> legendary = crafting(helper, "iceandfire:legendary_dragon_seeker");
        for (String element : List.of("fire", "ice", "lightning")) {
            helper.assertTrue(legendary.matches(seeker(item("iaftfc:metal/ingot/dragonsteel_" + element)),
                helper.getLevel()), "IaFTFC Dragonsteel did not make Legendary Seeker");
            helper.assertTrue(!legendary.matches(seeker(item("iceandfire:dragonsteel_" + element + "_ingot")),
                helper.getLevel()), "Legacy IaF ingot made Legendary Seeker");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void seekerRangesAndControls(GameTestHelper helper) {
        String[] names = {"dragon_seeker", "epic_dragon_seeker", "legendary_dragon_seeker", "godly_dragon_seeker"};
        int[] ranges = {150, 200, 300, 500};
        for (int index = 0; index < ranges.length; index++) {
            Vec3 extent = DragonSeekerItem.SeekerType.values()[index].add(Vec3.ZERO, false);
            helper.assertTrue(extent.equals(new Vec3(ranges[index], ranges[index], ranges[index])),
                "Seeker range changed: " + names[index]);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "seeker", timeoutTicks = 200)
    public static void seekerFindsDragonAtEachRange(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = FakePlayerFactory.getMinecraft(level);
        Vec3 origin = Vec3.atCenterOf(helper.absolutePos(new net.minecraft.core.BlockPos(2, 360, 2)));
        DragonBaseEntity dragon = IafEntities.FIRE_DRAGON.get().create(level);
        dragon.setNoAi(true);
        dragon.setAgeInDays(1);
        dragon.setHealth(dragon.getMaxHealth());
        dragon.moveTo(origin);
        helper.assertTrue(level.addFreshEntity(dragon), "Dragon could not be added to the test world");
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(level.getEntitiesOfClass(DragonBaseEntity.class, AABB.ofSize(origin, 4, 4, 4)).contains(dragon),
                "Seeker target is not indexed in its loaded chunk");
            String[] names = {"dragon_seeker", "epic_dragon_seeker", "legendary_dragon_seeker", "godly_dragon_seeker"};
            int[] distances = {130, 175, 250, 490};
            for (int step = 0; step < distances.length; step++) {
                player.moveTo(origin.add(distances[step], 0, 0));
                helper.assertTrue(TargetingConditions.forCombat().test(player, dragon),
                    "Seeker target is not valid for IaF combat targeting at " + distances[step]);
                for (int tier = 0; tier < names.length; tier++) {
                    ItemStack seeker = item("iceandfire:" + names[tier]);
                    player.setItemInHand(InteractionHand.MAIN_HAND, seeker);
                    InteractionResult result = seeker.getItem().use(level, player, InteractionHand.MAIN_HAND).getResult();
                    boolean found = tier >= step;
                    helper.assertTrue((result == InteractionResult.SUCCESS) == found,
                        "Seeker tier " + tier + " tracked the dragon incorrectly at " + distances[step]
                            + " blocks: result=" + result);
                }
            }
            player.moveTo(origin.add(130, 0, 0));
            dragon.setTame(true, false);
            for (int tier = 0; tier < names.length; tier++) {
                ItemStack seeker = item("iceandfire:" + names[tier]);
                player.setItemInHand(InteractionHand.MAIN_HAND, seeker);
                helper.assertTrue((seeker.getItem().use(level, player, InteractionHand.MAIN_HAND).getResult()
                    == InteractionResult.SUCCESS) == (tier < 2),
                    "Seeker tame-dragon filter changed: " + names[tier]);
            }
            dragon.discard();
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void dragonFluteAndCrystal(GameTestHelper helper) {
        var player = player(helper, "iaftfc_control");
        DragonBaseEntity dragon = IafEntities.FIRE_DRAGON.get().create(helper.getLevel());
        dragon.setNoAi(true);
        dragon.setAgeInDays(100);
        dragon.setTame(true, false);
        dragon.setOwnerUUID(player.getUUID());
        Vec3 position = Vec3.atCenterOf(helper.absolutePos(new net.minecraft.core.BlockPos(2, 2, 2)));
        dragon.moveTo(position);
        player.moveTo(position.add(1, 0, 0));
        helper.getLevel().addFreshEntity(dragon);
        helper.assertTrue(dragon.isOwnedBy(player) && !dragon.isModelDead(),
            "Control dragon is not alive and owned: tame=" + dragon.isTame() + ", owner=" + dragon.getOwnerUUID());
        dragon.setFlying(true);
        dragon.setHovering(true);
        player.setItemInHand(InteractionHand.MAIN_HAND, item("iceandfire:dragon_flute"));
        ((DragonFluteItem) player.getMainHandItem().getItem()).use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(!dragon.isFlying() && !dragon.isHovering(), "Owner's flute did not land the dragon");

        int command = dragon.getCommand();
        player.setItemInHand(InteractionHand.MAIN_HAND, item("iceandfire:dragon_stick"));
        dragon.mobInteract(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(dragon.getCommand() == (command + 1) % 3, "Dragon Staff did not cycle the command");

        player.setItemInHand(InteractionHand.MAIN_HAND, item("iceandfire:summoning_crystal_fire"));
        dragon.mobInteract(player, InteractionHand.MAIN_HAND);
        ItemStack crystal = player.getMainHandItem();
        helper.assertTrue(SummoningCrystalItem.hasDragon(crystal) && dragon.isBoundToCrystal(),
            "Summoning Crystal did not bind the owned dragon");
        CompoundTag saved = (CompoundTag) crystal.save(helper.getLevel().registryAccess());
        ItemStack loaded = ItemStack.parseOptional(helper.getLevel().registryAccess(), saved);
        helper.assertTrue(SummoningCrystalItem.hasDragon(loaded), "Crystal binding did not survive saving");
        dragon.discard();
        helper.getLevel().removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
        helper.succeed();
    }

    private static net.neoforged.neoforge.common.util.FakePlayer player(GameTestHelper helper, String name) {
        UUID id = UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(id, name));
        player.moveTo(Vec3.atCenterOf(helper.absolutePos(new net.minecraft.core.BlockPos(2, 2, 2))));
        helper.getLevel().addNewPlayer(player);
        return player;
    }

    private static void checkFood(GameTestHelper helper, String id, float saturation, float protein,
        float water, float vegetables, float decay) {
        ItemStack stack = item(id);
        var food = FoodCapability.get(stack);
        helper.assertTrue(food != null, "Missing TFC FoodCapability: " + id);
        var data = food.getData();
        helper.assertTrue(data.hunger() == 4 && data.saturation() == saturation && data.water() == water
            && data.nutrient(Nutrient.PROTEIN) == protein
            && data.nutrient(Nutrient.VEGETABLES) == vegetables
            && data.nutrient(Nutrient.GRAIN) == 0 && data.nutrient(Nutrient.FRUIT) == 0
            && data.nutrient(Nutrient.DAIRY) == 0 && data.intoxication() == 0
            && data.decayModifier() == decay,
            "Wrong TFC nutrition or decay for " + id);
        helper.assertTrue(ItemSizeManager.get(stack).getSize(stack) == Size.SMALL
            && ItemSizeManager.get(stack).getWeight(stack) == Weight.LIGHT,
            "Wrong food size or weight: " + id);
        ItemStack rotten = FoodCapability.setRotten(stack.copy());
        helper.assertTrue(FoodCapability.isRotten(rotten), "Food cannot rot: " + id);
        CompoundTag saved = (CompoundTag) rotten.save(helper.getLevel().registryAccess());
        ItemStack loaded = ItemStack.parseOptional(helper.getLevel().registryAccess(), saved);
        helper.assertTrue(loaded.is(stack.getItem()) && FoodCapability.isRotten(loaded),
            "Food decay state did not survive saving: " + id);
    }

    private static SmithingRecipeInput smithing(String template, String addition) {
        return new SmithingRecipeInput(item("tfc:metal/ingot/" + template + "_steel"),
            item("iceandfire:dragon_seeker"), item("tfc:metal/ingot/" + addition + "_steel"));
    }

    private static ItemStack smithingResult(GameTestHelper helper, net.minecraft.core.BlockPos table,
        SmithingRecipeInput input) {
        var player = FakePlayerFactory.getMinecraft(helper.getLevel());
        SmithingMenu menu = new SmithingMenu(0, player.getInventory(),
            ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(table)));
        menu.getSlot(SmithingMenu.TEMPLATE_SLOT).set(input.template().copy());
        menu.getSlot(SmithingMenu.BASE_SLOT).set(input.base().copy());
        menu.getSlot(SmithingMenu.ADDITIONAL_SLOT).set(input.addition().copy());
        menu.createResult();
        ItemStack result = menu.getSlot(SmithingMenu.RESULT_SLOT).getItem().copy();
        menu.removed(player);
        return result;
    }

    private static CraftingInput seeker(ItemStack metal) {
        return CraftingInput.of(1, 3, List.of(item("iceandfire:dragon_skull_fire"), metal.copy(), metal.copy()));
    }

    private static CraftingInput meal(ItemStack meat) {
        return meal(meat, new ItemStack(Items.BONE));
    }

    private static CraftingInput meal(ItemStack meat, ItemStack bone) {
        List<ItemStack> slots = new ArrayList<>();
        for (String row : List.of("BMB", "MBM", "BMB")) {
            for (char cell : row.toCharArray()) slots.add(cell == 'B' ? bone.copy() : meat.copy());
        }
        return CraftingInput.of(3, 3, slots);
    }

    private static ItemStack item(String id) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
    }

    @SuppressWarnings("unchecked")
    private static Recipe<CraftingInput> crafting(GameTestHelper helper, String id) {
        var holder = helper.getLevel().getRecipeManager().byKey(ResourceLocation.parse(id)).orElseThrow();
        helper.assertTrue(holder.value().getType() == RecipeType.CRAFTING, "Not a crafting recipe: " + id);
        return (Recipe<CraftingInput>) holder.value();
    }
}
