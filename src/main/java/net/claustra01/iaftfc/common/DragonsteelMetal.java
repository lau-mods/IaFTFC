package net.claustra01.iaftfc.common;

public enum DragonsteelMetal {
    FIRE("dragonsteel_fire", 0x6E211B),
    ICE("dragonsteel_ice", 0x6BADC9),
    LIGHTNING("dragonsteel_lightning", 0x4D3A69);

    private final String id;
    private final int color;

    DragonsteelMetal(String id, int color) {
        this.id = id;
        this.color = color;
    }

    public String id() { return id; }
    public int color() { return color; }
}
