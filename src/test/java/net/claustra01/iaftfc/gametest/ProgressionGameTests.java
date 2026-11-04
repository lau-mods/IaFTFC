package net.claustra01.iaftfc.gametest;

import com.google.gson.JsonParser;
import com.iafenvoy.iceandfire.item.block.entity.DragonForgeBlockEntity;
import com.iafenvoy.iceandfire.item.block.entity.DragonForgeInputBlockEntity;
import com.iafenvoy.iceandfire.recipe.DragonForgeRecipe;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.DragonEggEntity;
import com.iafenvoy.iceandfire.registry.IafDragonColors;
import com.iafenvoy.iceandfire.registry.IafEntities;
import com.iafenvoy.iceandfire.registry.IafAttributes;
import com.iafenvoy.iceandfire.config.IafCommonConfig;
import com.iafenvoy.iceandfire.entity.util.dragon.IafDragonDestructionManager;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.claustra01.iaftfc.metal.Dragonsteel;
import net.claustra01.iaftfc.dragon.CalendarDragonGrowth;
import net.dries007.tfc.util.calendar.Calendars;
import net.dries007.tfc.util.calendar.ICalendar;
import net.dries007.tfc.world.TFCChunkGenerator;
import net.dries007.tfc.common.blockentities.AnvilBlockEntity;
import net.dries007.tfc.common.component.forge.ForgeStep;
import net.dries007.tfc.common.component.heat.HeatCapability;
import net.dries007.tfc.common.container.KnappingContainer;
import net.dries007.tfc.common.recipes.AnvilRecipe;
import net.dries007.tfc.common.recipes.WeldingRecipe;
import net.dries007.tfc.common.recipes.CastingRecipe;
import net.dries007.tfc.common.component.mold.IMold;
import net.dries007.tfc.util.data.KnappingType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("iaftfc")
@PrefixGameTestTemplate(false)
public final class ProgressionGameTests {
    private static final String[] ELEMENTS = {"fire", "ice", "lightning"};
    private static final String[] STEELS = {"red_steel", "blue_steel", "black_steel"};
    private static final String[] SCALE_COLORS = {"red", "blue", "electric"};

    @GameTest(template = "dragon_forge", timeoutTicks = 1000)
    public static void connectedSurvivalProgression(GameTestHelper helper) {
        var player = FakePlayerFactory.getMinecraft(helper.getLevel());
        List<List<ItemStack>> scales = new ArrayList<>();
        for (int index = 0; index < ELEMENTS.length; index++) {
            scales.add(dragonScalesFromLoot(helper, index, index == 0 ? 22 : 21));
        }
        ItemStack bone = boneFromSkeleton(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, bone);
        KnappingContainer knapping = KnappingContainer.create(bone, KnappingType.get(bone),
            InteractionHand.MAIN_HAND, player.getInventory().selected, player.getInventory(), 0);
        String[] swordPattern = {"  ###", "   ##", "#   #", "##  #", "#### "};
        for (int y = 0; y < 5; y++) for (int x = 0; x < 5; x++) {
            if (swordPattern[y].charAt(x) == ' ') knapping.onButtonPress(y * 5 + x, new CompoundTag());
        }
        ItemStack blade = knapping.getSlot(0).getItem().copy();
        helper.assertTrue(bone.isEmpty() && blade.is(item("iaftfc:dragonbone/sword_blade").getItem()),
            "One recovered bone did not produce a sword blade");
        ItemStack boneSword = craft(helper, List.of(blade, ItemStack.EMPTY, ItemStack.EMPTY,
            new ItemStack(Items.STICK), ItemStack.EMPTY, ItemStack.EMPTY,
            ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY));
        helper.assertTrue(boneSword.is(item("iceandfire:dragonbone_sword").getItem()),
            "Knapped blade did not assemble the original sword");
        knapping.removed(player);

        ItemStack scale = scales.get(0).removeFirst();
        player.setItemInHand(InteractionHand.MAIN_HAND, scale);
        KnappingContainer scaleKnapping = KnappingContainer.create(scale, KnappingType.get(scale),
            InteractionHand.MAIN_HAND, player.getInventory().selected, player.getInventory(), 0);
        String[] helmetPattern = {"#####", "#   #", "#   #", "     ", "     "};
        for (int y = 0; y < 5; y++) for (int x = 0; x < 5; x++) {
            if (helmetPattern[y].charAt(x) == ' ') scaleKnapping.onButtonPress(y * 5 + x, new CompoundTag());
        }
        helper.assertTrue(scale.isEmpty() && scaleKnapping.getSlot(0).getItem()
            .is(item("iceandfire:armor_red_helmet").getItem()),
            "One recovered scale did not produce the matching armor");
        scaleKnapping.removed(player);
        List<List<ItemStack>> baseSteels = prepareTfcSteels(helper);

        for (int index = 0; index < 3; index++) {
            int x = 3 + index * 12;
            BlockPos center = new BlockPos(x, 3, 13);
            for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                if (dy == 0 && dx == 0 && dz == 0) continue;
                String block = dy == 0 && dx == 0 && dz == -1
                    ? "iceandfire:dragonforge_" + ELEMENTS[index] + "_input"
                    : dy != 0 && dx != 0 && dz != 0 ? "iceandfire:dragon_bone_block"
                    : "iceandfire:dragonforge_" + ELEMENTS[index] + "_brick";
                helper.setBlock(center.offset(dx, dy, dz), BuiltInRegistries.BLOCK.get(ResourceLocation.parse(block)));
            }
            helper.setBlock(center, BuiltInRegistries.BLOCK.get(ResourceLocation.parse(
                "iceandfire:dragonforge_" + ELEMENTS[index] + "_core_disabled")));
        }
        helper.runAfterDelay(3, () -> {
            DragonEggEntity egg = IafEntities.DRAGON_EGG.get().create(helper.getLevel());
            egg.setEggType(IafDragonColors.RED);
            egg.setNoGravity(true);
            BlockPos eggPos = new BlockPos(3, 3, 3);
            BlockPos absolute = helper.absolutePos(eggPos);
            helper.setBlock(eggPos.below(), Blocks.NETHERRACK);
            helper.setBlock(eggPos, Blocks.FIRE);
            egg.setPos(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5);
            helper.getLevel().addFreshEntity(egg);
            egg.setDragonAge(IafCommonConfig.INSTANCE.dragon.eggBornTime.getValue());
            egg.updateEggCondition();
            egg.updateEggCondition();
            var hatchlings = helper.getLevel().getEntitiesOfClass(DragonBaseEntity.class,
                new AABB(absolute).inflate(4));
            helper.assertTrue(hatchlings.size() == 1, "Fire egg did not hatch a forge dragon");
            DragonBaseEntity fireDragon = hatchlings.getFirst();
            fireDragon.setNoAi(true);
            fireDragon.setNoGravity(true);
            long now = Calendars.SERVER.getCalendarTicks();
            ((CalendarDragonGrowth) fireDragon).iaftfc$resetCalendar(now);
            ((CalendarDragonGrowth) fireDragon).iaftfc$advanceCalendar(now + 50 * ICalendar.CALENDAR_TICKS_IN_DAY);
            helper.assertTrue(fireDragon.getAgeInDays() >= 50, "Hatched dragon did not grow by calendar time");
            fireDragon.setPos(absolute.getX() + 0.5, 300, absolute.getZ() + 0.5);
            List<DragonBaseEntity> dragons = new ArrayList<>();
            dragons.add(fireDragon);
            for (int index = 1; index < ELEMENTS.length; index++) {
                DragonBaseEntity dragon = (DragonBaseEntity) BuiltInRegistries.ENTITY_TYPE.get(
                    ResourceLocation.parse("iceandfire:" + ELEMENTS[index] + "_dragon")).create(helper.getLevel());
                dragon.setAgeInDays(50);
                dragon.setNoAi(true);
                dragon.setNoGravity(true);
                BlockPos location = helper.absolutePos(new BlockPos(3 + index * 12, 300, 3));
                dragon.setPos(location.getX() + 0.5, 300, location.getZ() + 0.5);
                helper.getLevel().addFreshEntity(dragon);
                dragons.add(dragon);
            }
            produce(helper, 0, 0, new ArrayList<>(), dragons, scales, baseSteels);
        });
    }

    private static void produce(GameTestHelper helper, int index, int cycle, List<ItemStack> ingots,
        List<DragonBaseEntity> dragons, List<List<ItemStack>> scales, List<List<ItemStack>> baseSteels) {
        if (index == 3) {
            dragons.forEach(DragonBaseEntity::discard);
            helper.succeed();
            return;
        }
        if (cycle == 21) {
            finishMetal(helper, index, ingots);
            produce(helper, index + 1, 0, new ArrayList<>(), dragons, scales, baseSteels);
            return;
        }
        int x = 3 + index * 12;
        DragonForgeBlockEntity core = (DragonForgeBlockEntity) helper.getBlockEntity(new BlockPos(x, 3, 13));
        DragonForgeInputBlockEntity input = (DragonForgeInputBlockEntity) helper.getBlockEntity(new BlockPos(x, 3, 12));
        DragonForgeInputBlockEntity.tick(helper.getLevel(), input.getBlockPos(),
            helper.getLevel().getBlockState(input.getBlockPos()), input);
        helper.assertTrue(core.assembled(), "Forge core was not assembled: " + ELEMENTS[index]);
        core.setItem(0, scales.get(index).removeFirst());
        core.setItem(1, new ItemStack(Items.GLASS_BOTTLE));
        helper.assertTrue(core.canSmelt() && input.isAssembled(),
            "Scale and bottle did not activate the assembled forge");
        IafDragonDestructionManager.destroyAreaBreath(helper.getLevel(), input.getBlockPos(), dragons.get(index));
        breatheUntilCooked(input, dragons.get(index), 750);
        helper.runAfterDelay(2, () -> {
            ItemStack blood = core.getItem(2).copy();
            helper.assertTrue(blood.is(item("iceandfire:" + ELEMENTS[index] + "_dragon_blood").getItem()),
                "Forge did not yield blood for its own metal cycle");
            core.setItem(2, ItemStack.EMPTY);
            core.setItem(0, baseSteels.get(index).removeFirst());
            core.setItem(1, blood);
            helper.assertTrue(core.canSmelt(), "Blood and base steel did not enter Dragonsteel recipe");
            breatheUntilCooked(input, dragons.get(index), 1000);
            helper.runAfterDelay(2, () -> {
                ItemStack ingot = core.getItem(2).copy();
                helper.assertTrue(ingot.is(item("iaftfc:metal/ingot/dragonsteel_" + ELEMENTS[index]).getItem())
                    && ingot.getCount() == 1, "Forge yielded the wrong Dragonsteel");
                ingots.add(ingot);
                core.setItem(2, ItemStack.EMPTY);
                produce(helper, index, cycle + 1, ingots, dragons, scales, baseSteels);
            });
        });
    }

    private static void finishMetal(GameTestHelper helper, int index, List<ItemStack> ingots) {
        String metal = Dragonsteel.METALS[index];
        AnvilBlockEntity low = (AnvilBlockEntity) helper.getBlockEntity(new BlockPos(2, 1, 1));
        List<ItemStack> doubles = new ArrayList<>();
        for (int pair = 0; pair < 7; pair++) {
            ItemStack first = ingots.get(pair * 2);
            ItemStack second = ingots.get(pair * 2 + 1);
            HeatCapability.setStaticTemperature(first, 1600);
            HeatCapability.setStaticTemperature(second, 1600);
            low.getInventory().setStackInSlot(0, first);
            low.getInventory().setStackInSlot(1, second);
            low.getInventory().setStackInSlot(2, item("tfc:metal/hammer/steel"));
            low.getInventory().setStackInSlot(3, item("tfc:powder/flux"));
            helper.assertTrue(low.weld(FakePlayerFactory.getMinecraft(helper.getLevel())).consumesAction(),
                "Tier 6 failed to weld forge outputs");
            doubles.add(low.getInventory().getStackInSlot(0).copy());
        }
        ItemStack anvilStack = craft(helper, List.of(doubles.get(0), doubles.get(1), doubles.get(2),
            ItemStack.EMPTY, doubles.get(3), ItemStack.EMPTY, doubles.get(4), doubles.get(5), doubles.get(6)));
        helper.assertTrue(anvilStack.is(item("iaftfc:metal/anvil/" + metal).getItem()), "Wrong Tier 7 anvil");
        helper.setBlock(new BlockPos(1, 1, 1), ((net.minecraft.world.item.BlockItem) anvilStack.getItem()).getBlock());
        AnvilBlockEntity high = (AnvilBlockEntity) helper.getBlockEntity(new BlockPos(1, 1, 1));
        ItemStack last = ingots.get(14);
        HeatCapability.setStaticTemperature(last, 1600);
        high.getInventory().setStackInSlot(0, last);
        high.getInventory().setStackInSlot(2, item("tfc:metal/hammer/steel"));
        high.chooseRecipe(ResourceLocation.parse("iaftfc:anvil/metal/pickaxe_head/" + metal));
        int target = high.getMainInputForging().target();
        helper.assertTrue(high.workRemotely(ForgeStep.DRAW, target - ForgeStep.PUNCH.step(), true),
            "Tier 7 forging could not start");
        high.workRemotely(ForgeStep.BEND, 0, false);
        high.workRemotely(ForgeStep.DRAW, 0, false);
        high.work(FakePlayerFactory.getMinecraft(helper.getLevel()), ForgeStep.PUNCH);
        ItemStack head = high.getInventory().getStackInSlot(0).copy();
        helper.assertTrue(head.is(item("iaftfc:metal/pickaxe_head/" + metal).getItem()),
            "Forge output did not become a pickaxe head");
        ItemStack pickaxe = craft(helper, List.of(head, ItemStack.EMPTY, ItemStack.EMPTY,
            new ItemStack(Items.STICK), ItemStack.EMPTY, ItemStack.EMPTY,
            ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY));
        helper.assertTrue(pickaxe.is(item("iceandfire:" + metal + "_pickaxe").getItem()),
            "Forged head did not yield the original IaF pickaxe");
        List<ItemStack> armorDoubles = new ArrayList<>();
        for (int pair = 0; pair < 3; pair++) {
            ItemStack first = ingots.get(15 + pair * 2);
            ItemStack second = ingots.get(16 + pair * 2);
            HeatCapability.setTemperature(first, 1700);
            HeatCapability.setTemperature(second, 1700);
            low.getInventory().setStackInSlot(0, first);
            low.getInventory().setStackInSlot(1, second);
            low.getInventory().setStackInSlot(3, item("tfc:powder/flux"));
            helper.assertTrue(low.weld(FakePlayerFactory.getMinecraft(helper.getLevel())).consumesAction(),
                "Additional forge ingots did not weld for armor");
            armorDoubles.add(low.getInventory().getStackInSlot(0).copy());
        }
        List<ItemStack> sheets = new ArrayList<>();
        for (ItemStack welded : armorDoubles) {
            HeatCapability.setTemperature(welded, 1700);
            high.getInventory().setStackInSlot(0, welded);
            high.chooseRecipe(ResourceLocation.parse("iaftfc:anvil/metal/sheet/" + metal));
            int sheetTarget = high.getMainInputForging().target();
            helper.assertTrue(high.workRemotely(ForgeStep.DRAW, sheetTarget, true),
                "Sheet forging could not start");
            high.workRemotely(ForgeStep.HIT_LIGHT, 0, false);
            high.workRemotely(ForgeStep.HIT_LIGHT, 0, false);
            high.workRemotely(ForgeStep.HIT_LIGHT, 0, false);
            ItemStack sheet = high.getInventory().getStackInSlot(0).copy();
            helper.assertTrue(sheet.is(item("iaftfc:metal/sheet/" + metal).getItem()),
                "Double ingot did not become a sheet");
            sheets.add(sheet);
        }
        HeatCapability.setTemperature(sheets.get(0), 1700);
        HeatCapability.setTemperature(sheets.get(1), 1700);
        low.getInventory().setStackInSlot(0, sheets.get(0));
        low.getInventory().setStackInSlot(1, sheets.get(1));
        low.getInventory().setStackInSlot(3, item("tfc:powder/flux"));
        helper.assertTrue(low.weld(FakePlayerFactory.getMinecraft(helper.getLevel())).consumesAction(),
            "Sheets did not weld into a double sheet");
        ItemStack doubleSheet = low.getInventory().getStackInSlot(0).copy();
        high.getInventory().setStackInSlot(0, doubleSheet);
        high.chooseRecipe(ResourceLocation.parse("iaftfc:anvil/metal/unfinished_helmet/" + metal));
        helper.assertTrue(high.workRemotely(ForgeStep.DRAW, high.getMainInputForging().target(), true),
            "Helmet forging could not start");
        high.workRemotely(ForgeStep.BEND, 0, false);
        high.workRemotely(ForgeStep.BEND, 0, false);
        high.workRemotely(ForgeStep.HIT_LIGHT, 0, false);
        ItemStack unfinished = high.getInventory().getStackInSlot(0).copy();
        helper.assertTrue(unfinished.is(item("iaftfc:metal/unfinished_helmet/" + metal).getItem()),
            "Double sheet did not become an unfinished helmet");
        HeatCapability.setTemperature(unfinished, 1700);
        HeatCapability.setTemperature(sheets.get(2), 1700);
        low.getInventory().setStackInSlot(0, unfinished);
        low.getInventory().setStackInSlot(1, sheets.get(2));
        low.getInventory().setStackInSlot(3, item("tfc:powder/flux"));
        helper.assertTrue(low.weld(FakePlayerFactory.getMinecraft(helper.getLevel())).consumesAction()
            && low.getInventory().getStackInSlot(0).is(item("iceandfire:" + metal + "_helmet").getItem()),
            "Forge outputs did not become Dragonsteel player armor");
    }

    @GameTest(template = "empty")
    public static void noLegacyShortcuts(GameTestHelper helper) throws Exception {
        var manager = helper.getLevel().getRecipeManager();
        var registry = helper.getLevel().registryAccess();
        var mod = ModList.get().getModFileById("iaftfc").getFile();
        try (var reader = new InputStreamReader(ProgressionGameTests.class.getClassLoader()
            .getResourceAsStream("iaftfc/gametest/override_manifest.json"), StandardCharsets.UTF_8)) {
            var manifest = JsonParser.parseReader(reader).getAsJsonObject();
            for (var entry : manifest.getAsJsonArray("allowed_overrides")) {
                String path = entry.getAsString();
                if (!path.contains(":recipe/")) continue;
                var file = mod.findResource("data/" + path.replace(":", "/"));
                if (!Files.isRegularFile(file)) continue;
                try (var recipeReader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    var recipe = JsonParser.parseReader(recipeReader).getAsJsonObject();
                    if (recipe.has("neoforge:conditions") && recipe.getAsJsonArray("neoforge:conditions")
                        .toString().contains("neoforge:false")) {
                        String id = path.substring(0, path.indexOf(":recipe/")) + ":"
                            + path.substring(path.indexOf(":recipe/") + 8, path.length() - 5);
                        helper.assertTrue(manager.byKey(ResourceLocation.parse(id)).isEmpty(),
                            "Disabled override is still in RecipeManager: " + id);
                    }
                }
            }
        }
        List<ItemStack> forbidden = List.of(new ItemStack(Items.IRON_INGOT), new ItemStack(Items.GOLD_INGOT),
            new ItemStack(Items.DIAMOND), new ItemStack(Items.NETHERITE_INGOT), item("iceandfire:silver_ingot"),
            item("iceandfire:sapphire_gem"));
        for (var holder : manager.getRecipes()) {
            ItemStack result = holder.value().getResultItem(registry);
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(result.getItem());
            boolean progression = id.getPath().startsWith("dragonarmor_")
                || id.getPath().matches("dragonsteel_(fire|ice|lightning)_(sword|pickaxe|axe|shovel|hoe|helmet|chestplate|leggings|boots)")
                || id.getNamespace().equals("iaftfc") && id.getPath().startsWith("metal/");
            if (progression) for (var ingredient : holder.value().getIngredients()) for (ItemStack input : forbidden) {
                helper.assertTrue(!ingredient.test(input), "Forbidden metal or gem reaches progression: " + holder.id());
            }
            if (!id.getNamespace().equals("iceandfire")) continue;
            String path = id.getPath();
            helper.assertTrue(!path.matches("dragonsteel_(fire|ice|lightning)_(ingot|block)"),
                "A recipe still produces old IaF Dragonsteel: " + holder.id());
            helper.assertTrue(!path.matches("dragonarmor_(copper|silver|gold|diamond|netherite)_(head|neck|body|tail)"),
                "A recipe still produces disabled Dragon Armor: " + holder.id());
        }
        for (String element : ELEMENTS) {
            CraftingInput oldSword = CraftingInput.of(1, 3, List.of(
                item("iceandfire:dragonsteel_" + element + "_ingot"),
                item("iceandfire:dragonsteel_" + element + "_ingot"), new ItemStack(Items.STICK)));
            var result = manager.getRecipeFor(RecipeType.CRAFTING, oldSword, helper.getLevel())
                .map(holder -> holder.value().assemble(oldSword, registry)).orElse(ItemStack.EMPTY);
            helper.assertTrue(!result.is(item("iceandfire:dragonsteel_" + element + "_sword").getItem()),
                "Old ingots still craft a completed Dragonsteel sword");
            CraftingInput oldBlock = CraftingInput.of(1, 1, List.of(
                item("iceandfire:dragonsteel_" + element + "_block")));
            result = manager.getRecipeFor(RecipeType.CRAFTING, oldBlock, helper.getLevel())
                .map(holder -> holder.value().assemble(oldBlock, registry)).orElse(ItemStack.EMPTY);
            helper.assertTrue(!result.is(item("iceandfire:dragonsteel_" + element + "_ingot").getItem()),
                "Old IaF block still reverses to an ingot");
            ItemStack menuResult = craft(helper, List.of(item("iceandfire:dragonsteel_" + element + "_ingot"),
                ItemStack.EMPTY, ItemStack.EMPTY, item("iceandfire:dragonsteel_" + element + "_ingot"),
                ItemStack.EMPTY, ItemStack.EMPTY, new ItemStack(Items.STICK), ItemStack.EMPTY, ItemStack.EMPTY));
            helper.assertTrue(!menuResult.is(item("iceandfire:dragonsteel_" + element + "_sword").getItem()),
                "Old ingot shortcut worked in the crafting menu");
            menuResult = craft(helper, List.of(item("iceandfire:dragonsteel_" + element + "_block"),
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY));
            helper.assertTrue(!menuResult.is(item("iceandfire:dragonsteel_" + element + "_ingot").getItem()),
                "Old block reverse recipe worked in the crafting menu");
        }
        for (String material : List.of("copper", "silver", "gold", "diamond")) {
            String block = switch (material) {
                case "copper" -> "minecraft:copper_block";
                case "silver" -> "iceandfire:silver_block";
                case "gold" -> "minecraft:gold_block";
                default -> "minecraft:diamond_block";
            };
            ItemStack old = item(block);
            ItemStack result = craft(helper, List.of(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                ItemStack.EMPTY, old, old, old, old, old));
            helper.assertTrue(!result.is(item("iceandfire:dragonarmor_" + material + "_head").getItem()),
                "Disabled Dragon Armor worked in the crafting menu: " + material);
        }
        for (String id : List.of("iceandfire:dragon_horn", "iceandfire:dragon_flute",
            "iceandfire:dragon_meal", "iceandfire:dragonforge_fire_brick")) {
            helper.assertTrue(manager.byKey(ResourceLocation.parse(id)).isPresent(),
                "Unrelated dragon recipe was disabled: " + id);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void legacyWorkstationsAndLoot(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 1, 1), Dragonsteel.METAL_PARTS.get("dragonsteel_fire").anvil().get());
        AnvilBlockEntity anvil = (AnvilBlockEntity) helper.getBlockEntity(new BlockPos(1, 1, 1));
        for (String element : ELEMENTS) {
            ItemStack old = item("iceandfire:dragonsteel_" + element + "_ingot");
            anvil.getInventory().setStackInSlot(0, old.copy());
            anvil.getInventory().setStackInSlot(1, old.copy());
            anvil.getInventory().setStackInSlot(3, item("tfc:powder/flux"));
            helper.assertTrue(!anvil.canWeldNow()
                && !anvil.weld(FakePlayerFactory.getMinecraft(helper.getLevel())).consumesAction(),
                "Old IaF ingots welded into Dragonsteel: " + element);
            anvil.getInventory().setStackInSlot(1, ItemStack.EMPTY);
            anvil.chooseRecipe(ResourceLocation.parse("iaftfc:anvil/metal/pickaxe_head/dragonsteel_fire"));
            helper.assertTrue(!anvil.workRemotely(ForgeStep.DRAW, 1, true),
                "Old IaF ingot entered the Dragonsteel anvil recipe");
        }
        IMold mold = IMold.get(item("tfc:ceramic/ingot_mold"));
        helper.assertTrue(mold != null && mold.fill(new FluidStack(BuiltInRegistries.FLUID.get(
            ResourceLocation.parse("tfc:metal/wrought_iron")), 100), IFluidHandler.FluidAction.EXECUTE) == 100,
            "Iron mold could not be prepared");
        mold.setTemperature(20);
        CastingRecipe casting = CastingRecipe.get(mold);
        helper.assertTrue(casting != null && casting.matches(mold)
            && !BuiltInRegistries.ITEM.getKey(casting.assemble(mold).getItem()).getPath().startsWith("dragonsteel_"),
            "Iron mold cast an old IaF Dragonsteel ingot");
        var params = new LootParams.Builder(helper.getLevel()).withParameter(LootContextParams.ORIGIN,
            new Vec3(0.5, 80.5, 0.5)).create(LootContextParamSets.CHEST);
        for (String element : ELEMENTS) for (String place : List.of("roost", "female_cave", "male_cave")) {
            var table = helper.getLevel().getServer().reloadableRegistries().getLootTable(ResourceKey.create(
                Registries.LOOT_TABLE, ResourceLocation.parse("iceandfire:chest/" + element + "_dragon_" + place)));
            for (long seed = 0; seed < 64; seed++) for (ItemStack drop : table.getRandomItems(params, seed)) {
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(drop.getItem());
                helper.assertTrue(!id.getPath().matches("dragonsteel_(fire|ice|lightning)_(ingot|block)")
                    && !id.getPath().matches("dragonarmor_(copper|silver|gold|diamond|netherite)_(head|neck|body|tail)")
                    && !id.toString().matches("iceandfire:(silver_ingot|sapphire_gem)"),
                    "Legacy shortcut remains in dragon loot: " + id);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void allEquipmentReachable(GameTestHelper helper) {
        var manager = helper.getLevel().getRecipeManager();
        var registry = helper.getLevel().registryAccess();
        String[] materials = {"iron", "steel", "black_steel", "red_steel", "blue_steel",
            "dragon_steel_fire", "dragon_steel_ice", "dragon_steel_lightning"};
        String[][] shapes = {{"   ", " ##", "###"}, {"   ", "###", " ##"},
            {"###", "###", "# #"}, {"   ", "  #", "## "}};
        String[] armorParts = {"head", "neck", "body", "tail"};
        for (String material : materials) {
            String metal = material.equals("iron") ? "wrought_iron" : material;
            boolean dragonsteel = material.startsWith("dragon_steel_");
            if (dragonsteel) metal = "dragonsteel_" + material.substring("dragon_steel_".length());
            String namespace = dragonsteel ? "iaftfc" : "tfc";
            WeldingRecipe doubleIngot = (WeldingRecipe) manager.byKey(ResourceLocation.parse(
                namespace + ":welding/metal/double_ingot/" + metal)).orElseThrow().value();
            ItemStack sourceIngot = item(namespace + ":metal/ingot/" + metal);
            helper.assertTrue(doubleIngot.getFirstInput().test(sourceIngot)
                && doubleIngot.getSecondInput().test(sourceIngot),
                "Base ingot cannot reach a double ingot: " + metal);
            AnvilRecipe sheet = (AnvilRecipe) manager.byKey(ResourceLocation.parse(
                namespace + ":anvil/metal/sheet/" + metal)).orElseThrow().value();
            helper.assertTrue(sheet.getInput().test(doubleIngot.getResultItem(registry))
                && sheet.getResultItem(registry).is(item(namespace + ":metal/sheet/" + metal).getItem()),
                "Sheet route is missing for " + metal);
            WeldingRecipe doubleSheet = (WeldingRecipe) manager.byKey(ResourceLocation.parse(
                namespace + ":welding/metal/double_sheet/" + metal)).orElseThrow().value();
            helper.assertTrue(doubleSheet.getFirstInput().test(sheet.getResultItem(registry))
                && doubleSheet.getSecondInput().test(sheet.getResultItem(registry)),
                "Double sheet does not accept reachable sheets: " + metal);
            ItemStack doubleSheetItem = doubleSheet.getResultItem(registry);
            helper.assertTrue(doubleSheetItem.is(item(namespace + ":metal/double_sheet/" + metal).getItem()),
                "Wrong double sheet output for " + metal);
            for (int part = 0; part < armorParts.length; part++) {
                String id = (material.equals("iron") || dragonsteel ? "iceandfire:" : "iaftfc:")
                    + "dragonarmor_" + material + "_" + armorParts[part];
                @SuppressWarnings("unchecked")
                Recipe<CraftingInput> recipe = (Recipe<CraftingInput>) manager.byKey(
                    ResourceLocation.parse(id)).orElseThrow().value();
                List<ItemStack> slots = new ArrayList<>();
                for (String row : shapes[part]) for (char cell : row.toCharArray()) {
                    slots.add(cell == '#' ? doubleSheetItem.copy() : ItemStack.EMPTY);
                }
                CraftingInput input = CraftingInput.of(3, 3, slots);
                helper.assertTrue(recipe.matches(input, helper.getLevel())
                    && recipe.assemble(input, registry).is(item(id).getItem()),
                    "Valid Dragon Armor lacks a reachable sheet route: " + id);
            }
        }
        for (int index = 0; index < ELEMENTS.length; index++) {
            String metal = Dragonsteel.METALS[index];
            AnvilRecipe sheet = (AnvilRecipe) manager.byKey(ResourceLocation.parse(
                "iaftfc:anvil/metal/sheet/" + metal)).orElseThrow().value();
            WeldingRecipe doubleSheet = (WeldingRecipe) manager.byKey(ResourceLocation.parse(
                "iaftfc:welding/metal/double_sheet/" + metal)).orElseThrow().value();
            var forge = manager.byKey(ResourceLocation.parse("iceandfire:dragonforge/dragonsteel_"
                + ELEMENTS[index] + "_ingot")).orElseThrow().value();
            ItemStack ingot = forge.getResultItem(registry);
            helper.assertTrue(ingot.is(item("iaftfc:metal/ingot/" + metal).getItem()),
                "Dragon Forge does not feed the metal route: " + metal);
            WeldingRecipe doubleIngot = (WeldingRecipe) manager.byKey(ResourceLocation.parse(
                "iaftfc:welding/metal/double_ingot/" + metal)).orElseThrow().value();
            helper.assertTrue(doubleIngot.getFirstInput().test(ingot) && doubleIngot.getSecondInput().test(ingot)
                && doubleIngot.getTier() == 6, "Forge ingot cannot reach the Tier 7 anvil: " + metal);
            ItemStack welded = doubleIngot.getResultItem(registry);
            @SuppressWarnings("unchecked")
            Recipe<CraftingInput> anvil = (Recipe<CraftingInput>) manager.byKey(ResourceLocation.parse(
                "iaftfc:crafting/metal/anvil/" + metal)).orElseThrow().value();
            CraftingInput seven = CraftingInput.of(3, 3, List.of(welded.copy(), welded.copy(), welded.copy(),
                ItemStack.EMPTY, welded.copy(), ItemStack.EMPTY, welded.copy(), welded.copy(), welded.copy()));
            helper.assertTrue(anvil.matches(seven, helper.getLevel())
                && anvil.assemble(seven, registry).is(item("iaftfc:metal/anvil/" + metal).getItem()),
                "Seven forged double ingots cannot become a Tier 7 anvil: " + metal);
            for (String tool : List.of("sword", "pickaxe", "axe", "shovel", "hoe")) {
                String part = tool.equals("sword") ? "sword_blade" : tool + "_head";
                AnvilRecipe head = (AnvilRecipe) manager.byKey(ResourceLocation.parse(
                    "iaftfc:anvil/metal/" + part + "/" + metal)).orElseThrow().value();
                helper.assertTrue(head.getInput().test(tool.equals("sword") ? welded : ingot)
                    && head.isCorrectTier(7), "Forge input cannot reach " + part + " " + metal);
                ItemStack madeHead = head.getResultItem(registry);
                @SuppressWarnings("unchecked")
                Recipe<CraftingInput> assembly = (Recipe<CraftingInput>) manager.byKey(ResourceLocation.parse(
                    "iaftfc:crafting/metal/" + tool + "/" + metal)).orElseThrow().value();
                CraftingInput pair = CraftingInput.of(1, 2, List.of(madeHead, new ItemStack(Items.STICK)));
                helper.assertTrue(assembly.matches(pair, helper.getLevel())
                    && assembly.assemble(pair, registry).is(item("iceandfire:" + metal + "_" + tool).getItem()),
                    "Reachable head cannot assemble a finished tool: " + metal + " " + tool);
            }
            for (String part : List.of("helmet", "chestplate", "greaves", "boots")) {
                AnvilRecipe unfinished = (AnvilRecipe) manager.byKey(ResourceLocation.parse(
                    "iaftfc:anvil/metal/unfinished_" + part + "/" + metal)).orElseThrow().value();
                ItemStack sheetInput = part.equals("boots") ? sheet.getResultItem(registry)
                    : doubleSheet.getResultItem(registry);
                helper.assertTrue(unfinished.getInput().test(sheetInput) && unfinished.isCorrectTier(7),
                    "Sheet cannot reach unfinished armor: " + metal + " " + part);
                WeldingRecipe finished = (WeldingRecipe) manager.byKey(ResourceLocation.parse(
                    "iaftfc:welding/metal/" + part + "/" + metal)).orElseThrow().value();
                helper.assertTrue(finished.getFirstInput().test(unfinished.getResultItem(registry))
                    && finished.getSecondInput().test(part.equals("chestplate")
                        ? doubleSheet.getResultItem(registry) : sheet.getResultItem(registry))
                    && finished.getTier() == 6, "Unfinished armor cannot reach welding: " + metal + " " + part);
                String output = part.equals("greaves") ? "leggings" : part;
                helper.assertTrue(finished.getResultItem(registry)
                    .is(item("iceandfire:" + metal + "_" + output).getItem()),
                    "Welding does not finish the expected armor: " + metal + " " + part);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void equipmentReachableFromSources(GameTestHelper helper) {
        var level = helper.getLevel();
        var manager = level.getRecipeManager();
        var registry = level.registryAccess();
        Set<Item> reached = new HashSet<>();
        var lootParams = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN,
            new Vec3(0.5, 80.5, 0.5)).create(LootContextParamSets.CHEST);
        for (String element : ELEMENTS) for (String place : List.of("roost", "female_cave", "male_cave")) {
            var table = level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(
                Registries.LOOT_TABLE, ResourceLocation.parse("iceandfire:chest/" + element + "_dragon_" + place)));
            for (long seed = 0; seed < 256; seed++) {
                for (ItemStack drop : table.getRandomItems(lootParams, seed)) reached.add(drop.getItem());
            }
        }
        for (String id : List.of("iceandfire:dragonbone", "minecraft:stick", "minecraft:glass_bottle",
            "tfc:metal/ingot/wrought_iron", "tfc:metal/ingot/steel", "tfc:metal/ingot/black_steel",
            "tfc:metal/ingot/red_steel", "tfc:metal/ingot/blue_steel", "tfc:metal/anvil/red_steel")) {
            reached.add(item(id).getItem());
        }
        for (String color : List.of("red", "green", "bronze", "gray", "blue", "white", "sapphire",
            "silver", "electric", "amethyst", "copper", "black")) {
            reached.add(item("iceandfire:dragonscales_" + color).getItem());
        }
        boolean changed;
        do {
            changed = false;
            for (var holder : manager.getRecipes()) {
                var recipe = holder.value();
                ItemStack output = recipe.getResultItem(registry);
                if (output.isEmpty() || reached.contains(output.getItem())) continue;
                boolean available;
                if (recipe instanceof DragonForgeRecipe forge) {
                    available = reached.stream().anyMatch(source -> forge.getInput().test(new ItemStack(source)))
                        && reached.stream().anyMatch(source -> forge.getBlood().test(new ItemStack(source)));
                } else if (recipe instanceof WeldingRecipe weld) {
                    available = reached.stream().anyMatch(source -> weld.getFirstInput().test(new ItemStack(source)))
                        && reached.stream().anyMatch(source -> weld.getSecondInput().test(new ItemStack(source)));
                } else if (recipe instanceof AnvilRecipe anvil) {
                    available = reached.stream().anyMatch(source -> anvil.getInput().test(new ItemStack(source)));
                    if (anvil.isCorrectTier(7) && holder.id().getNamespace().equals("iaftfc")) {
                        available &= reached.contains(item("iaftfc:metal/anvil/"
                            + holder.id().getPath().substring(holder.id().getPath().lastIndexOf('/') + 1)).getItem());
                    }
                } else {
                    available = !recipe.getIngredients().isEmpty() && recipe.getIngredients().stream().allMatch(
                        ingredient -> ingredient.isEmpty() || reached.stream()
                            .anyMatch(source -> ingredient.test(new ItemStack(source))));
                }
                if (available) changed |= reached.add(output.getItem());
            }
        } while (changed);
        for (String metal : Dragonsteel.METALS) {
            for (String part : List.of("sword", "pickaxe", "axe", "shovel", "hoe", "helmet",
                "chestplate", "leggings", "boots")) {
                String id = "iceandfire:" + metal + "_" + part;
                helper.assertTrue(reached.contains(item(id).getItem()), "Unreachable equipment: " + id
                    + ", reached=" + reached.stream().map(BuiltInRegistries.ITEM::getKey)
                        .filter(key -> key.toString().contains(metal)).sorted().toList());
            }
        }
        for (String material : List.of("iron", "steel", "black_steel", "red_steel", "blue_steel",
            "dragon_steel_fire", "dragon_steel_ice", "dragon_steel_lightning")) {
            for (String part : List.of("head", "neck", "body", "tail")) {
                String id = (material.equals("iron") || material.startsWith("dragon_steel_")
                    ? "iceandfire:" : "iaftfc:") + "dragonarmor_" + material + "_" + part;
                helper.assertTrue(reached.contains(item(id).getItem()), "Unreachable Dragon Armor: " + id);
            }
        }
        helper.succeed();
    }

    private static ItemStack item(String id) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
    }

    private static void breatheUntilCooked(DragonForgeInputBlockEntity input, DragonBaseEntity dragon,
        int cookTime) {
        double speed = dragon.getAttributeValue(IafAttributes.DRAGON_FORGE_SPEED);
        for (int pulse = 0; pulse < (int) Math.ceil((cookTime + 1) / speed); pulse++) {
            input.onHitWithFlame(dragon);
        }
    }

    private static List<List<ItemStack>> prepareTfcSteels(GameTestHelper helper) {
        // Ordinary TFC metallurgy supplies the steel anvil, hammer, flux and weak alloy ingots before dragon progression.
        helper.setBlock(new BlockPos(2, 1, 1), BuiltInRegistries.BLOCK.get(ResourceLocation.parse(
            "tfc:metal/anvil/steel")));
        AnvilBlockEntity anvil = (AnvilBlockEntity) helper.getBlockEntity(new BlockPos(2, 1, 1));
        List<ItemStack> black = new ArrayList<>();
        for (int index = 0; index < 91; index++) {
            black.add(makeTfcSteel(helper, anvil, "black_steel",
                item("tfc:metal/ingot/weak_steel"), item("tfc:metal/ingot/pig_iron")));
        }
        anvil = craftTfcAnvil(helper, anvil, "black_steel", black.subList(0, 14));
        List<ItemStack> red = new ArrayList<>();
        List<ItemStack> blue = new ArrayList<>();
        for (int index = 0; index < 35; index++) {
            red.add(makeTfcSteel(helper, anvil, "red_steel",
                item("tfc:metal/ingot/weak_red_steel"), black.get(index + 14)));
        }
        for (int index = 0; index < 21; index++) {
            blue.add(makeTfcSteel(helper, anvil, "blue_steel",
                item("tfc:metal/ingot/weak_blue_steel"), black.get(index + 49)));
        }
        craftTfcAnvil(helper, anvil, "red_steel", red.subList(0, 14));
        return List.of(new ArrayList<>(red.subList(14, 35)), blue,
            new ArrayList<>(black.subList(70, 91)));
    }

    private static AnvilBlockEntity craftTfcAnvil(GameTestHelper helper, AnvilBlockEntity anvil,
        String metal, List<ItemStack> ingots) {
        List<ItemStack> doubles = new ArrayList<>();
        for (int pair = 0; pair < 7; pair++) {
            ItemStack first = ingots.get(pair * 2);
            ItemStack second = ingots.get(pair * 2 + 1);
            HeatCapability.setTemperature(first, 1300);
            HeatCapability.setTemperature(second, 1300);
            anvil.getInventory().setStackInSlot(0, first);
            anvil.getInventory().setStackInSlot(1, second);
            anvil.getInventory().setStackInSlot(3, item("tfc:powder/flux"));
            helper.assertTrue(anvil.weld(FakePlayerFactory.getMinecraft(helper.getLevel())).consumesAction(),
                "TFC " + metal + " anvil double ingot welding failed");
            doubles.add(anvil.getInventory().getStackInSlot(0).copy());
        }
        ItemStack block = craft(helper, List.of(doubles.get(0), doubles.get(1), doubles.get(2),
            ItemStack.EMPTY, doubles.get(3), ItemStack.EMPTY, doubles.get(4), doubles.get(5), doubles.get(6)));
        helper.assertTrue(block.is(item("tfc:metal/anvil/" + metal).getItem()),
            "TFC double ingots did not craft a " + metal + " anvil");
        helper.setBlock(new BlockPos(2, 1, 1), ((net.minecraft.world.item.BlockItem) block.getItem()).getBlock());
        return (AnvilBlockEntity) helper.getBlockEntity(new BlockPos(2, 1, 1));
    }

    private static ItemStack makeTfcSteel(GameTestHelper helper, AnvilBlockEntity anvil, String metal,
        ItemStack first, ItemStack second) {
        HeatCapability.setTemperature(first, 1300);
        HeatCapability.setTemperature(second, 1300);
        anvil.getInventory().setStackInSlot(0, first);
        anvil.getInventory().setStackInSlot(1, second);
        anvil.getInventory().setStackInSlot(2, item("tfc:metal/hammer/steel"));
        anvil.getInventory().setStackInSlot(3, item("tfc:powder/flux"));
        helper.assertTrue(anvil.weld(FakePlayerFactory.getMinecraft(helper.getLevel())).consumesAction(),
            "TFC high-carbon " + metal + " welding failed");
        ItemStack highCarbon = anvil.getInventory().getStackInSlot(0);
        helper.assertTrue(highCarbon.is(item("tfc:metal/ingot/high_carbon_" + metal).getItem()),
            "TFC welding yielded the wrong high-carbon steel");
        HeatCapability.setTemperature(highCarbon, 1300);
        anvil.chooseRecipe(ResourceLocation.parse("tfc:anvil/metal/ingot/" + metal));
        helper.assertTrue(anvil.workRemotely(ForgeStep.DRAW, anvil.getMainInputForging().target(), true),
            "TFC steel anvil work could not start");
        for (int hit = 0; hit < 3; hit++) anvil.workRemotely(ForgeStep.HIT_LIGHT, 0, false);
        ItemStack steel = anvil.getInventory().getStackInSlot(0).copy();
        helper.assertTrue(steel.is(item("tfc:metal/ingot/" + metal).getItem()),
            "TFC high-carbon steel did not become base steel: " + metal);
        return steel;
    }

    private static ItemStack boneFromSkeleton(GameTestHelper helper) {
        var level = helper.getLevel();
        var preset = level.registryAccess().registryOrThrow(Registries.WORLD_PRESET)
            .get(ResourceLocation.parse("tfc:overworld"));
        var generator = (TFCChunkGenerator) preset.overworld().orElseThrow().generator();
        var configured = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE);
        ConfiguredFeature<?, ?> feature = configured.get(ResourceLocation.parse("iceandfire:spawn_dragon_skeleton_fire"));
        RandomSource certain = (RandomSource) Proxy.newProxyInstance(RandomSource.class.getClassLoader(),
            new Class<?>[] {RandomSource.class}, (proxy, method, args) -> switch (method.getName()) {
                case "nextDouble" -> 0.0;
                case "nextInt" -> 0;
                case "nextLong" -> 0L;
                case "nextFloat" -> 0f;
                case "nextBoolean" -> false;
                default -> throw new AssertionError("Unexpected skeleton RNG call: " + method.getName());
            });
        BlockPos origin = helper.absolutePos(new BlockPos(40, 80, 0));
        level.setBlock(origin.offset(8, -1, 8), Blocks.STONE.defaultBlockState(), 3);
        feature.place(level, generator, certain, origin);
        var skeletons = level.getEntitiesOfClass(DragonBaseEntity.class,
            AABB.encapsulatingFullBlocks(origin.offset(-2, -64, -2), origin.offset(18, 320, 18)));
        DragonBaseEntity skeleton = skeletons.stream().filter(DragonBaseEntity::isModelDead)
            .findFirst().orElseThrow();
        skeleton.setAgeInTicks(2400000);
        skeleton.setDeathStage(1);
        var player = FakePlayerFactory.getMinecraft(level);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        for (int stage = 0; stage < 32 && !skeleton.isRemoved(); stage++) {
            skeleton.mobInteract(player, InteractionHand.MAIN_HAND);
        }
        var drops = level.getEntitiesOfClass(ItemEntity.class, skeleton.getBoundingBox().inflate(8));
        ItemEntity bone = drops.stream().filter(drop -> drop.getItem().is(item("iceandfire:dragonbone").getItem()))
            .findFirst().orElseThrow();
        ItemStack recovered = bone.getItem().split(1);
        skeleton.discard();
        return recovered;
    }

    private static List<ItemStack> dragonScalesFromLoot(GameTestHelper helper, int index, int count) {
        var level = helper.getLevel();
        DragonBaseEntity source = (DragonBaseEntity) BuiltInRegistries.ENTITY_TYPE.get(
            ResourceLocation.parse("iceandfire:" + ELEMENTS[index] + "_dragon")).create(level);
        source.setAgeInDays(50);
        source.setVariant(SCALE_COLORS[index]);
        var table = level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(
            Registries.LOOT_TABLE, ResourceLocation.parse("iceandfire:entities/dragon/"
                + ELEMENTS[index] + "_dragon_female")));
        var params = new LootParams.Builder(level)
            .withParameter(LootContextParams.THIS_ENTITY, source)
            .withParameter(LootContextParams.ORIGIN, source.position())
            .withParameter(LootContextParams.DAMAGE_SOURCE, level.damageSources().generic())
            .create(LootContextParamSets.ENTITY);
        List<ItemStack> scales = new ArrayList<>();
        for (long seed = 0; seed < 256 && scales.size() < count; seed++) {
            for (ItemStack drop : table.getRandomItems(params, seed)) {
                if (!drop.is(item("iceandfire:dragonscales_" + SCALE_COLORS[index]).getItem())) continue;
                while (!drop.isEmpty() && scales.size() < count) scales.add(drop.split(1));
            }
        }
        helper.assertTrue(scales.size() == count, "Dragon death loot did not supply enough scales");
        return scales;
    }

    private static ItemStack craft(GameTestHelper helper, List<ItemStack> grid) {
        var player = FakePlayerFactory.getMinecraft(helper.getLevel());
        var menu = new CraftingMenu(0, player.getInventory(),
            ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(new BlockPos(0, 1, 0))));
        for (int slot = 0; slot < 9; slot++) menu.getSlot(slot + 1).set(grid.get(slot).copy());
        ItemStack result = menu.getSlot(0).getItem().copy();
        if (!result.isEmpty()) menu.getSlot(0).onTake(player, result);
        menu.removed(player);
        return result;
    }
}
