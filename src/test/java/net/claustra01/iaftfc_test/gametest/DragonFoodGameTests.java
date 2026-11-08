package net.claustra01.iaftfc_test.gametest;

import com.iafenvoy.iceandfire.compat.delight.DelightFoodItem;
import com.iafenvoy.iceandfire.item.DragonFleshItem;
import java.util.List;
import java.util.Set;
import net.dries007.tfc.common.component.food.FoodCapability;
import net.dries007.tfc.common.component.food.FoodData;
import net.dries007.tfc.common.component.food.IFood;
import net.dries007.tfc.common.component.food.Nutrient;
import net.dries007.tfc.common.player.IPlayerInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("iaftfc")
@PrefixGameTestTemplate(false)
public final class DragonFoodGameTests {
    private static final String EMPTY_TEMPLATE = "empty";

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 200)
    public static void sixDragonFoodsUseTfcNutritionAndDecay(GameTestHelper helper) {
        FakePlayer player = FakePlayerFactory.getMinecraft(helper.getLevel());
        if (!helper.getLevel().players().contains(player)) {
            helper.getLevel().addNewPlayer(player);
        }
        for (String element : List.of("fire", "ice", "lightning")) {
            ItemStack flesh = stack("iceandfire:" + element + "_dragon_flesh");
            verifyFood(helper, flesh, new ExpectedFood(8, 0, 2, 0, 0, 3, 2));
            helper.assertTrue(flesh.getItem() instanceof DragonFleshItem,
                "Dragon Flesh must retain CE's special-effect item implementation");
            FoodProperties fleshProperties = flesh.get(DataComponents.FOOD);
            helper.assertValueEqual(fleshProperties.nutrition(), 8,
                "TFC food data must not replace CE Dragon Flesh nutrition components");
            helper.assertValueEqual(fleshProperties.saturation(), 12.8F,
                "TFC food data must not replace CE Dragon Flesh saturation components");
            eatAndCheck(helper, player, flesh, 8, Nutrient.PROTEIN);
            if (element.equals("fire")) {
                helper.assertTrue(player.getRemainingFireTicks() > 0, "Fire flesh must retain CE's ignition");
                player.clearFire();
            } else if (element.equals("ice")) {
                helper.assertTrue(player.hasEffect(MobEffects.MOVEMENT_SLOWDOWN),
                    "Ice flesh must retain CE's slowness");
                player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            } else {
                helper.assertTrue(!helper.getLevel().getEntitiesOfClass(LightningBolt.class,
                    player.getBoundingBox().inflate(3)).isEmpty(),
                    "Lightning flesh must retain CE's lightning strike");
            }

            ItemStack rice = stack("iceandfire:cooked_rice_with_" + element + "_dragon_meat");
            verifyFood(helper, rice, new ExpectedFood(4, 20, 1.5, 1.7, 2, 3, 3.5));
            helper.assertTrue(rice.getItem() instanceof DelightFoodItem,
                "Dragon fried rice must retain CE's food item implementation");
            FoodProperties riceProperties = rice.get(DataComponents.FOOD);
            helper.assertValueEqual(riceProperties.nutrition(), 4,
                "TFC food data must not replace CE fried-rice nutrition components");
            helper.assertTrue(riceProperties.effects().size() == 2,
                "CE fried-rice special effects must remain registered");
            helper.assertTrue(riceProperties.usingConvertsTo().orElse(ItemStack.EMPTY).is(Items.BOWL),
                "CE fried rice must retain its bowl remainder");
            eatAndCheck(helper, player, rice, 4, Nutrient.GRAIN);
            helper.assertTrue(player.hasEffect(switch (element) {
                case "fire" -> MobEffects.FIRE_RESISTANCE;
                case "ice" -> MobEffects.JUMP;
                default -> MobEffects.MOVEMENT_SPEED;
            }), "Dragon fried rice must retain its CE effect");
        }

        Set<ResourceLocation> expected = Set.of(
            ResourceLocation.parse("iaftfc:fire_dragon_flesh"),
            ResourceLocation.parse("iaftfc:ice_dragon_flesh"),
            ResourceLocation.parse("iaftfc:lightning_dragon_flesh"),
            ResourceLocation.parse("iaftfc:cooked_rice_with_fire_dragon_meat"),
            ResourceLocation.parse("iaftfc:cooked_rice_with_ice_dragon_meat"),
            ResourceLocation.parse("iaftfc:cooked_rice_with_lightning_dragon_meat"));
        Set<ResourceLocation> actual = helper.getLevel().getServer().getResourceManager()
            .listResources("tfc/food", id -> id.getNamespace().equals("iaftfc") && id.getPath().endsWith(".json"))
            .keySet().stream().map(id -> ResourceLocation.fromNamespaceAndPath("iaftfc",
                id.getPath().substring("tfc/food/".length(), id.getPath().length() - ".json".length())))
            .collect(java.util.stream.Collectors.toSet());
        helper.assertValueEqual(actual, expected, "Only the six dragon foods may have IaFTFC definitions");
        helper.succeed();
    }

    private static void eatAndCheck(GameTestHelper helper, FakePlayer player, ItemStack stack, int hunger,
        Nutrient nutrient) {
        IPlayerInfo info = IPlayerInfo.get(player);
        info.nutrition().readFromNbt(new ListTag());
        player.getFoodData().setFoodLevel(0);
        float before = info.nutrition().getNutrient(nutrient);
        stack.getItem().finishUsingItem(stack, helper.getLevel(), player);
        helper.assertValueEqual(player.getFoodData().getFoodLevel(), hunger,
            "Eating dragon food must apply TFC hunger");
        helper.assertTrue(info.nutrition().getNutrient(nutrient) > before,
            "Eating dragon food must add its TFC nutrients: " + nutrient + " " + before + " -> "
                + info.nutrition().getNutrient(nutrient));
    }

    private static void verifyFood(GameTestHelper helper, ItemStack stack, ExpectedFood expected) {
        IFood food = FoodCapability.get(stack);
        helper.assertTrue(food != null, "Dragon food must have a TFC food capability");
        FoodData data = food.getData();
        helper.assertValueEqual(data.hunger(), expected.hunger(), "Incorrect TFC hunger");
        assertFloat(helper, data.water(), expected.water(), "Incorrect TFC water");
        assertFloat(helper, data.saturation(), expected.saturation(), "Incorrect TFC saturation");
        assertFloat(helper, data.nutrient(Nutrient.GRAIN), expected.grain(), "Incorrect grain nutrition");
        assertFloat(helper, data.nutrient(Nutrient.VEGETABLES), expected.vegetables(),
            "Incorrect vegetable nutrition");
        assertFloat(helper, data.nutrient(Nutrient.PROTEIN), expected.protein(), "Incorrect protein nutrition");
        assertFloat(helper, data.decayModifier(), expected.decay(), "Incorrect decay modifier");
        helper.assertTrue(!food.isRotten(), "A newly created dragon food must be fresh");

        long expectedLifetime = FoodCapability.getLifeTime(food.getDecayDateModifier());
        helper.assertValueEqual(food.getRottenDate() - food.getCreationDate(), expectedLifetime,
            "Dragon food must decay at the configured TFC rate");
    }

    private static void assertFloat(GameTestHelper helper, float actual, double expected, String message) {
        helper.assertTrue(Math.abs(actual - expected) < 0.001, message + ": expected " + expected + ", got " + actual);
    }

    private static ItemStack stack(String id) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
        return FoodCapability.setCreatedNow(new ItemStack(item));
    }

    private record ExpectedFood(int hunger, double water, double saturation, double grain, double vegetables,
        double protein, double decay) {
    }

    private DragonFoodGameTests() {
    }
}
