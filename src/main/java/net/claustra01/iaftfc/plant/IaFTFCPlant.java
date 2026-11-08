package net.claustra01.iaftfc.plant;

import net.dries007.tfc.common.blocks.plant.Plant;
import net.dries007.tfc.util.registry.RegistryPlant;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public enum IaFTFCPlant implements RegistryPlant {
    FIRE_LILY("fire_lily"),
    FROST_LILY("frost_lily"),
    LIGHTNING_LILY("lightning_lily");

    private final String id;

    IaFTFCPlant(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    @Override
    public boolean isWetSeasonBlooming() {
        return Plant.BLOOD_LILY.isWetSeasonBlooming();
    }

    @Override
    public int getStartTime() {
        return Plant.BLOOD_LILY.getStartTime();
    }

    @Override
    public int getEndTime() {
        return Plant.BLOOD_LILY.getEndTime();
    }

    @Override
    public float getBloomOffset() {
        return Plant.BLOOD_LILY.getBloomOffset();
    }

    @Override
    public float getBloomingEnd() {
        return Plant.BLOOD_LILY.getBloomingEnd();
    }

    @Override
    public float getSeedingEnd() {
        return Plant.BLOOD_LILY.getSeedingEnd();
    }

    @Override
    public float getDyingEnd() {
        return Plant.BLOOD_LILY.getDyingEnd();
    }

    @Override
    public float getDormantEnd() {
        return Plant.BLOOD_LILY.getDormantEnd();
    }

    @Override
    public float getSproutingEnd() {
        return Plant.BLOOD_LILY.getSproutingEnd();
    }

    @Override
    public float getSpeedFactor() {
        return Plant.BLOOD_LILY.getSpeedFactor();
    }

    @Override
    public IntegerProperty getAgeProperty() {
        return Plant.BLOOD_LILY.getAgeProperty();
    }
}
