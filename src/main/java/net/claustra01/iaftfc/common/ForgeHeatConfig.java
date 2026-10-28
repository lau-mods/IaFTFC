package net.claustra01.iaftfc.common;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.iafenvoy.iceandfire.recipe.DragonForgeRecipe;
import java.util.Map;
import net.dries007.tfc.common.component.TFCComponents;
import net.dries007.tfc.common.component.heat.HeatCapability;
import net.dries007.tfc.common.component.heat.HeatComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ForgeHeatConfig extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(ForgeHeatConfig.class);
    private static final ResourceLocation CONFIG = ResourceLocation.fromNamespaceAndPath("iaftfc", "dragonsteel");
    private static final float DEFAULT_TEMPERATURE = 1800;
    private static float temperature = DEFAULT_TEMPERATURE;
    private final ReloadableServerResources resources;

    public ForgeHeatConfig(ReloadableServerResources resources) {
        super(new Gson(), "forge_heat");
        this.resources = resources;
    }

    public static void register(AddReloadListenerEvent event) {
        event.addListener(new ForgeHeatConfig(event.getServerResources()));
    }

    public static float temperature() {
        return temperature;
    }

    public static void prepareHeat(ItemStack stack) {
        stack.set(TFCComponents.HEAT, HeatComponent.of(HeatCapability.getDefinition(stack)));
    }

    @Override
    public void apply(Map<ResourceLocation, JsonElement> values, ResourceManager manager, ProfilerFiller profiler) {
        temperature = readTemperature(values.get(CONFIG));
        RecipeManager recipes = resources.getRecipeManager();
        for (String element : new String[] {"fire", "ice", "lightning"}) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath("iceandfire", "dragonforge/dragonsteel_" + element + "_ingot");
            recipes.byKey(id).ifPresent(holder -> setPreview(holder, element));
        }
    }

    private static float readTemperature(JsonElement entry) {
        float value = DEFAULT_TEMPERATURE;
        if (entry != null) {
            try {
                JsonObject object = entry.getAsJsonObject();
                value = object.get("temperature").getAsFloat();
                if (!Float.isFinite(value) || value <= 0) throw new IllegalArgumentException("temperature must be positive and finite");
            } catch (RuntimeException exception) {
                LOGGER.warn("Invalid dragon forge temperature; using {} C", DEFAULT_TEMPERATURE, exception);
                value = DEFAULT_TEMPERATURE;
            }
        }
        return value;
    }

    private static void setPreview(RecipeHolder<?> holder, String element) {
        if (holder.value() instanceof DragonForgeRecipe recipe
            && recipe.getResultItem().is(BuiltInRegistries.ITEM.get(
                ResourceLocation.fromNamespaceAndPath("iaftfc", "metal/ingot/dragonsteel_" + element)))) {
            prepareHeat(recipe.getResultItem());
            HeatCapability.setStaticTemperature(recipe.getResultItem(), temperature);
        }
    }
}
