package net.claustra01.iaftfc.dragon;

import net.dries007.tfc.common.component.food.FoodCapability;
import net.dries007.tfc.common.component.food.IFood;
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

    private static TagKey<Item> tag(String namespace, String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(namespace, path));
    }

    private TfcDragonFood() {
    }
}
