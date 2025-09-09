package adris.altoclef.util.helpers;

import adris.altoclef.AltoClefController;
import adris.altoclef.multiversion.DamageSourceWrapper;
import adris.altoclef.multiversion.MethodWrapper;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.DamageUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PiglinEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.mob.ZombifiedPiglinEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class EntityHelper {
   public static final double ENTITY_GRAVITY = 0.08;

   public static boolean isAngryAtPlayer(AltoClefController mod, Entity mob) {
      boolean hostile = isProbablyHostileToPlayer(mod, mob);
      return !(mob instanceof MobEntity entity) ? hostile : hostile && entity.getTarget() == mod.getPlayer();
   }

   public static boolean isProbablyHostileToPlayer(AltoClefController mod, Entity entity) {
      if (entity instanceof MobEntity mob) {
         if (mob instanceof SlimeEntity slime) {
            return slime.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE) > 0.0;
         } else if (mob instanceof PiglinEntity piglin) {
            return piglin.isAttacking() && !isTradingPiglin(mob) && piglin.isAdult();
         } else if (mob instanceof EndermanEntity enderman) {
            return enderman.isAngry();
         } else {
            return mob instanceof ZombifiedPiglinEntity zombifiedPiglin ? zombifiedPiglin.isAttacking() : mob.isAttacking() || mob instanceof HostileEntity;
         }
      } else {
         return false;
      }
   }

   public static boolean isTradingPiglin(Entity entity) {
      if (entity instanceof PiglinEntity pig && pig.getHandItems() != null) {
         for (ItemStack stack : pig.getHandItems()) {
            if (stack.getItem().equals(Items.GOLD_INGOT)) {
               return true;
            }
         }
      }

      return false;
   }

   public static double calculateResultingPlayerDamage(LivingEntity player, DamageSource src, double damageAmount) {
      DamageSourceWrapper source = DamageSourceWrapper.of(src);
      if (player.isInvulnerableTo(src)) {
         return 0.0;
      } else {
         if (!source.bypassesArmor()) {
            damageAmount = MethodWrapper.getDamageLeft(
               player, damageAmount, src, (double)player.getArmor(), player.getAttributeValue(EntityAttributes.GENERIC_ARMOR_TOUGHNESS)
            );
         }

         if (!source.bypassesShield()) {
            if (player.hasStatusEffect(StatusEffects.RESISTANCE) && source.isOutOfWorld()) {
               float k = (player.getStatusEffect(StatusEffects.RESISTANCE).getAmplifier() + 1) * 5;
               float j = 25.0F - k;
               double f = damageAmount * j;
               damageAmount = Math.max(f / 25.0, 0.0);
            }

            if (damageAmount <= 0.0) {
               damageAmount = 0.0;
            } else {
               float k = EnchantmentHelper.getProtectionAmount(player.getArmorItems(), src);
               if (k > 0.0F) {
                  damageAmount = DamageUtil.getInflictedDamage((float)damageAmount, k);
               }
            }
         }

         return Math.max(damageAmount - player.getAbsorptionAmount(), 0.0);
      }
   }
}
