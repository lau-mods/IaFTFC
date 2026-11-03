package net.claustra01.iaftfc.metal;

public enum DragonsteelMetal {
    FIRE("dragonsteel_fire", 0xff6e211b),
    ICE("dragonsteel_ice", 0xff6badc9),
    LIGHTNING("dragonsteel_lightning", 0xff4d3a69);

    private final String id;
    private final int color;

    DragonsteelMetal(String id, int color) {
        this.id = id;
        this.color = color;
    }

    public String id() {
        return id;
    }

    public int color() {
        return color;
    }
}
