package dev.iaftfc.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.iaftfc.IaFTFC;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

// Vanilla 1.21.1 ignores the source JSON's treasure flag, so it is retained only for faithful data encoding.
public record EnchantEquipment(int levels, boolean treasure, ResourceLocation source) implements LootItemFunction {
    public static final DeferredRegister<LootItemFunctionType<?>> FUNCTIONS =
        DeferredRegister.create(Registries.LOOT_FUNCTION_TYPE, IaFTFC.MOD_ID);
    private static final MapCodec<EnchantEquipment> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Codec.INT.fieldOf("levels").forGetter(EnchantEquipment::levels),
        Codec.BOOL.fieldOf("treasure").forGetter(EnchantEquipment::treasure),
        ResourceLocation.CODEC.fieldOf("source").forGetter(EnchantEquipment::source)
    ).apply(instance, EnchantEquipment::new));
    public static final DeferredHolder<LootItemFunctionType<?>, LootItemFunctionType<EnchantEquipment>> TYPE =
        FUNCTIONS.register("enchant_equipment", () -> new LootItemFunctionType<>(CODEC));

    @Override
    public LootItemFunctionType<EnchantEquipment> getType() {
        return TYPE.get();
    }

    @Override
    public ItemStack apply(ItemStack stack, LootContext context) {
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        if (path.startsWith("metal/unfinished_") || path.contains("_head/") || path.contains("_blade/")) return stack;
        ItemStack original = new ItemStack(BuiltInRegistries.ITEM.get(source));
        // The source item preserves the original enchantability and enchantment pool for this chest entry.
        EnchantmentHelper.enchantItem(context.getRandom(), original, levels, context.getLevel().registryAccess(), Optional.empty());
        EnchantmentHelper.setEnchantments(stack, EnchantmentHelper.getEnchantmentsForCrafting(original));
        return stack;
    }
}
