package net.claustra01.iaftfc_test.gametest;

import com.google.gson.JsonElement;
import com.iafenvoy.iceandfire.item.block.entity.DragonForgeBlockEntity;
import com.iafenvoy.iceandfire.recipe.DragonForgeRecipe;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import java.util.List;
import net.claustra01.iaftfc.metal.Dragonsteel;
import net.dries007.tfc.common.component.heat.HeatCapability;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("iaftfc")
@PrefixGameTestTemplate(false)
public final class DragonForgeGameTests {
    private static final String EMPTY_TEMPLATE = "empty";
    private static final List<ForgeCase> FORGES = List.of(
        new ForgeCase("fire", "tfc:metal/ingot/red_steel", "iceandfire:fire_dragon_blood",
            "iceandfire:dragonforge_fire_core", Dragonsteel.FIRE),
        new ForgeCase("ice", "tfc:metal/ingot/blue_steel", "iceandfire:ice_dragon_blood",
            "iceandfire:dragonforge_ice_core", Dragonsteel.ICE),
        new ForgeCase("lightning", "tfc:metal/ingot/black_steel", "iceandfire:lightning_dragon_blood",
            "iceandfire:dragonforge_lightning_core", Dragonsteel.LIGHTNING));

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 200)
    public static void dragonForgeOutputsStayHotThroughCodecAndSync(GameTestHelper helper) {
        for (int index = 0; index < FORGES.size(); index++) {
            ForgeCase forgeCase = FORGES.get(index);
            DragonForgeRecipe recipe = dragonForgeRecipe(helper,
                "iceandfire:dragonforge/dragonsteel_" + forgeCase.element() + "_ingot");
            ItemStack input = stack(forgeCase.input());
            ItemStack blood = stack(forgeCase.blood());
            helper.assertValueEqual(recipe.getDragonType(), forgeCase.element(),
                "Dragon Forge recipe must keep the CE element check");
            helper.assertValueEqual(recipe.getCookTime(), 1000,
                "Dragon Forge recipe must keep the CE cook time");
            helper.assertTrue(recipe.isValidInput(input), "Dragon Forge must use the matching TFC steel");
            helper.assertTrue(recipe.isValidBlood(blood), "Dragon Forge must use the matching dragon blood");
            assertHotIngot(helper, recipe.getResultItem(), forgeCase.metal(), "display result");

            BlockPos relativePos = new BlockPos(1 + index * 2, 2, 1);
            Block core = block(forgeCase.core());
            helper.setBlock(relativePos, core);
            DragonForgeBlockEntity forge = (DragonForgeBlockEntity)helper.getBlockEntity(relativePos);
            forge.setItem(0, input.copy());
            forge.setItem(1, blood.copy());
            helper.assertTrue(forge.canSmelt(), "The matching element Forge must accept its recipe");
            forge.smeltItem();
            assertHotIngot(helper, forge.getItem(2), forgeCase.metal(), "actual Forge output");

            CompoundTag savedTag = forge.saveWithoutMetadata(helper.getLevel().registryAccess());
            DragonForgeBlockEntity loaded = copy(forge, savedTag, helper);
            assertHotIngot(helper, loaded.getItem(2), forgeCase.metal(), "saved and loaded output");
            CompoundTag updateTag = forge.getUpdateTag(helper.getLevel().registryAccess());
            DragonForgeBlockEntity synced = copy(forge, updateTag, helper);
            assertHotIngot(helper, synced.getItem(2), forgeCase.metal(), "network update output");

            DragonForgeRecipe.Serializer serializer = (DragonForgeRecipe.Serializer)recipe.getSerializer();
            DynamicOps<JsonElement> ops = helper.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
            JsonElement encoded = serializer.codec().codec().encodeStart(ops, recipe).getOrThrow();
            DragonForgeRecipe decoded = serializer.codec().codec().parse(ops, encoded).getOrThrow();
            assertHotIngot(helper, decoded.getResultItem(), forgeCase.metal(), "recipe codec output");

            RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(
                Unpooled.buffer(), helper.getLevel().registryAccess());
            serializer.streamCodec().encode(buffer, recipe);
            DragonForgeRecipe streamed = serializer.streamCodec().decode(buffer);
            assertHotIngot(helper, streamed.getResultItem(), forgeCase.metal(), "recipe stream codec output");
        }
        helper.succeed();
    }

    private static DragonForgeBlockEntity copy(DragonForgeBlockEntity source, CompoundTag tag,
        GameTestHelper helper) {
        BlockState state = source.getBlockState();
        DragonForgeBlockEntity copy = new DragonForgeBlockEntity(source.getBlockPos(), state);
        copy.setLevel(helper.getLevel());
        copy.loadWithComponents(tag, helper.getLevel().registryAccess());
        return copy;
    }

    private static void assertHotIngot(GameTestHelper helper, ItemStack stack, Dragonsteel metal,
        String stage) {
        helper.assertTrue(stack.is(Dragonsteel.item(metal, net.dries007.tfc.util.Metal.ItemType.INGOT).get()),
            stage + " must use the IaFTFC Dragonsteel ingot");
        helper.assertTrue(Math.abs(HeatCapability.getTemperature(stack)
            - Dragonsteel.FORGE_OUTPUT_TEMPERATURE) < 0.01F,
            stage + " must retain 1750 C");
    }

    private static DragonForgeRecipe dragonForgeRecipe(GameTestHelper helper, String id) {
        Recipe<?> recipe = helper.getLevel().getServer().getRecipeManager()
            .byKey(ResourceLocation.parse(id))
            .orElseThrow(() -> new AssertionError("Missing recipe " + id)).value();
        helper.assertTrue(recipe instanceof DragonForgeRecipe, id + " must be a Dragon Forge recipe");
        return (DragonForgeRecipe)recipe;
    }

    private static ItemStack stack(String id) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
    }

    private static Block block(String id) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id));
    }

    private record ForgeCase(String element, String input, String blood, String core, Dragonsteel metal) {
    }

    private DragonForgeGameTests() {
    }
}
