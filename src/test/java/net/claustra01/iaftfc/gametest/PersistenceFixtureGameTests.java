package net.claustra01.iaftfc.gametest;

import com.iafenvoy.iceandfire.entity.DragonEggEntity;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.item.block.entity.DragonForgeBlockEntity;
import com.iafenvoy.iceandfire.item.component.BestiaryPageComponent;
import com.iafenvoy.iceandfire.item.component.DragonHornComponent;
import com.iafenvoy.iceandfire.registry.IafDragonColors;
import com.iafenvoy.iceandfire.registry.IafDataComponents;
import com.iafenvoy.iceandfire.registry.IafEntities;
import com.iafenvoy.iceandfire.registry.IafRegistries;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.dries007.tfc.common.blockentities.CrucibleBlockEntity;
import net.dries007.tfc.common.component.heat.HeatCapability;
import net.dries007.tfc.common.component.food.FoodCapability;
import net.dries007.tfc.util.calendar.Calendars;
import net.claustra01.iaftfc.metal.Dragonsteel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("iaftfc_fixture")
@PrefixGameTestTemplate(false)
public final class PersistenceFixtureGameTests {
    @GameTest(template = "empty")
    public static void saveFixture(GameTestHelper helper) throws IOException {
        var level = helper.getLevel();
        DeterministicOutputs.write(level, "fixture");
        BlockPos pos = new BlockPos(32, 240, 32);
        level.setChunkForced(pos.getX() >> 4, pos.getZ() >> 4, true);
        level.setBlockAndUpdate(pos, Blocks.CHEST.defaultBlockState());
        ChestBlockEntity chest = (ChestBlockEntity) level.getBlockEntity(pos);
        ItemStack hot = new ItemStack(BuiltInRegistries.ITEM.get(
            ResourceLocation.parse("iaftfc:metal/ingot/dragonsteel_fire")));
        HeatCapability.setTemperature(hot, 1600);
        chest.setItem(0, hot);
        ItemStack book = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("iceandfire:bestiary")));
        book.set(IafDataComponents.BESTIARY_PAGES.get(), new BestiaryPageComponent(
            IafRegistries.BESTIARY_PAGE.stream().filter(page ->
                page.name().equals("introduction") || page.name().equals("dragonforge")).toList()));
        chest.setItem(1, book);
        var player = FakePlayerFactory.getMinecraft(level);
        DragonBaseEntity stored = IafEntities.FIRE_DRAGON.get().create(level);
        stored.setAgeInDays(10);
        stored.setTame(true, false);
        stored.setOwnerUUID(player.getUUID());
        var storedTag = new net.minecraft.nbt.CompoundTag();
        stored.save(storedTag);
        ItemStack horn = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("iceandfire:dragon_horn")));
        horn.set(IafDataComponents.DRAGON_HORN.get(), new DragonHornComponent(
            BuiltInRegistries.ENTITY_TYPE.getKey(stored.getType()), stored.getUUID(), storedTag));
        chest.setItem(2, horn);
        chest.setItem(3, new ItemStack(BuiltInRegistries.ITEM.get(
            ResourceLocation.parse("iaftfc:dragonarmor_steel_head"))));
        chest.setItem(4, FoodCapability.setCreationDate(new ItemStack(BuiltInRegistries.ITEM.get(
            ResourceLocation.parse("iceandfire:fire_dragon_flesh"))), FoodCapability.getRoundedCreationDate()));
        chest.setItem(5, FoodCapability.setRotten(new ItemStack(BuiltInRegistries.ITEM.get(
            ResourceLocation.parse("iceandfire:fire_dragon_flesh")))));
        level.setBlockAndUpdate(pos.east(), BuiltInRegistries.BLOCK.get(
            ResourceLocation.parse("tfc:crucible")).defaultBlockState());
        CrucibleBlockEntity crucible = (CrucibleBlockEntity) level.getBlockEntity(pos.east());
        crucible.getSyncableData().set(0, 2100);
        helper.assertTrue(crucible.getAlloy().fill(new FluidStack(
            Dragonsteel.METAL_PARTS.get("dragonsteel_fire").fluid().getSource(), 100),
            IFluidHandler.FluidAction.EXECUTE, crucible.containerInfo()) == 100,
            "Crucible could not contain molten Dragonsteel");
        crucible.setChanged();
        level.setBlockAndUpdate(pos.west(), BuiltInRegistries.BLOCK.get(
            ResourceLocation.parse("iceandfire:dragonforge_fire_core_disabled")).defaultBlockState());
        DragonForgeBlockEntity forge = (DragonForgeBlockEntity) level.getBlockEntity(pos.west());
        forge.setItem(0, new ItemStack(BuiltInRegistries.ITEM.get(
            ResourceLocation.parse("tfc:metal/ingot/red_steel"))));
        forge.setItem(1, new ItemStack(BuiltInRegistries.ITEM.get(
            ResourceLocation.parse("iceandfire:fire_dragon_blood"))));
        forge.transferPower(100);
        forge.setChanged();
        DragonEggEntity egg = IafEntities.DRAGON_EGG.get().create(level);
        egg.setEggType(IafDragonColors.RED);
        egg.setNoGravity(true);
        egg.setPos(pos.getX() + 0.5, pos.getY() + 2, pos.getZ() + 0.5);
        egg.setDragonAge(120);
        helper.assertTrue(level.addFreshEntity(egg), "Egg fixture could not spawn");
        DragonBaseEntity dragon = IafEntities.FIRE_DRAGON.get().create(level);
        dragon.setAgeInDays(10);
        dragon.setTame(true, false);
        dragon.setOwnerUUID(player.getUUID());
        dragon.setNoAi(true);
        dragon.setNoGravity(true);
        EquipmentSlot[] armorSlots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        String[] armorMetals = {"steel", "black_steel", "red_steel", "blue_steel"};
        String[] armorParts = {"head", "neck", "body", "tail"};
        for (int index = 0; index < armorSlots.length; index++) {
            dragon.setItemSlot(armorSlots[index], new ItemStack(BuiltInRegistries.ITEM.get(
                ResourceLocation.parse("iaftfc:dragonarmor_" + armorMetals[index] + "_" + armorParts[index]))));
        }
        dragon.moveTo(pos.getX() + 0.5, pos.getY() + 6, pos.getZ() + 0.5);
        helper.assertTrue(level.addFreshEntity(dragon), "Armored dragon fixture could not spawn");
        ArmorStand wearer = EntityType.ARMOR_STAND.create(level);
        wearer.setNoGravity(true);
        wearer.setItemSlot(EquipmentSlot.CHEST, new ItemStack(BuiltInRegistries.ITEM.get(
            ResourceLocation.parse("iceandfire:dragonsteel_fire_chestplate"))));
        wearer.moveTo(pos.getX() + 4.5, pos.getY() + 2, pos.getZ() + 0.5);
        helper.assertTrue(level.addFreshEntity(wearer), "Worn Dragonsteel fixture could not spawn");
        helper.assertTrue(chest.getItem(0).is(hot.getItem()) && crucible.getTemperature() == 2100,
            "Persistence fixture was not initialized");
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(level.getEntitiesOfClass(DragonEggEntity.class, new AABB(pos).inflate(3)).size() == 1,
                "Egg did not remain in the fixture chunk before save: removed=" + egg.isRemoved()
                    + ", pos=" + egg.position());
            net.minecraft.nbt.CompoundTag dragonTag = new net.minecraft.nbt.CompoundTag();
            dragon.save(dragonTag);
            net.minecraft.nbt.CompoundTag hornTag = new net.minecraft.nbt.CompoundTag();
            stored.save(hornTag);
            try {
                Files.writeString(Path.of(System.getProperty("iaftfc.gametest.fixture")),
                    pos.getX() + "," + pos.getY() + "," + pos.getZ() + ","
                        + HeatCapability.getTemperature(chest.getItem(0)) + "," + egg.getDragonAge() + ","
                        + dragon.getUUID() + "," + player.getUUID() + "," + stored.getUUID() + ","
                        + hornTag.getInt("AgeTicks") + "," + dragonTag.getLong("IaftfcLastCalendarTick") + ","
                        + FoodCapability.get(chest.getItem(4)).getCreationDate() + ","
                        + forge.saveWithoutMetadata(level.registryAccess()).getInt("CookTime") + ","
                        + Calendars.SERVER.getTicks() + "," + wearer.getUUID());
            } catch (IOException exception) {
                throw new IllegalStateException("Could not save fixture expectations", exception);
            }
            level.setChunkForced(pos.getX() >> 4, pos.getZ() >> 4, false);
            helper.succeed();
        });
    }
}
