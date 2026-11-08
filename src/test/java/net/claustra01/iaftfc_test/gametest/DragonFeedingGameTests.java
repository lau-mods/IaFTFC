package net.claustra01.iaftfc_test.gametest;

import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.FireDragonEntity;
import com.iafenvoy.iceandfire.entity.IceDragonEntity;
import com.iafenvoy.iceandfire.entity.LightningDragonEntity;
import com.iafenvoy.iceandfire.entity.ai.DragonAITargetItemsGoal;
import com.iafenvoy.iceandfire.registry.IafEntities;
import com.iafenvoy.iceandfire.registry.IafItems;
import com.iafenvoy.uranus.object.item.FoodUtils;
import java.util.ArrayList;
import java.util.List;
import net.dries007.tfc.common.component.food.FoodCapability;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("iaftfc")
@PrefixGameTestTemplate(false)
public final class DragonFeedingGameTests {
    private static final String EMPTY_TEMPLATE = "empty";

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 200)
    public static void tfcNonRottenMeatFeedsDragons(GameTestHelper helper) {
        ItemStack rawMeat = fresh("tfc:food/beef");
        ItemStack cookedMeat = fresh("tfc:food/cooked_beef");
        helper.assertValueEqual(rawMeat.get(DataComponents.FOOD).nutrition(), 0,
            "TFC raw meat must retain zero vanilla nutrition");
        helper.assertValueEqual(cookedMeat.get(DataComponents.FOOD).nutrition(), 0,
            "TFC cooked meat must retain zero vanilla nutrition");
        helper.assertTrue(FoodUtils.getFoodPoints(rawMeat, true, false) > 0,
            "TFC FoodData hunger must produce positive Uranus food points");
        helper.assertTrue(FoodUtils.getFoodPoints(cookedMeat, true, false) > 0,
            "Cooked TFC meat must produce positive Uranus food points");

        FakePlayer player = FakePlayerFactory.getMinecraft(helper.getLevel());
        if (!helper.getLevel().players().contains(player)) {
            helper.getLevel().addNewPlayer(player);
        }
        verifyHandFeeding(helper, player,
            new FireDragonEntity(IafEntities.FIRE_DRAGON.get(), helper.getLevel()), fresh("tfc:food/beef"));
        verifyHandFeeding(helper, player,
            new LightningDragonEntity(IafEntities.LIGHTNING_DRAGON.get(), helper.getLevel()),
            fresh("tfc:food/cooked_beef"));
        verifyHandFeeding(helper, player,
            new IceDragonEntity(IafEntities.ICE_DRAGON.get(), helper.getLevel()), fresh("tfc:food/beef"));

        FireDragonEntity fire = dragon(new FireDragonEntity(IafEntities.FIRE_DRAGON.get(), helper.getLevel()), player);
        ItemStack rotten = FoodCapability.setRotten(fresh("tfc:food/beef"));
        player.setItemInHand(InteractionHand.MAIN_HAND, rotten);
        InteractionResult rottenResult = fire.mobInteract(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(!rottenResult.consumesAction(), "A Fire Dragon must reject rotten TFC meat");
        helper.assertValueEqual(rotten.getCount(), 1, "Rejected rotten meat must not be consumed");
        helper.assertValueEqual(fire.getHunger(), 0, "Rejected rotten meat must not restore hunger");

        verifyFishRules(helper, player);
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 200)
    public static void dragonMealUsesFreshTfcMeat(GameTestHelper helper) {
        RecipeHolder<?> holder = helper.getLevel().getServer().getRecipeManager()
            .byKey(ResourceLocation.parse("iceandfire:dragon_meal")).orElseThrow();
        helper.assertTrue(holder.value() instanceof CraftingRecipe,
            "The Dragon Meal override must remain a crafting recipe");
        CraftingRecipe recipe = (CraftingRecipe) holder.value();

        CraftingInput freshInput = dragonMealInput(() -> fresh("tfc:food/beef"));
        helper.assertTrue(recipe.matches(freshInput, helper.getLevel()),
            "Dragon Meal must accept fresh TFC meat");
        ItemStack result = recipe.assemble(freshInput, helper.getLevel().registryAccess());
        helper.assertTrue(result.is(IafItems.DRAGON_MEAL.get()),
            "The override must preserve the CE Dragon Meal output");

        CraftingInput rottenInput = dragonMealInput(() -> FoodCapability.setRotten(fresh("tfc:food/beef")));
        helper.assertTrue(!recipe.matches(rottenInput, helper.getLevel()),
            "Dragon Meal must reject rotten TFC meat");
        CraftingInput fishInput = dragonMealInput(() -> fresh("tfc:food/cod"));
        helper.assertTrue(!recipe.matches(fishInput, helper.getLevel()),
            "Dragon Meal must not treat TFC fish as meat");
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 200)
    public static void groundMeatSearchRejectsRottenFood(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        FireDragonEntity dragon = new FireDragonEntity(IafEntities.FIRE_DRAGON.get(), level);
        dragon.setAgeInDays(75);
        dragon.setHunger(0);
        dragon.setPos(helper.absolutePos(new net.minecraft.core.BlockPos(3, 2, 3)).getCenter());
        level.addFreshEntity(dragon);
        TestTargetItemsGoal goal = new TestTargetItemsGoal(dragon);

        ItemEntity rotten = itemEntity(helper, 3.5, FoodCapability.setRotten(fresh("tfc:food/beef")));
        ItemEntity fresh = itemEntity(helper, 5.5, fresh("tfc:food/beef"));
        helper.assertTrue(!goal.accepts(rotten), "Rotten TFC meat must be excluded during target selection");
        helper.assertTrue(goal.accepts(fresh), "Fresh TFC meat must be eligible during target selection");

        int delay = Math.floorMod((int) -level.getGameTime(), 4);
        helper.runAfterDelay(delay, () -> {
            helper.assertTrue(goal.canUse(), "The CE ground-food goal must find fresh TFC meat");
            helper.assertTrue(goal.target() == fresh, "The ground-food goal must skip nearer rotten meat");

            goal.select(fresh);
            goal.start();
            helper.assertTrue(goal.canContinueToUse(),
                "The dragon must be tracking fresh meat before it rots");
            fresh.setItem(FoodCapability.setRotten(fresh.getItem()));
            helper.assertTrue(!goal.canContinueToUse(),
                "A dragon must stop tracking meat that rots after targeting");
            int trackedCount = fresh.getItem().getCount();
            goal.tick();
            helper.assertValueEqual(fresh.getItem().getCount(), trackedCount,
                "Meat that rots before contact must not be consumed");
            helper.assertValueEqual(dragon.getHunger(), 0,
                "Meat that rots before consumption must not restore hunger");

            ItemEntity consumable = itemEntity(helper, 3.5, fresh("tfc:food/cooked_beef"));
            goal.select(consumable);
            goal.tick();
            helper.assertTrue(consumable.getItem().isEmpty(), "Fresh ground meat must be consumed on contact");
            helper.assertTrue(dragon.getHunger() > 0,
                "Fresh ground meat must restore hunger through CE's consumption path");
            helper.succeed();
        });
    }

    private static void verifyHandFeeding(GameTestHelper helper, FakePlayer player, DragonBaseEntity dragon,
        ItemStack meat) {
        dragon(dragon, player);
        int expected = FoodCapability.get(meat).getData().hunger() * 10;
        player.setItemInHand(InteractionHand.MAIN_HAND, meat);
        InteractionResult result = dragon.mobInteract(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(result.consumesAction(), "Fresh TFC meat must use CE's hand-feeding path");
        helper.assertTrue(meat.isEmpty(), "Hand feeding must consume one TFC meat");
        helper.assertValueEqual(dragon.getHunger(), expected,
            "Hand feeding must restore the TFC hunger value in Uranus food-point units");
    }

    private static void verifyFishRules(GameTestHelper helper, FakePlayer player) {
        FireDragonEntity fire = dragon(new FireDragonEntity(IafEntities.FIRE_DRAGON.get(), helper.getLevel()), player);
        LightningDragonEntity lightning = dragon(
            new LightningDragonEntity(IafEntities.LIGHTNING_DRAGON.get(), helper.getLevel()), player);
        IceDragonEntity ice = dragon(new IceDragonEntity(IafEntities.ICE_DRAGON.get(), helper.getLevel()), player);

        helper.assertTrue(FoodUtils.getFoodPoints(fresh("tfc:food/cod"), true, true) > 0,
            "Ice Dragons must retain CE's fish-food classification for TFC fish");
        helper.assertValueEqual(FoodUtils.getFoodPoints(fresh("tfc:food/cod"), true, false), 0,
            "TFC fish must not be folded into the meat classification");

        ItemStack fireFish = fresh("tfc:food/cod");
        player.setItemInHand(InteractionHand.MAIN_HAND, fireFish);
        helper.assertTrue(!fire.mobInteract(player, InteractionHand.MAIN_HAND).consumesAction(),
            "Fire Dragons must reject fish");
        helper.assertValueEqual(fireFish.getCount(), 1, "Fire Dragons must leave TFC fish untouched");
        helper.assertValueEqual(fire.getHunger(), 0, "Fire Dragons must gain no hunger from TFC fish");
        ItemStack lightningFish = fresh("tfc:food/cod");
        player.setItemInHand(InteractionHand.MAIN_HAND, lightningFish);
        helper.assertTrue(!lightning.mobInteract(player, InteractionHand.MAIN_HAND).consumesAction(),
            "Lightning Dragons must reject fish");
        helper.assertValueEqual(lightningFish.getCount(), 1, "Lightning Dragons must leave TFC fish untouched");
        helper.assertValueEqual(lightning.getHunger(), 0, "Lightning Dragons must gain no hunger from TFC fish");
        ItemStack iceFish = fresh("tfc:food/cod");
        player.setItemInHand(InteractionHand.MAIN_HAND, iceFish);
        helper.assertTrue(ice.mobInteract(player, InteractionHand.MAIN_HAND).consumesAction(),
            "Ice Dragons must consume CE's original fish targets");
        helper.assertTrue(iceFish.isEmpty(), "Ice Dragon fish feeding must consume the item");
    }

    private static <T extends DragonBaseEntity> T dragon(T dragon, FakePlayer player) {
        dragon.setAgeInDays(75);
        dragon.setHunger(0);
        dragon.tame(player);
        dragon.setTame(true, true);
        return dragon;
    }

    private static CraftingInput dragonMealInput(java.util.function.Supplier<ItemStack> meat) {
        List<ItemStack> stacks = new ArrayList<>(9);
        for (String row : List.of("BMB", "MBM", "BMB")) {
            for (int index = 0; index < row.length(); index++) {
                stacks.add(row.charAt(index) == 'B' ? new ItemStack(Items.BONE) : meat.get());
            }
        }
        return CraftingInput.of(3, 3, stacks);
    }

    private static ItemEntity itemEntity(GameTestHelper helper, double relativeX, ItemStack stack) {
        net.minecraft.core.BlockPos origin = helper.absolutePos(net.minecraft.core.BlockPos.ZERO);
        ItemEntity entity = new ItemEntity(helper.getLevel(), origin.getX() + relativeX,
            origin.getY() + 2, origin.getZ() + 3.5, stack);
        helper.getLevel().addFreshEntity(entity);
        return entity;
    }

    private static ItemStack fresh(String id) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
        return FoodCapability.setCreatedNow(new ItemStack(item));
    }

    private static final class TestTargetItemsGoal extends DragonAITargetItemsGoal {
        private TestTargetItemsGoal(DragonBaseEntity dragon) {
            super(dragon, 0, false, false, false);
        }

        private boolean accepts(ItemEntity item) {
            return targetEntitySelector.test(item);
        }

        private ItemEntity target() {
            return targetEntity;
        }

        private void select(ItemEntity item) {
            targetEntity = item;
        }
    }

    private DragonFeedingGameTests() {
    }
}
