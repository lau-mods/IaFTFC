package net.claustra01.iaftfc.registry;

import java.util.LinkedHashMap;
import java.util.Map;
import net.claustra01.iaftfc.common.DragonsteelMetal;
import net.dries007.tfc.common.fluids.FluidHolder;
import net.dries007.tfc.common.fluids.MoltenFluid;
import net.dries007.tfc.util.registry.RegistrationHelpers;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModFluids {
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, "tfc");
    public static final DeferredRegister<FluidType> TYPES = DeferredRegister.create(NeoForgeRegistries.FLUID_TYPES, "tfc");
    public static final Map<DragonsteelMetal, FluidHolder<MoltenFluid>> METALS = new LinkedHashMap<>();

    static {
        for (DragonsteelMetal metal : DragonsteelMetal.values()) {
            String id = "metal/" + metal.id();
            METALS.put(metal, RegistrationHelpers.registerFluid(TYPES, FLUIDS, id, "metal/flowing_" + metal.id(), id,
                properties -> properties.tickRate(20),
                () -> new FluidType(FluidType.Properties.create().density(3000).viscosity(6000).temperature(2000)
                    .canConvertToSource(false).canDrown(false).canExtinguish(false).canHydrate(false).canPushEntity(false).canSwim(false).supportsBoating(false)
                    .descriptionId("fluid.tfc.metal." + metal.id())),
                MoltenFluid.Source::new, MoltenFluid.Flowing::new));
        }
    }

    private ModFluids() {}
}
