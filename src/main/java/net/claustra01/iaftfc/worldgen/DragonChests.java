package net.claustra01.iaftfc.worldgen;

import com.iafenvoy.iceandfire.world.structure.FireDragonCaveStructure;
import com.iafenvoy.iceandfire.world.structure.FireDragonRoostStructure;
import com.iafenvoy.iceandfire.world.structure.IceDragonCaveStructure;
import com.iafenvoy.iceandfire.world.structure.IceDragonRoostStructure;
import com.iafenvoy.iceandfire.world.structure.LightningDragonCaveStructure;
import com.iafenvoy.iceandfire.world.structure.LightningDragonRoostStructure;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

public final class DragonChests {
    public static Block block(DragonClimate.Element element) {
        String wood = switch (element) {
            case FIRE -> "chestnut";
            case ICE -> "white_cedar";
            case LIGHTNING -> "blackwood";
        };
        return BuiltInRegistries.BLOCK.get(ResourceLocation.parse("tfc:wood/chest/" + wood));
    }

    public static DragonClimate.Element element(Object piece) {
        if (piece instanceof FireDragonCaveStructure.FireDragonCavePiece
            || piece instanceof FireDragonRoostStructure.FireDragonRoostPiece) {
            return DragonClimate.Element.FIRE;
        }
        if (piece instanceof IceDragonCaveStructure.IceDragonCavePiece
            || piece instanceof IceDragonRoostStructure.IceDragonRoostPiece) {
            return DragonClimate.Element.ICE;
        }
        if (piece instanceof LightningDragonCaveStructure.LightningDragonCavePiece
            || piece instanceof LightningDragonRoostStructure.LightningDragonRoostPiece) {
            return DragonClimate.Element.LIGHTNING;
        }
        throw new IllegalArgumentException("Unknown dragon structure piece: " + piece.getClass().getName());
    }

    private DragonChests() {
    }
}
