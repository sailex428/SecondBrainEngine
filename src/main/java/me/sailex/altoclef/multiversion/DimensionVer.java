package me.sailex.altoclef.multiversion;

import net.minecraft.world.World;

//? >=1.21.11 {
/*import net.minecraft.world.attribute.EnvironmentAttributes;
*///?}

public class DimensionVer {

    public static boolean isUltrawarm(World world) {
        //? >=1.21.11 {
        /*return world.getEnvironmentAttributes().getAttributeValue(EnvironmentAttributes.WATER_EVAPORATES_GAMEPLAY);
        *///?} else {
        return world.getDimension().ultrawarm();
        //?}
    }
}
