package net.claustra01.iaftfc.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.iafenvoy.iceandfire.data.DragonArmorPart;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.claustra01.iaftfc.dragonarmor.DragonArmor;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

public final class DragonArmorData implements DataProvider {
    private static final List<List<String>> PATTERNS = List.of(
        List.of("   ", " ##", "###"),
        List.of("   ", "###", " ##"),
        List.of("###", "###", "# #"),
        List.of("   ", "  #", "## "));
    private static final List<ExistingMetal> EXISTING_METALS = List.of(
        new ExistingMetal("iron", "tfc:metal/double_sheet/wrought_iron"),
        new ExistingMetal("dragon_steel_fire", "iaftfc:metal/double_sheet/dragonsteel_fire"),
        new ExistingMetal("dragon_steel_ice", "iaftfc:metal/double_sheet/dragonsteel_ice"),
        new ExistingMetal("dragon_steel_lightning", "iaftfc:metal/double_sheet/dragonsteel_lightning"));

    private final PackOutput.PathProvider recipes;
    private final PackOutput.PathProvider models;

    public DragonArmorData(PackOutput output) {
        recipes = output.createPathProvider(PackOutput.Target.DATA_PACK, "recipe");
        models = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "models/item");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        List<CompletableFuture<?>> writes = new ArrayList<>();
        for (DragonArmorPart part : DragonArmorPart.values()) {
            for (DragonArmor.Metal metal : DragonArmor.METALS) {
                addRecipe(writes, output, "iaftfc", metal.id(), part,
                    "tfc:metal/double_sheet/" + metal.id());
                addModel(writes, output, metal.id(), part);
            }
            for (ExistingMetal metal : EXISTING_METALS) {
                addRecipe(writes, output, "iceandfire", metal.id(), part, metal.ingredient());
            }
            for (String metal : DragonArmor.DISABLED_METALS) {
                disableRecipe(writes, output, metal, part);
            }
        }
        return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
    }

    private void addRecipe(List<CompletableFuture<?>> writes, CachedOutput output, String namespace,
        String metal, DragonArmorPart part, String ingredient) {
        JsonObject recipe = new JsonObject();
        recipe.addProperty("type", "minecraft:crafting_shaped");
        recipe.addProperty("category", "equipment");
        recipe.add("pattern", strings(PATTERNS.get(part.ordinal())));
        JsonObject key = new JsonObject();
        key.add("#", item(ingredient));
        recipe.add("key", key);
        String id = DragonArmor.id(metal, part);
        recipe.add("result", stack(namespace + ":" + id));
        recipe.addProperty("show_notification", true);
        save(writes, output, recipes, namespace, id, recipe);
    }

    private void disableRecipe(List<CompletableFuture<?>> writes, CachedOutput output, String metal,
        DragonArmorPart part) {
        JsonObject recipe = new JsonObject();
        JsonObject condition = new JsonObject();
        condition.addProperty("type", "neoforge:false");
        JsonArray conditions = new JsonArray();
        conditions.add(condition);
        recipe.add("neoforge:conditions", conditions);
        save(writes, output, recipes, "iceandfire", DragonArmor.id(metal, part), recipe);
    }

    private void addModel(List<CompletableFuture<?>> writes, CachedOutput output, String metal,
        DragonArmorPart part) {
        String id = DragonArmor.id(metal, part);
        JsonObject model = new JsonObject();
        model.addProperty("parent", "minecraft:item/generated");
        JsonObject textures = new JsonObject();
        textures.addProperty("layer0", "iaftfc:item/" + id);
        model.add("textures", textures);
        save(writes, output, models, "iaftfc", id, model);
    }

    private static JsonArray strings(List<String> values) {
        JsonArray array = new JsonArray();
        values.forEach(array::add);
        return array;
    }

    private static JsonObject item(String id) {
        JsonObject item = new JsonObject();
        item.addProperty("item", id);
        return item;
    }

    private static JsonObject stack(String id) {
        JsonObject stack = new JsonObject();
        stack.addProperty("count", 1);
        stack.addProperty("id", id);
        return stack;
    }

    private static void save(List<CompletableFuture<?>> writes, CachedOutput output,
        PackOutput.PathProvider provider, String namespace, String path, JsonObject json) {
        writes.add(DataProvider.saveStable(output, json,
            provider.json(ResourceLocation.fromNamespaceAndPath(namespace, path))));
    }

    @Override
    public String getName() {
        return "IaFTFC dragon armor";
    }

    private record ExistingMetal(String id, String ingredient) {
    }
}
