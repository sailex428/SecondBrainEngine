package me.sailex.altoclef.multiversion;

import net.minecraft.entity.mob.CreeperEntity;

public class MobEntityVer {

    public static float getClientFuseTime(CreeperEntity creeperEntity, float tickProgress) {
        //? >=1.21.8 {
        /*return creeperEntity.getLerpedFuseTime(tickProgress);
        *///?} else {
        return creeperEntity.getClientFuseTime(tickProgress);
         //?}
    }

}
