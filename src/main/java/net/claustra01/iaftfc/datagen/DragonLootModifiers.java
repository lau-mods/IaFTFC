package net.claustra01.iaftfc.datagen;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.claustra01.iaftfc.IaFTFC;
import net.claustra01.iaftfc.loot.TfcLootReplacementModifier;
import net.claustra01.iaftfc.loot.TfcLootRules;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.data.GlobalLootModifierProvider;
import net.neoforged.neoforge.common.loot.LootTableIdCondition;

/** Attaches the TFC replacement GLM to the nine unmodified CE dragon chest tables. */
public final class DragonLootModifiers extends GlobalLootModifierProvider {
    /** Level of the CE male cave {@code enchant_with_levels} applied to its iron and diamond equipment. */
    private static final int MALE_CAVE_EQUIPMENT_LEVELS = 25;

    /**
     * CE 2.2's ice male cave rolls Gold where its other ice tables roll Silver, an upstream data mistake. Treat that
     * Gold as the ice element metal.
     */
    private static final Map<ResourceLocation, ResourceLocation> ICE_MALE_CAVE_OVERRIDES = Map.of(
        ResourceLocation.parse("minecraft:gold_ingot"), ResourceLocation.parse("tfc:metal/ingot/silver"),
        ResourceLocation.parse("minecraft:gold_nugget"), ResourceLocation.parse("tfc:ore/small_native_silver"));

    public DragonLootModifiers(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, IaFTFC.MOD_ID);
    }

    @Override
    protected void start() {
        for (TfcLootRules.Element element : TfcLootRules.Element.values()) {
            add(element, "roost", TfcLootRules.Profile.ORDINARY, Map.of(), Optional.empty());
            add(element, "female_cave", TfcLootRules.Profile.DANGEROUS, Map.of(), Optional.empty());
            add(element, "male_cave", TfcLootRules.Profile.LATEGAME,
                element == TfcLootRules.Element.ICE ? ICE_MALE_CAVE_OVERRIDES : Map.of(),
                Optional.of(MALE_CAVE_EQUIPMENT_LEVELS));
        }
    }

    private void add(TfcLootRules.Element element, String variant, TfcLootRules.Profile profile,
        Map<ResourceLocation, ResourceLocation> overrides, Optional<Integer> enchantLevels) {
        String table = element.getSerializedName() + "_dragon_" + variant;
        LootItemCondition condition = LootTableIdCondition.builder(
            ResourceLocation.fromNamespaceAndPath("iceandfire", "chest/" + table)).build();
        add("dragon_chest/" + table, new TfcLootReplacementModifier(new LootItemCondition[] {condition}, profile,
            Optional.of(element), overrides, enchantLevels));
    }
}
