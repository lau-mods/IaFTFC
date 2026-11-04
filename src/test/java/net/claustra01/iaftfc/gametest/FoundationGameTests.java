package net.claustra01.iaftfc.gametest;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("iaftfc")
@PrefixGameTestTemplate(false)
public final class FoundationGameTests {
    @GameTest(template = "empty")
    public static void registeredTranslations(GameTestHelper helper) throws Exception {
        JsonObject english;
        JsonObject japanese;
        try (var reader = new InputStreamReader(FoundationGameTests.class.getClassLoader()
            .getResourceAsStream("assets/iaftfc/lang/en_us.json"), StandardCharsets.UTF_8)) {
            english = JsonParser.parseReader(reader).getAsJsonObject();
        }
        try (var reader = new InputStreamReader(FoundationGameTests.class.getClassLoader()
            .getResourceAsStream("assets/iaftfc/lang/ja_jp.json"), StandardCharsets.UTF_8)) {
            japanese = JsonParser.parseReader(reader).getAsJsonObject();
        }
        for (var item : BuiltInRegistries.ITEM) {
            if (BuiltInRegistries.ITEM.getKey(item).getNamespace().equals("iaftfc")) {
                String key = item.getDescriptionId();
                helper.assertTrue(english.has(key) && japanese.has(key), "Missing item translation: " + key);
            }
        }
        for (var block : BuiltInRegistries.BLOCK) {
            if (BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals("iaftfc")) {
                String key = block.getDescriptionId();
                helper.assertTrue(english.has(key) && japanese.has(key), "Missing block translation: " + key);
            }
        }
        for (var fluid : BuiltInRegistries.FLUID) {
            if (BuiltInRegistries.FLUID.getKey(fluid).getNamespace().equals("iaftfc")) {
                String key = fluid.getFluidType().getDescriptionId();
                helper.assertTrue(english.has(key) && japanese.has(key), "Missing fluid translation: " + key);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void shippedModelTextures(GameTestHelper helper) throws Exception {
        var mod = ModList.get().getModFileById("iaftfc").getFile();
        for (String metal : List.of("steel", "black_steel", "red_steel", "blue_steel")) {
            for (String part : List.of("head", "neck", "body", "tail")) {
                String suffix = "dragonarmor_" + metal + "_" + part + ".png";
                helper.assertTrue(Files.isRegularFile(mod.findResource("assets/iaftfc/textures/item/" + suffix)),
                    "Missing Dragon Armor item sprite: " + suffix);
                helper.assertTrue(Files.isRegularFile(mod.findResource(
                    "assets/iceandfire/textures/entity/dragon_armor/armor_" + part + "_" + metal + ".png")),
                    "Missing Dragon Armor overlay: " + metal + " " + part);
            }
        }
        var assets = mod.findResource("assets");
        try (var paths = Files.walk(assets)) {
            for (var model : paths.filter(path -> Files.isRegularFile(path)
                && path.toString().endsWith(".json") && path.toString().contains("/models/")).toList()) {
                JsonObject json;
                try (var reader = Files.newBufferedReader(model, StandardCharsets.UTF_8)) {
                    json = JsonParser.parseReader(reader).getAsJsonObject();
                }
                if (!json.has("textures")) continue;
                String modelNamespace = assets.relativize(model).getName(0).toString();
                for (var texture : json.getAsJsonObject("textures").entrySet()) {
                    String value = texture.getValue().getAsString();
                    if (value.startsWith("#")) continue;
                    ResourceLocation id = value.contains(":") ? ResourceLocation.parse(value)
                        : ResourceLocation.fromNamespaceAndPath(modelNamespace, value);
                    String path = "assets/" + id.getNamespace() + "/textures/" + id.getPath() + ".png";
                    boolean supplied = Files.isRegularFile(mod.findResource(path));
                    var dependency = ModList.get().getModFileById(id.getNamespace());
                    supplied |= dependency != null && Files.isRegularFile(dependency.getFile().findResource(path));
                    helper.assertTrue(supplied || id.getNamespace().equals("minecraft"),
                        "Missing model texture: " + value + " in " + model);
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void requiredMods(GameTestHelper helper) {
        checkVersion(helper, "neoforge", "21.1.248");
        checkVersion(helper, "tfc", "4.2.12");
        checkVersion(helper, "iceandfire", "2.1.4");
        helper.assertTrue(ModList.get().isLoaded("iaftfc"), "IaFTFC must be loaded");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void onlyAllowedOverrides(GameTestHelper helper) throws Exception {
        JsonObject manifest;
        try (var reader = new InputStreamReader(FoundationGameTests.class.getClassLoader()
            .getResourceAsStream("iaftfc/gametest/override_manifest.json"), StandardCharsets.UTF_8)) {
            manifest = JsonParser.parseReader(reader).getAsJsonObject();
        }
        Set<ResourceLocation> allowed = new HashSet<>();
        for (var entry : manifest.getAsJsonArray("allowed_overrides")) {
            allowed.add(ResourceLocation.parse(entry.getAsString()));
        }
        Set<ResourceLocation> actual = new HashSet<>();
        var data = ModList.get().getModFileById("iaftfc").getFile().findResource("data");
        if (Files.exists(data)) {
            try (var paths = Files.walk(data)) {
                paths.filter(Files::isRegularFile).forEach(path -> {
                    var relative = data.relativize(path);
                    String namespace = relative.getName(0).toString();
                    String name = relative.subpath(1, relative.getNameCount()).toString().replace('\\', '/');
                    if ((!namespace.equals("iaftfc") && !namespace.equals("iaftfc_fixture")
                        && !namespace.equals("iaftfc_verify")) || name.startsWith("neoforge/biome_modifier/")
                        || name.startsWith("neoforge/loot_modifier/")
                        || name.startsWith("neoforge/loot_modifiers/")) {
                        actual.add(ResourceLocation.fromNamespaceAndPath(namespace, name));
                    }
                });
            }
        }
        helper.assertTrue(actual.equals(allowed), "Data overrides differ from manifest: actual=" + actual + ", allowed=" + allowed);

        var resources = helper.getLevel().getServer().getResourceManager();
        String iafPack = resources.getResource(ResourceLocation.parse("iceandfire:recipe/dragon_horn.json"))
            .orElseThrow().sourcePackId();
        helper.assertTrue(iafPack.equals("mod/iceandfire"), "IaF base recipe is not supplied by IaF: " + iafPack);
        for (String name : List.of("fire_dragon", "ice_dragon", "lightning_dragon", "hippogryph", "cockatrice")) {
            helper.assertTrue(BuiltInRegistries.ENTITY_TYPE.containsKey(ResourceLocation.fromNamespaceAndPath("iceandfire", name)),
                "Missing IaF entity: " + name);
        }
        for (String path : List.of(
            "recipe/diamond_hippogryph_armor.json", "recipe/cockatrice_scepter.json",
            "loot_table/entities/hippogryph.json", "loot_table/entities/cockatrice.json",
            "neoforge/biome_modifier/hippogryphs.json", "neoforge/biome_modifier/cockatrice.json",
            "worldgen/structure/cyclops_cave.json", "worldgen/structure/gorgon_temple.json",
            "worldgen/structure_set/cyclops_cave.json", "worldgen/structure_set/gorgon_temple.json",
            "recipe/dragon_horn.json",
            "worldgen/structure_set/dragon_cave.json", "worldgen/structure_set/dragon_roost.json")) {
            assertIafResource(helper, iafPack, path);
        }
        var structures = helper.getLevel().registryAccess().registryOrThrow(Registries.STRUCTURE);
        for (String path : List.of("cyclops_cave", "gorgon_temple")) {
            helper.assertTrue(structures.containsKey(ResourceLocation.fromNamespaceAndPath("iceandfire", path)),
                "Missing IaF structure: " + path);
        }
        for (String element : List.of("fire", "ice", "lightning")) {
            for (String place : List.of("cave", "roost")) {
                String structure = element + "_dragon_" + place;
                helper.assertTrue(structures.containsKey(ResourceLocation.fromNamespaceAndPath("iceandfire", structure)),
                    "Missing IaF dragon structure: " + structure);
                assertIafResource(helper, iafPack, "worldgen/structure/" + structure + ".json");
            }
            for (String variant : List.of("female", "male", "skeleton")) {
                assertIafResource(helper, iafPack, "loot_table/entities/dragon/" + element + "_dragon_" + variant + ".json");
            }
            assertIafResource(helper, iafPack, "neoforge/biome_modifier/spawn_dragon_skeleton_" + element + ".json");
            for (String part : List.of("brick", "input")) {
                String path = "recipe/dragonforge_" + element + "_" + part + ".json";
                var source = resources.getResource(ResourceLocation.fromNamespaceAndPath("iceandfire", path)).orElseThrow();
                helper.assertTrue(source.sourcePackId().equals("mod/iaftfc"), "Forge material recipe was not overridden: " + path);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void legacyItemSaveRoundTrip(GameTestHelper helper) throws Exception {
        String fixture;
        try (var stream = FoundationGameTests.class.getClassLoader().getResourceAsStream("iaftfc/gametest/legacy_items.snbt")) {
            fixture = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
        CompoundTag root = TagParser.parseTag(fixture);
        for (Tag tag : root.getList("Items", Tag.TAG_COMPOUND)) {
            CompoundTag original = (CompoundTag) tag;
            ItemStack loaded = ItemStack.parseOptional(helper.getLevel().registryAccess(), original);
            helper.assertTrue(!loaded.isEmpty(), "Legacy item must load: " + original.getString("id"));
            helper.assertTrue(BuiltInRegistries.ITEM.getKey(loaded.getItem()).equals(ResourceLocation.parse(original.getString("id"))),
                "Legacy ID changed: " + original.getString("id"));
            helper.assertTrue(loaded.getCount() == original.getInt("count"), "Legacy count changed: " + original.getString("id"));
            CompoundTag saved = (CompoundTag) loaded.save(helper.getLevel().registryAccess());
            CompoundTag components = saved.getCompound("components");
            components.remove("tfc:heat");
            if (components.isEmpty()) {
                saved.remove("components");
            }
            helper.assertTrue(saved.equals(original), "Legacy item data changed: " + original.getString("id") + " saved=" + saved);
        }
        try (var reader = new InputStreamReader(FoundationGameTests.class.getClassLoader()
            .getResourceAsStream("iaftfc/gametest/override_manifest.json"), StandardCharsets.UTF_8)) {
            for (var entry : JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("disabled_recipes")) {
                JsonObject disabled = entry.getAsJsonObject();
                ResourceLocation recipe = ResourceLocation.parse(disabled.get("recipe").getAsString());
                ResourceLocation item = ResourceLocation.parse(disabled.get("item").getAsString());
                helper.assertTrue(helper.getLevel().getServer().getRecipeManager().byKey(recipe).isEmpty(),
                    "Disabled recipe remains: " + recipe);
                helper.assertTrue(BuiltInRegistries.ITEM.containsKey(item), "Legacy output item vanished: " + item);
            }
        }
        helper.succeed();
    }

    private static void assertIafResource(GameTestHelper helper, String iafPack, String path) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("iceandfire", path);
        var stack = helper.getLevel().getServer().getResourceManager().getResourceStack(id);
        helper.assertTrue(!stack.isEmpty() && stack.getLast().sourcePackId().equals(iafPack),
            "IaF data replaced: " + id + " sources=" + stack.stream().map(resource -> resource.sourcePackId()).toList());
    }

    private static void checkVersion(GameTestHelper helper, String modId, String version) {
        var mod = ModList.get().getModContainerById(modId);
        helper.assertTrue(mod.isPresent() && mod.get().getModInfo().getVersion().toString().equals(version),
            modId + " must be " + version);
    }
}
