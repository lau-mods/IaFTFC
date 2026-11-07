package net.claustra01.iaftfc.worldgen;

import java.util.Optional;

public final class DragonClimate {
    public static final float ICE_MAX_TEMPERATURE = 0.0F;
    public static final float FIRE_MIN_TEMPERATURE = 18.0F;
    public static final float FIRE_MAX_GROUNDWATER = 250.0F;
    public static final float LIGHTNING_MIN_TEMPERATURE = 8.0F;
    public static final float LIGHTNING_MAX_TEMPERATURE = 16.0F;
    public static final float LIGHTNING_MIN_GROUNDWATER = 350.0F;

    public enum Element {
        FIRE,
        ICE,
        LIGHTNING
    }

    public static Optional<Element> select(float averageTemperature, float averageGroundwater) {
        if (averageTemperature <= ICE_MAX_TEMPERATURE) {
            return Optional.of(Element.ICE);
        }
        if (averageTemperature >= FIRE_MIN_TEMPERATURE && averageGroundwater <= FIRE_MAX_GROUNDWATER) {
            return Optional.of(Element.FIRE);
        }
        if (averageTemperature >= LIGHTNING_MIN_TEMPERATURE
            && averageTemperature <= LIGHTNING_MAX_TEMPERATURE
            && averageGroundwater >= LIGHTNING_MIN_GROUNDWATER) {
            return Optional.of(Element.LIGHTNING);
        }
        return Optional.empty();
    }

    private DragonClimate() {
    }
}
