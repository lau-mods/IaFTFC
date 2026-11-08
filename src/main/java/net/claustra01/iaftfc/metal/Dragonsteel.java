package net.claustra01.iaftfc.metal;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.claustra01.iaftfc.IaFTFC;
import net.dries007.tfc.common.LevelTier;
import net.dries007.tfc.common.TFCArmorMaterials;
import net.dries007.tfc.common.blockentities.TFCBlockEntities;
import net.dries007.tfc.common.blocks.MoltenFluidBlock;
import net.dries007.tfc.common.fluids.FluidHolder;
import net.dries007.tfc.common.fluids.MoltenFluid;
import net.dries007.tfc.util.Metal;
import net.dries007.tfc.util.registry.RegistrationHelpers;
import net.dries007.tfc.util.registry.RegistryMetal;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.pathfinder.PathType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public enum Dragonsteel implements RegistryMetal {
    FIRE("dragonsteel_fire", 0xB64A32, MapColor.FIRE, TFCArmorMaterials.RED_STEEL),
    ICE("dragonsteel_ice", 0x7FCBD9, MapColor.ICE, TFCArmorMaterials.BLUE_STEEL),
    LIGHTNING("dragonsteel_lightning", 0x726D83, MapColor.COLOR_PURPLE, TFCArmorMaterials.BLACK_STEEL);

    public static final float MELT_TEMPERATURE = 2000;
    public static final float FORGING_TEMPERATURE = 1200;
    public static final float WELDING_TEMPERATURE = 1600;
    public static final float FORGE_OUTPUT_TEMPERATURE = 1750;
    public static final int ANVIL_TIER = 7;

    public static final List<Metal.ItemType> ITEM_TYPES = List.of(
        Metal.ItemType.INGOT,
        Metal.ItemType.DOUBLE_INGOT,
        Metal.ItemType.SHEET,
        Metal.ItemType.DOUBLE_SHEET,
        Metal.ItemType.ROD,
        Metal.ItemType.AXE_HEAD,
        Metal.ItemType.HOE_HEAD,
        Metal.ItemType.PICKAXE_HEAD,
        Metal.ItemType.SHOVEL_HEAD,
        Metal.ItemType.SWORD_BLADE,
        Metal.ItemType.UNFINISHED_HELMET,
        Metal.ItemType.UNFINISHED_CHESTPLATE,
        Metal.ItemType.UNFINISHED_GREAVES,
        Metal.ItemType.UNFINISHED_BOOTS);
    public static final List<Metal.BlockType> BLOCK_TYPES = List.of(
        Metal.BlockType.BLOCK,
        Metal.BlockType.BLOCK_SLAB,
        Metal.BlockType.BLOCK_STAIRS,
        Metal.BlockType.ANVIL);

    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, IaFTFC.MOD_ID);
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, IaFTFC.MOD_ID);
    private static final DeferredRegister<FluidType> FLUID_TYPES =
        DeferredRegister.create(NeoForgeRegistries.FLUID_TYPES, "tfc");
    private static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, "tfc");

    private static final Map<Dragonsteel, Map<Metal.ItemType, DeferredHolder<Item, Item>>> METAL_ITEMS =
        new EnumMap<>(Dragonsteel.class);
    private static final Map<Dragonsteel, Map<Metal.BlockType, DeferredHolder<Block, Block>>> METAL_BLOCKS =
        new EnumMap<>(Dragonsteel.class);
    private static final Map<Dragonsteel, FluidHolder<BaseFlowingFluid>> METAL_FLUIDS =
        new EnumMap<>(Dragonsteel.class);
    private static final Map<Dragonsteel, DeferredHolder<Block, MoltenFluidBlock>> FLUID_BLOCKS =
        new EnumMap<>(Dragonsteel.class);
    private static final Map<Dragonsteel, DeferredHolder<Item, Item>> FLUID_BUCKETS =
        new EnumMap<>(Dragonsteel.class);
    private static final LevelTier DRAGONSTEEL_TIER = new DragonsteelTier();

    static {
        for (Dragonsteel metal : values()) {
            Map<Metal.BlockType, DeferredHolder<Block, Block>> blocks = new EnumMap<>(Metal.BlockType.class);
            METAL_BLOCKS.put(metal, blocks);
            for (Metal.BlockType type : BLOCK_TYPES) {
                DeferredHolder<Block, Block> block = BLOCKS.register(type.createName(metal), type.create(metal));
                blocks.put(type, block);
                ITEMS.register(type.createName(metal),
                    () -> type.createBlockItem(new Item.Properties().rarity(metal.rarity())).apply(block.get()));
            }

            Map<Metal.ItemType, DeferredHolder<Item, Item>> items = new EnumMap<>(Metal.ItemType.class);
            METAL_ITEMS.put(metal, items);
            for (Metal.ItemType type : ITEM_TYPES) {
                String id = "metal/" + type.name().toLowerCase(Locale.ROOT) + "/" + metal.getSerializedName();
                items.put(type, ITEMS.register(id, () -> type.create(metal)));
            }

            String fluidId = "metal/" + metal.getSerializedName();
            FluidHolder<BaseFlowingFluid> fluid = RegistrationHelpers.registerFluid(
                FLUID_TYPES,
                FLUIDS,
                fluidId,
                fluidId,
                "metal/flowing_" + metal.getSerializedName(),
                properties -> properties.block(FLUID_BLOCKS.get(metal)).bucket(FLUID_BUCKETS.get(metal))
                    .explosionResistance(100),
                () -> new FluidType(FluidType.Properties.create()
                    .adjacentPathType(PathType.LAVA)
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY_LAVA)
                    .descriptionId("fluid.tfc.metal." + metal.getSerializedName())
                    .rarity(metal.rarity())
                    .lightLevel(15)
                    .density(3000)
                    .viscosity(6000)
                    .temperature(2000)
                    .canConvertToSource(false)
                    .canDrown(false)
                    .canExtinguish(false)
                    .canHydrate(false)
                    .canPushEntity(false)
                    .canSwim(false)
                    .supportsBoating(false)
                    .fallDistanceModifier(0)),
                MoltenFluid.Source::new,
                MoltenFluid.Flowing::new);
            METAL_FLUIDS.put(metal, fluid);
            FLUID_BLOCKS.put(metal, BLOCKS.register("fluid/metal/" + metal.getSerializedName(),
                () -> new MoltenFluidBlock(fluid.source(),
                    net.minecraft.world.level.block.state.BlockBehaviour.Properties.ofFullCopy(Blocks.LAVA)
                        .noLootTable())));
            FLUID_BUCKETS.put(metal, ITEMS.register("metal/bucket/" + metal.getSerializedName(),
                () -> new BucketItem(fluid.getSource(), new Item.Properties().craftRemainder(Items.BUCKET)
                    .stacksTo(1).rarity(metal.rarity()))));
        }
    }

    private final String serializedName;
    private final int color;
    private final MapColor mapColor;
    private final TFCArmorMaterials.Id armorMaterial;

    Dragonsteel(String serializedName, int color, MapColor mapColor, TFCArmorMaterials.Id armorMaterial) {
        this.serializedName = serializedName;
        this.color = color;
        this.mapColor = mapColor;
        this.armorMaterial = armorMaterial;
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        BLOCKS.register(modBus);
        FLUID_TYPES.register(modBus);
        FLUIDS.register(modBus);
        modBus.addListener(Dragonsteel::addAnvilBlocks);
    }

    private static void addAnvilBlocks(BlockEntityTypeAddBlocksEvent event) {
        event.modify(TFCBlockEntities.ANVIL.get(),
            FIRE.getBlock(Metal.BlockType.ANVIL),
            ICE.getBlock(Metal.BlockType.ANVIL),
            LIGHTNING.getBlock(Metal.BlockType.ANVIL));
    }

    public static DeferredHolder<Item, Item> item(Dragonsteel metal, Metal.ItemType type) {
        return METAL_ITEMS.get(metal).get(type);
    }

    public static DeferredHolder<Block, Block> block(Dragonsteel metal, Metal.BlockType type) {
        return METAL_BLOCKS.get(metal).get(type);
    }

    public static FluidHolder<BaseFlowingFluid> fluid(Dragonsteel metal) {
        return METAL_FLUIDS.get(metal);
    }

    public static boolean isIngot(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        if (!id.getNamespace().equals(IaFTFC.MOD_ID)) {
            return false;
        }
        for (Dragonsteel metal : values()) {
            if (id.getPath().equals("metal/ingot/" + metal.getSerializedName())) {
                return true;
            }
        }
        return false;
    }

    public int color() {
        return color;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }

    @Override
    public LevelTier toolTier() {
        return DRAGONSTEEL_TIER;
    }

    @Override
    public Holder<ArmorMaterial> armorMaterial() {
        return armorMaterial.holder();
    }

    @Override
    public int armorDurability(ArmorItem.Type type) {
        return switch (type) {
            case HELMET -> armorMaterial.headDamage();
            case BODY, CHESTPLATE -> armorMaterial.chestDamage();
            case LEGGINGS -> armorMaterial.legDamage();
            case BOOTS -> armorMaterial.feetDamage();
        };
    }

    @Override
    public Block getBlock(Metal.BlockType type) {
        DeferredHolder<Block, Block> block = METAL_BLOCKS.get(this).get(type);
        if (block == null) {
            throw new IllegalArgumentException("Dragonsteel block type is not registered: " + type);
        }
        return block.get();
    }

    @Override
    public MapColor mapColor() {
        return mapColor;
    }

    @Override
    public Rarity rarity() {
        return Rarity.EPIC;
    }

    @Override
    public float weatheringResistance() {
        return -1;
    }

    private static final class DragonsteelTier implements LevelTier {
        @Override
        public int level() {
            return ANVIL_TIER;
        }

        @Override
        public int getUses() {
            return 0;
        }

        @Override
        public float getSpeed() {
            return 0;
        }

        @Override
        public float getAttackDamageBonus() {
            return 0;
        }

        @Override
        public TagKey<Block> getIncorrectBlocksForDrops() {
            return BlockTags.INCORRECT_FOR_NETHERITE_TOOL;
        }

        @Override
        public int getEnchantmentValue() {
            return 0;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.EMPTY;
        }
    }
}
