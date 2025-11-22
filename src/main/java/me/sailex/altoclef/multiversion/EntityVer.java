package me.sailex.altoclef.multiversion;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class EntityVer {

    public static Vec3d getPos(Entity entity) {
        //? >=1.21.10 {
        /*return entity.getEntityPos();
        *///?} else {
        return entity.getPos();
         //?}
    }

    public static World getWorld(Entity entity) {
        //? >=1.21.10 {
        /*return entity.getEntityWorld();
        *///?} else {
        return entity.getWorld();
         //?}
    }

}
