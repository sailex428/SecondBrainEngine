package me.sailex.altoclef.multiversion;

import net.minecraft.entity.DamageUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

//? >=1.21 {
/*import net.minecraft.block.spawner.MobSpawnerLogic;
*///?} elif >= 1.20 {

import net.minecraft.world.MobSpawnerLogic;
import net.minecraft.util.math.random.Random;
//?}


public class MethodWrapper {
   public static Entity getRenderedEntity(MobSpawnerLogic logic, World world, BlockPos pos) {
        //? >=1.21 {
       /*return logic.getRenderedEntity(world, pos);
        *///?} elif >= 1.20 {
        
        return logic.getRenderedEntity(world, Random.create(), pos);
        //?}
   }

   public static float getDamageLeft(LivingEntity armorWearer, double damage, DamageSource source, double armor, double armorToughness) {
      return getDamageLeft(armorWearer, (float)damage, source, (float)armor, (float)armorToughness);
   }

   public static float getDamageLeft(LivingEntity armorWearer, float damage, DamageSource source, float armor, float armorToughness) {
       //? >=1.21 {
       /*return DamageUtil.getDamageLeft(armorWearer, damage, source, armor, armorToughness);
       *///?} elif >= 1.20 {
        
        return DamageUtil.getDamageLeft(damage, armor, armorToughness);
       //?}
   }
}
