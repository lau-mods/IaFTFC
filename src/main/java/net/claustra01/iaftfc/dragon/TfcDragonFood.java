package net.claustra01.iaftfc.dragon;

import net.dries007.tfc.common.component.food.FoodCapability;
import java.util.Set;
import net.dries007.tfc.common.component.food.IFood;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class TfcDragonFood {
    private static final int URANUS_FOOD_POINT_SCALE = 10;
    private static final TagKey<Item> MEAT = tag("c", "foods/meat");
    private static final TagKey<Item> FISH = tag("c", "foods/fish");
    private static final Set<ResourceLocation> CE_FOOD_COMPONENT_ITEMS = Set.of(
        ResourceLocation.parse("iceandfire:fire_dragon_flesh"),
        ResourceLocation.parse("iceandfire:ice_dragon_flesh"),
        ResourceLocation.parse("iceandfire:lightning_dragon_flesh"),
        ResourceLocation.parse("iceandfire:cooked_rice_with_fire_dragon_meat"),
        ResourceLocation.parse("iceandfire:cooked_rice_with_ice_dragon_meat"),
        ResourceLocation.parse("iceandfire:cooked_rice_with_lightning_dragon_meat"));

    public static int foodPointsOverride(ItemStack stack, boolean acceptsMeat, boolean acceptsFish) {
        IFood food = FoodCapability.get(stack);
        if (food == null) {
            return -1;
        }
        if (food.isRotten()) {
            return 0;
        }

        boolean ceFood = acceptsMeat && stack.is(ItemTags.MEAT)
            || acceptsFish && stack.is(ItemTags.FISHES);
        boolean tfcMeat = acceptsMeat && stack.is(MEAT) && !stack.is(FISH);
        boolean tfcFish = acceptsFish && stack.is(FISH);
        return ceFood || tfcMeat || tfcFish ? food.getData().hunger() * URANUS_FOOD_POINT_SCALE : 0;
    }

    /**
     * Dragon foods whose CE {@code minecraft:food} component must survive TFC's post-reload default component
     * rewrite, which otherwise replaces it with an empty component whenever the TFC stack size differs.
     */
    public static boolean keepsCeFoodComponent(Item item) {
        return CE_FOOD_COMPONENT_ITEMS.contains(BuiltInRegistries.ITEM.getKey(item));
    }

    private static TagKey<Item> tag(String namespace, String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(namespace, path));
    }

    private TfcDragonFood() {
    }
}
