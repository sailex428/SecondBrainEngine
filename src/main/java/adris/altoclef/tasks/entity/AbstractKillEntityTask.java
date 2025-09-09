package adris.altoclef.tasks.entity;

import adris.altoclef.AltoClefController;
import adris.altoclef.chains.MobDefenseChain;
import adris.altoclef.mixins.LivingEntityMixin;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.helpers.LookHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.slots.PlayerSlot;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolItem;
import net.minecraft.util.math.MathHelper;

public abstract class AbstractKillEntityTask extends AbstractDoToEntityTask {
   private static final double OTHER_FORCE_FIELD_RANGE = 2.0;
   private static final double CONSIDER_COMBAT_RANGE = 10.0;

   protected AbstractKillEntityTask() {
      this(10.0, 2.0);
   }

   protected AbstractKillEntityTask(double combatGuardLowerRange, double combatGuardLowerFieldRadius) {
      super(combatGuardLowerRange, combatGuardLowerFieldRadius);
   }

   protected AbstractKillEntityTask(double maintainDistance, double combatGuardLowerRange, double combatGuardLowerFieldRadius) {
      super(maintainDistance, combatGuardLowerRange, combatGuardLowerFieldRadius);
   }

   public static Item bestWeapon(AltoClefController mod) {
      ToolItem toolItem1 = null;
      List<ItemStack> invStacks = mod.getItemStorage().getItemStacksPlayerInventory(true);
      ToolItem toolItem2 = MobDefenseChain.getBestWeapon(mod);
      if (toolItem2 != null) {
         return toolItem2;
      } else {
         Item item = StorageHelper.getItemStackInSlot(PlayerSlot.getEquipSlot(mod.getInventory())).getItem();
         float bestDamage = Float.NEGATIVE_INFINITY;
         if (item instanceof ToolItem handToolItem) {
            bestDamage = handToolItem.getMaterial().getAttackDamage();
         }

         for (ItemStack invStack : invStacks) {
            if (invStack.getItem() instanceof ToolItem toolItem) {
               float itemDamage = toolItem.getMaterial().getAttackDamage();
               if (itemDamage > bestDamage) {
                  toolItem1 = toolItem;
                  bestDamage = itemDamage;
               }
            }
         }

         return toolItem1;
      }
   }

   public static boolean equipWeapon(AltoClefController mod) {
      Item bestWeapon = bestWeapon(mod);
      Item equipedWeapon = StorageHelper.getItemStackInSlot(PlayerSlot.getEquipSlot(mod.getInventory())).getItem();
      if (bestWeapon != null && bestWeapon != equipedWeapon) {
         mod.getSlotHandler().forceEquipItem(bestWeapon);
         return true;
      } else {
         return false;
      }
   }

   @Override
   protected Task onEntityInteract(AltoClefController mod, Entity entity) {
      if (!equipWeapon(mod)) {
         float hitProg = this.getAttackCooldownProgress(mod.getPlayer(), 0.0F);
         if (hitProg >= 1.0F && (mod.getPlayer().isOnGround() || mod.getPlayer().getVelocity().getY() < 0.0 || mod.getPlayer().isTouchingWater())) {
            LookHelper.lookAt(mod, entity.getEyePos());
            mod.getControllerExtras().attack(entity);
         }
      }

      return null;
   }

   public float getAttackCooldownProgressPerTick(LivingEntity entity) {
      return 5.0F;
   }

   public float getAttackCooldownProgress(LivingEntity entity, float baseTime) {
      return MathHelper.clamp((((LivingEntityMixin)entity).getLastAttackedTicks() + baseTime) / this.getAttackCooldownProgressPerTick(entity), 0.0F, 1.0F);
   }
}
