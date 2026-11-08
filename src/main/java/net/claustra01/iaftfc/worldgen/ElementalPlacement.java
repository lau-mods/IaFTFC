package net.claustra01.iaftfc.worldgen;

public final class ElementalPlacement {
    public enum Element {
        FIRE(DragonClimate.FIRE_MIN_TEMPERATURE, 40.0F, 0.0F, DragonClimate.FIRE_MAX_GROUNDWATER),
        FROST(-40.0F, DragonClimate.ICE_MAX_TEMPERATURE, 0.0F, 500.0F),
        LIGHTNING(DragonClimate.LIGHTNING_MIN_TEMPERATURE, DragonClimate.LIGHTNING_MAX_TEMPERATURE,
            DragonClimate.LIGHTNING_MIN_GROUNDWATER, 500.0F);

        private final float minTemperature;
        private final float maxTemperature;
        private final float minGroundwater;
        private final float maxGroundwater;

        Element(float minTemperature, float maxTemperature, float minGroundwater, float maxGroundwater) {
            this.minTemperature = minTemperature;
            this.maxTemperature = maxTemperature;
            this.minGroundwater = minGroundwater;
            this.maxGroundwater = maxGroundwater;
        }

        public String id() {
            return name().toLowerCase() + "_lily";
        }

        public float minTemperature() {
            return minTemperature;
        }

        public float maxTemperature() {
            return maxTemperature;
        }

        public float minGroundwater() {
            return minGroundwater;
        }

        public float maxGroundwater() {
            return maxGroundwater;
        }


    }

    private ElementalPlacement() {
    }
}
