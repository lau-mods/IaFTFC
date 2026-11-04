package net.claustra01.iaftfc.gametest;

import com.iafenvoy.iceandfire.entity.DragonEggEntity;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.item.block.entity.DragonForgeBlockEntity;
import com.iafenvoy.iceandfire.registry.IafDataComponents;
import com.iafenvoy.iceandfire.registry.IafEntities;
import net.claustra01.iaftfc.metal.Dragonsteel;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import net.dries007.tfc.common.blockentities.CrucibleBlockEntity;
import net.dries007.tfc.common.component.heat.HeatCapability;
import net.dries007.tfc.common.component.food.FoodCapability;
import net.dries007.tfc.util.calendar.Calendars;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.phys.AABB;
import java.util.UUID;
import com.mojang.authlib.GameProfile;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("iaftfc_verify")
@PrefixGameTestTemplate(false)
public final class PersistenceVerifyGameTests {
    @GameTest(template = "empty", timeoutTicks = 300)
    public static void savedStateSurvivesRestart(GameTestHelper helper) throws IOException {
        var level = helper.getLevel();
        DeterministicOutputs.write(level, "verify");
        String[] coordinates = Files.readString(Path.of(System.getProperty("iaftfc.gametest.fixture"))).split(",");
        BlockPos pos = new BlockPos(Integer.parseInt(coordinates[0]), Integer.parseInt(coordinates[1]),
            Integer.parseInt(coordinates[2]));
        float savedHeat = Float.parseFloat(coordinates[3]);
        int savedEggAge = Integer.parseInt(coordinates[4]);
        UUID dragonId = UUID.fromString(coordinates[5]);
        UUID ownerId = UUID.fromString(coordinates[6]);
        UUID storedId = UUID.fromString(coordinates[7]);
        int storedAge = Integer.parseInt(coordinates[8]);
        long calendarTick = Long.parseLong(coordinates[9]);
        long creationDate = Long.parseLong(coordinates[10]);
        int cookTime = Integer.parseInt(coordinates[11]);
        long savedCalendarTicks = Long.parseLong(coordinates[12]);
        UUID wearerId = UUID.fromString(coordinates[13]);
        helper.assertTrue(level.setChunkForced(pos.getX() >> 4, pos.getZ() >> 4, true),
            "Fixture chunk remained force-loaded across server shutdown");
        ChunkPos chunk = new ChunkPos(pos);
        level.getChunkSource().addRegionTicket(net.minecraft.server.level.TicketType.PLAYER,
            chunk, 0, chunk, true);
        var loader = FakePlayerFactory.getMinecraft(level);
        loader.setPos(pos.getX() + 0.5, pos.getY() + 2, pos.getZ() + 0.5);
        level.addNewPlayer(loader);
        level.getChunk(pos);
        entityManager(level).updateChunkStatus(chunk, FullChunkStatus.ENTITY_TICKING);
        helper.runAfterDelay(10, () -> {
            ChestBlockEntity chest = (ChestBlockEntity) level.getBlockEntity(pos);
            helper.assertTrue(chest != null, "Saved fixture chunk or chest was lost");
            var hot = chest.getItem(0);
            float expectedHeat = HeatCapability.adjustTemp(savedHeat,
                HeatCapability.get(hot).getHeatCapacity(), Calendars.SERVER.getTicks() - savedCalendarTicks);
            helper.assertTrue(hot.is(BuiltInRegistries.ITEM.get(ResourceLocation.parse(
                "iaftfc:metal/ingot/dragonsteel_fire"))) && HeatCapability.getTemperature(hot) >= 0
                && HeatCapability.getTemperature(hot) <= savedHeat
                && Math.abs(HeatCapability.getTemperature(hot) - expectedHeat) < 2,
                "Dragonsteel type or heat changed on restart: saved=" + savedHeat
                    + ", loaded=" + HeatCapability.getTemperature(hot) + ", expected=" + expectedHeat);
            for (int slot = 1; slot <= 5; slot++) {
                helper.assertTrue(!chest.getItem(slot).isEmpty() && chest.getItem(slot).getCount() == 1,
                    "Saved item was lost or duplicated in slot " + slot);
            }
            helper.assertTrue(level.getBlockEntity(pos.east()) instanceof CrucibleBlockEntity,
                "Crucible was lost on restart");
            CrucibleBlockEntity crucible = (CrucibleBlockEntity) level.getBlockEntity(pos.east());
            helper.assertTrue(crucible.getTemperature() >= 0 && crucible.getTemperature() <= 2100
                && crucible.getAlloy().getAmount() == 100
                && crucible.getAlloy().getContent().containsKey(
                    Dragonsteel.METAL_PARTS.get("dragonsteel_fire").fluid().getSource()),
                "Crucible metal, amount or temperature changed on restart: temperature="
                    + crucible.getTemperature() + ", amount=" + crucible.getAlloy().getAmount());
            helper.assertTrue(level.getBlockEntity(pos.west()) instanceof DragonForgeBlockEntity forge
                && forge.getItem(0).is(BuiltInRegistries.ITEM.get(ResourceLocation.parse("tfc:metal/ingot/red_steel")))
                && forge.getItem(1).is(BuiltInRegistries.ITEM.get(ResourceLocation.parse("iceandfire:fire_dragon_blood")))
                && forge.saveWithoutMetadata(level.registryAccess()).getInt("CookTime") == cookTime,
                "Dragon Forge inventory or cook progress was lost");
            var pages = chest.getItem(1).get(IafDataComponents.BESTIARY_PAGES.get());
            helper.assertTrue(pages != null && pages.pages().size() == 2
                && pages.pages().stream().anyMatch(page -> page.name().equals("dragonforge")),
                "Bestiary unlock was lost");
            var horn = chest.getItem(2).get(IafDataComponents.DRAGON_HORN.get());
            helper.assertTrue(horn != null && horn.entityUuid().equals(storedId)
                && horn.entityData().getInt("AgeTicks") == storedAge,
                "Horn storage or age was lost");
            DragonBaseEntity stored = IafEntities.FIRE_DRAGON.get().create(level);
            stored.load(horn.entityData());
            helper.assertTrue(stored.getOwnerUUID().equals(ownerId) && stored.getAgeInDays() == 10,
                "Stored dragon owner or age was lost");
            helper.assertTrue(FoodCapability.get(chest.getItem(4)).getCreationDate() == creationDate
                && FoodCapability.isRotten(chest.getItem(5)), "Food creation date or decay state was lost");
            helper.runAfterDelay(90, () -> {
                checkEntities(helper, pos, savedEggAge, dragonId, ownerId, calendarTick, wearerId, chest.getItem(3));
                var spawnRule = level.getGameRules().getRule(GameRules.RULE_SPAWN_CHUNK_RADIUS);
                int spawnRadius = spawnRule.get();
                spawnRule.set(0, level.getServer());
                level.removePlayerImmediately(loader, Entity.RemovalReason.DISCARDED);
                level.setChunkForced(pos.getX() >> 4, pos.getZ() >> 4, false);
                level.getChunkSource().removeRegionTicket(net.minecraft.server.level.TicketType.PLAYER,
                    chunk, 0, chunk, true);
                entityManager(level).updateChunkStatus(chunk, FullChunkStatus.INACCESSIBLE);
                helper.runAfterDelay(40, () -> {
                    helper.assertTrue(!level.areEntitiesLoaded(chunk.toLong())
                        && level.getEntitiesOfClass(DragonEggEntity.class, new AABB(pos).inflate(10)).isEmpty(),
                        "Fixture entities did not unload with their chunk");
                    spawnRule.set(spawnRadius, level.getServer());
                    var reloader = FakePlayerFactory.get(level, new GameProfile(
                        new UUID(0, 2), "FixtureReload"));
                    reloader.setPos(pos.getX() + 0.5, pos.getY() + 2, pos.getZ() + 0.5);
                    level.addNewPlayer(reloader);
                    level.setChunkForced(pos.getX() >> 4, pos.getZ() >> 4, true);
                    level.getChunkSource().addRegionTicket(net.minecraft.server.level.TicketType.PLAYER,
                        chunk, 0, chunk, true);
                    level.getChunk(pos);
                    entityManager(level).updateChunkStatus(chunk, FullChunkStatus.ENTITY_TICKING);
                    helper.runAfterDelay(40, () -> {
                        checkEntities(helper, pos, savedEggAge, dragonId, ownerId, calendarTick, wearerId,
                            ((ChestBlockEntity) level.getBlockEntity(pos)).getItem(3));
                        level.removePlayerImmediately(reloader, Entity.RemovalReason.DISCARDED);
                        helper.succeed();
                    });
                });
            });
        });
    }

    private static void checkEntities(GameTestHelper helper, BlockPos pos, int eggAge, UUID dragonId,
        UUID ownerId, long calendarTick, UUID wearerId, net.minecraft.world.item.ItemStack armor) {
        var level = helper.getLevel();
        helper.assertTrue(level.areEntitiesLoaded(new ChunkPos(pos).toLong()),
            "Fixture chunk entities did not enter the live level");
        var eggs = level.getEntitiesOfClass(DragonEggEntity.class, new AABB(pos).inflate(10));
        var dragons = level.getEntitiesOfClass(DragonBaseEntity.class, new AABB(pos).inflate(10));
        helper.assertTrue(eggs.size() == 1 && eggs.getFirst().getDragonAge() == eggAge,
            "Egg age was lost or duplicated after reload: count=" + eggs.size()
                + ", ages=" + eggs.stream().map(DragonEggEntity::getDragonAge).toList());
        helper.assertTrue(dragons.size() == 1 && dragons.getFirst().getUUID().equals(dragonId)
            && dragons.getFirst().getOwnerUUID().equals(ownerId)
            && dragons.getFirst().getAgeInDays() == 10
            && dragons.getFirst().getItemBySlot(EquipmentSlot.HEAD).is(armor.getItem()),
            "Dragon owner, age or armor was lost or duplicated after reload");
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        String[] metals = {"steel", "black_steel", "red_steel", "blue_steel"};
        String[] parts = {"head", "neck", "body", "tail"};
        for (int index = 0; index < slots.length; index++) {
            helper.assertTrue(dragons.getFirst().getItemBySlot(slots[index]).is(BuiltInRegistries.ITEM.get(
                ResourceLocation.parse("iaftfc:dragonarmor_" + metals[index] + "_" + parts[index]))),
                "Dragon Armor part was lost: " + parts[index]);
        }
        var wearers = level.getEntitiesOfClass(ArmorStand.class, new AABB(pos).inflate(10));
        helper.assertTrue(wearers.size() == 1 && wearers.getFirst().getUUID().equals(wearerId)
            && wearers.getFirst().getItemBySlot(EquipmentSlot.CHEST).is(BuiltInRegistries.ITEM.get(
                ResourceLocation.parse("iceandfire:dragonsteel_fire_chestplate"))),
            "Equipped Dragonsteel armor was lost or duplicated");
        var tag = new net.minecraft.nbt.CompoundTag();
        dragons.getFirst().save(tag);
        helper.assertTrue(tag.getLong("IaftfcLastCalendarTick") >= calendarTick
            && tag.getLong("IaftfcLastCalendarTick") <= Calendars.SERVER.getCalendarTicks(),
            "Dragon calendar growth base rolled back or advanced beyond the server calendar");
    }

    @SuppressWarnings("unchecked")
    private static PersistentEntitySectionManager<Entity> entityManager(ServerLevel level) {
        // GameTest does not make a fixture chunk outside its template entity-ticking without a real client.
        try {
            Field field = ServerLevel.class.getDeclaredField("entityManager");
            field.setAccessible(true);
            return (PersistentEntitySectionManager<Entity>) field.get(level);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not access GameTest entity chunk manager", exception);
        }
    }
}
