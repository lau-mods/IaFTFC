package net.claustra01.iaftfc.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.claustra01.iaftfc.dragonarmor.DragonArmor;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.neoforged.fml.ModList;

public final class DragonArmorData implements DataProvider {
    private static final String[][] PATTERNS = {
        {"   ", " ##", "###"}, {"   ", "###", " ##"},
        {"###", "###", "# #"}, {"   ", "  #", "## "}
    };
    private final PackOutput output;

    public DragonArmorData(PackOutput output) {
        this.output = output;
    }

    @Override
    public String getName() {
        return "IaFTFC dragon armor";
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        try {
            List<CompletableFuture<?>> writes = new ArrayList<>();
            String[] metals = {"iron", "steel", "black_steel", "red_steel", "blue_steel",
                "dragon_steel_fire", "dragon_steel_ice", "dragon_steel_lightning"};
            for (String part : DragonArmor.PARTS) {
                JsonArray items = new JsonArray();
                for (String metal : metals) {
                    JsonObject item = new JsonObject();
                    item.addProperty("item", (isNew(metal) ? "iaftfc:" : "iceandfire:")
                        + "dragonarmor_" + metal + "_" + part);
                    items.add(item);
                }
                for (String metal : DragonArmor.DISABLED_METALS) {
                    JsonObject item = new JsonObject();
                    item.addProperty("item", "iceandfire:dragonarmor_" + metal + "_" + part);
                    items.add(item);
                }
                JsonObject size = new JsonObject();
                size.add("ingredient", items);
                size.addProperty("size", "large");
                size.addProperty("weight", "medium");
                write(cache, writes, "data/iaftfc/tfc/item_size/dragonarmor_" + part + ".json", size);
                for (String metal : metals) {
                    String id = "dragonarmor_" + metal + "_" + part;
                    String material = switch (metal) {
                        case "iron" -> "tfc:metal/double_sheet/wrought_iron";
                        case "dragon_steel_fire" -> "iaftfc:metal/double_sheet/dragonsteel_fire";
                        case "dragon_steel_ice" -> "iaftfc:metal/double_sheet/dragonsteel_ice";
                        case "dragon_steel_lightning" -> "iaftfc:metal/double_sheet/dragonsteel_lightning";
                        default -> "tfc:metal/double_sheet/" + metal;
                    };
                    JsonObject recipe = new JsonObject();
                    recipe.addProperty("type", "minecraft:crafting_shaped");
                    recipe.addProperty("category", "equipment");
                    JsonArray pattern = new JsonArray();
                    for (String row : PATTERNS[java.util.Arrays.asList(DragonArmor.PARTS).indexOf(part)]) {
                        pattern.add(row);
                    }
                    recipe.add("pattern", pattern);
                    JsonObject key = new JsonObject();
                    JsonObject ingredientItem = new JsonObject();
                    ingredientItem.addProperty("item", material);
                    key.add("#", ingredientItem);
                    recipe.add("key", key);
                    JsonObject result = new JsonObject();
                    result.addProperty("id", (isNew(metal) ? "iaftfc:" : "iceandfire:") + id);
                    recipe.add("result", result);
                    write(cache, writes, "data/" + (isNew(metal) ? "iaftfc" : "iceandfire")
                        + "/recipe/" + id + ".json", recipe);
                }
            }
            for (String metal : DragonArmor.DISABLED_METALS) {
                for (String part : DragonArmor.PARTS) {
                    String path = "data/iceandfire/recipe/dragonarmor_" + metal + "_" + part + ".json";
                    if (!Files.exists(ModList.get().getModFileById("iceandfire").getFile().findResource(path))) {
                        throw new IOException("Missing IaF recipe: " + path);
                    }
                    JsonObject disabled = new JsonObject();
                    JsonObject falseCondition = new JsonObject();
                    falseCondition.addProperty("type", "neoforge:false");
                    JsonArray conditions = new JsonArray();
                    conditions.add(falseCondition);
                    disabled.add("neoforge:conditions", conditions);
                    write(cache, writes, path, disabled);
                }
            }
            for (int metal = 0; metal < DragonArmor.METALS.length; metal++) {
                String name = DragonArmor.METALS[metal];
                for (String part : DragonArmor.PARTS) {
                    String id = "dragonarmor_" + name + "_" + part;
                    JsonObject model = new JsonObject();
                    model.addProperty("parent", "minecraft:item/generated");
                    JsonObject textures = new JsonObject();
                    textures.addProperty("layer0", "iaftfc:item/" + id);
                    model.add("textures", textures);
                    write(cache, writes, "assets/iaftfc/models/item/" + id + ".json", model);
                    JsonObject item = new JsonObject();
                    JsonObject modelRef = new JsonObject();
                    modelRef.addProperty("type", "minecraft:model");
                    modelRef.addProperty("model", "iaftfc:item/" + id);
                    item.add("model", modelRef);
                    write(cache, writes, "assets/iaftfc/items/" + id + ".json", item);

                }
            }
            return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
        } catch (IOException exception) {
            return CompletableFuture.failedFuture(exception);
        }
    }

    private static boolean isNew(String metal) {
        return java.util.Arrays.asList(DragonArmor.METALS).contains(metal);
    }

    private void write(CachedOutput cache, List<CompletableFuture<?>> writes, String path, JsonObject json) {
        writes.add(DataProvider.saveStable(cache, json, output.getOutputFolder().resolve(path)));
    }
}
