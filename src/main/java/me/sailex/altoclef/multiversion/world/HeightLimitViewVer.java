package me.sailex.altoclef.multiversion.world;

import net.minecraft.world.World;

public class HeightLimitViewVer {

    public static int getTopY(World world) {
        //? >=1.21.8 {
        /*return world.getBottomY() + world.getHeight();
        *///?} else {
        
        return world.getTopY();
         //?}
    }

}
