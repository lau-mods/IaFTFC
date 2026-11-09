package net.claustra01.iaftfc.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Supplier;
import net.claustra01.iaftfc.IaFTFC;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Replaces stacks rolled by an unmodified source loot table according to {@link TfcLootRules}. */
public final class TfcLootReplacementModifier extends LootModifier {
    public static final MapCodec<TfcLootReplacementModifier> CODEC = RecordCodecBuilder.mapCodec(instance ->
        codecStart(instance).and(instance.group(
            TfcLootRules.Profile.CODEC.fieldOf("profile").forGetter(modifier -> modifier.profile),
            TfcLootRules.Element.CODEC.optionalFieldOf("element").forGetter(modifier -> modifier.element),
            Codec.unboundedMap(ResourceLocation.CODEC, ResourceLocation.CODEC)
                .optionalFieldOf("item_overrides", Map.of()).forGetter(modifier -> modifier.itemOverrides),
            Codec.intRange(1, 255).optionalFieldOf("finished_enchant_levels")
                .forGetter(modifier -> modifier.finishedEnchantLevels)))
            .apply(instance, TfcLootReplacementModifier::new));
    private static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> SERIALIZERS =
        DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, IaFTFC.MOD_ID);
    public static final Supplier<MapCodec<TfcLootReplacementModifier>> TYPE =
        SERIALIZERS.register("tfc_loot_replacement", () -> CODEC);

    private final TfcLootRules.Profile profile;
    private final Optional<TfcLootRules.Element> element;
    private final Map<ResourceLocation, ResourceLocation> itemOverrides;
    private final Optional<Integer> finishedEnchantLevels;

    public TfcLootReplacementModifier(LootItemCondition[] conditions, TfcLootRules.Profile profile,
        Optional<TfcLootRules.Element> element, Map<ResourceLocation, ResourceLocation> itemOverrides,
        Optional<Integer> finishedEnchantLevels) {
        super(conditions);
        this.profile = profile;
        this.element = element;
        this.itemOverrides = Map.copyOf(itemOverrides);
        this.finishedEnchantLevels = finishedEnchantLevels;
    }

    public static void register(IEventBus modBus) {
        SERIALIZERS.register(modBus);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        OptionalInt levels = finishedEnchantLevels.map(OptionalInt::of).orElseGet(OptionalInt::empty);
        ObjectArrayList<ItemStack> result = new ObjectArrayList<>(generatedLoot.size());
        for (ItemStack stack : generatedLoot) {
            result.addAll(TfcLootRules.replace(stack, profile, element, itemOverrides, levels, context));
        }
        return result;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return TYPE.get();
    }
}
