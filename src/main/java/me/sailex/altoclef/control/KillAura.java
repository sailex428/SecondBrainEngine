package me.sailex.altoclef.control;

import me.sailex.altoclef.AltoClefController;
import me.sailex.altoclef.chains.MobDefenseChain;
import me.sailex.altoclef.mixins.LivingEntityMixin;
import me.sailex.altoclef.multiversion.item.ItemVer;
import me.sailex.altoclef.util.helpers.LookHelper;
import me.sailex.altoclef.util.helpers.StlHelper;
import me.sailex.altoclef.util.helpers.StorageHelper;
import me.sailex.altoclef.util.helpers.WorldHelper;
import me.sailex.altoclef.util.slots.PlayerSlot;
import me.sailex.altoclef.util.slots.Slot;
import me.sailex.automatone.api.utils.input.Input;
import me.sailex.automatone.behavior.PathingBehavior;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.HoglinEntity;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.entity.mob.ZoglinEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ToolItem;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class KillAura {
   private final List<Entity> targets = new ArrayList<>();
   boolean shielding = false;
   private double forceFieldRange = Double.POSITIVE_INFINITY;
   private Entity forceHit = null;
   public boolean attackedLastTick = false;

   public static void equipWeapon(AltoClefController mod) {
      ToolItem toolItem = MobDefenseChain.getBestWeapon(mod);
      if (toolItem != null) {
         mod.getSlotHandler().forceEquipItem(toolItem);
      }
   }

   public void tickStart() {
      this.targets.clear();
      this.forceHit = null;
      this.attackedLastTick = false;
   }

   public void applyAura(Entity entity) {
      this.targets.add(entity);
      if (entity instanceof FireballEntity) {
         this.forceHit = entity;
      }
   }

   public void setRange(double range) {
      this.forceFieldRange = range;
   }

   public void tickEnd(AltoClefController mod) {
      Optional<Entity> entities = this.targets.stream().min(StlHelper.compareValues(entity -> entity.squaredDistanceTo(mod.getPlayer())));
      if (entities.isPresent()
         && !mod.getEntityTracker().entityFound(PotionEntity.class)
         && (
            Double.isInfinite(this.forceFieldRange)
               || entities.get().squaredDistanceTo(mod.getPlayer()) < this.forceFieldRange * this.forceFieldRange
               || entities.get().squaredDistanceTo(mod.getPlayer()) < 40.0
         )
         && !mod.getMLGBucketChain().isFalling(mod)
         && mod.getMLGBucketChain().doneMLG()
         && !mod.getMLGBucketChain().isChorusFruiting()) {
         Slot offhandSlot = PlayerSlot.getOffhandSlot(mod.getInventory());
         Item offhandItem = StorageHelper.getItemStackInSlot(offhandSlot).getItem();
         if (entities.get().getClass() != CreeperEntity.class
            && entities.get().getClass() != HoglinEntity.class
            && entities.get().getClass() != ZoglinEntity.class
            && entities.get().getClass() != WardenEntity.class
            && entities.get().getClass() != WitherEntity.class
            && (mod.getItemStorage().hasItem(Items.SHIELD) || mod.getItemStorage().hasItemInOffhand(mod, Items.SHIELD))
            && mod.getBaritone().getPathingBehavior().isSafeToCancel()) {
            LookHelper.lookAt(mod, entities.get().getEyePos());
            ItemStack shieldSlot = StorageHelper.getItemStackInSlot(PlayerSlot.getOffhandSlot(mod.getInventory()));
            if (shieldSlot.getItem() != Items.SHIELD) {
               mod.getSlotHandler().forceEquipItemToOffhand(Items.SHIELD);
            } else if (!WorldHelper.isSurroundedByHostiles(mod)) {
               this.startShielding(mod);
            }
         }

         this.performDelayedAttack(mod);
      } else {
         this.stopShielding(mod);
      }

      switch (mod.getModSettings().getForceFieldStrategy()) {
         case FASTEST:
            this.performFastestAttack(mod);
            break;
         case DELAY:
            this.performDelayedAttack(mod);
            break;
         case SMART:
            if (this.forceHit != null) {
               this.attack(mod, this.forceHit, true);
            } else if (!mod.getFoodChain().needsToEat()
               && !mod.getMLGBucketChain().isFalling(mod)
               && mod.getMLGBucketChain().doneMLG()
               && !mod.getMLGBucketChain().isChorusFruiting()) {
               this.performDelayedAttack(mod);
            }
      }
   }

   private void performDelayedAttack(AltoClefController mod) {
      if (!mod.getFoodChain().needsToEat()
         && !mod.getMLGBucketChain().isFalling(mod)
         && mod.getMLGBucketChain().doneMLG()
         && !mod.getMLGBucketChain().isChorusFruiting()) {
         if (this.forceHit != null) {
            this.attack(mod, this.forceHit, true);
         }

         if (this.targets.isEmpty()) {
            return;
         }

         Optional<Entity> toHit = this.targets.stream().min(StlHelper.compareValues(entity -> entity.squaredDistanceTo(mod.getPlayer())));
         if (mod.getPlayer() == null || this.getAttackCooldownProgress(mod.getPlayer(), 0.0F) < 1.0F) {
            return;
         }

         toHit.ifPresent(entity -> this.attack(mod, entity, true));
      }
   }

   public float getAttackCooldownProgressPerTick(LivingEntity entity) {
      return 5.0F;
   }

   public float getAttackCooldownProgress(LivingEntity entity, float baseTime) {
      return MathHelper.clamp((((LivingEntityMixin)entity).getLastAttackedTicks() + baseTime) / this.getAttackCooldownProgressPerTick(entity), 0.0F, 1.0F);
   }

   private void performFastestAttack(AltoClefController mod) {
      if (!mod.getFoodChain().needsToEat()
         && !mod.getMLGBucketChain().isFalling(mod)
         && mod.getMLGBucketChain().doneMLG()
         && !mod.getMLGBucketChain().isChorusFruiting()) {
         for (Entity entity : this.targets) {
            this.attack(mod, entity);
         }
      }
   }

   private void attack(AltoClefController mod, Entity entity) {
      this.attack(mod, entity, false);
   }

   private void attack(AltoClefController mod, Entity entity, boolean equipWeapon) {
      if (entity != null) {
         if (!(entity instanceof FireballEntity)) {
            double xAim = entity.getX();
            double yAim = entity.getY() + entity.getHeight() / 1.4;
            double zAim = entity.getZ();
            LookHelper.lookAt(mod, new Vec3d(xAim, yAim, zAim));
         }

         if (Double.isInfinite(this.forceFieldRange)
            || entity.squaredDistanceTo(mod.getPlayer()) < this.forceFieldRange * this.forceFieldRange
            || entity.squaredDistanceTo(mod.getPlayer()) < 40.0) {
            if (entity instanceof FireballEntity) {
               mod.getControllerExtras().attack(entity);
            }

            boolean canAttack;
            if (equipWeapon) {
               equipWeapon(mod);
               canAttack = true;
            } else {
               canAttack = mod.getSlotHandler().forceDeequipHitTool();
            }

            if (canAttack && (mod.getPlayer().isOnGround() || mod.getPlayer().getVelocity().getY() < 0.0 || mod.getPlayer().isTouchingWater())) {
               this.attackedLastTick = true;
               mod.getControllerExtras().attack(entity);
            }
         }
      }
   }

   public void startShielding(AltoClefController mod) {
      this.shielding = true;
      ((PathingBehavior)mod.getBaritone().getPathingBehavior()).requestPause();
      mod.getExtraBaritoneSettings().setInteractionPaused(true);
      if (!mod.getPlayer().isBlocking()) {
         ItemStack handItem = StorageHelper.getItemStackInSlot(PlayerSlot.getEquipSlot(mod.getInventory()));
         if (ItemVer.isFood(handItem)) {
            List<ItemStack> spaceSlots = mod.getItemStorage().getItemStacksPlayerInventory(false);
            if (!spaceSlots.isEmpty()) {
               for (ItemStack spaceSlot : spaceSlots) {
                  if (spaceSlot.isEmpty()) {
                     mod.getSlotHandler().clickSlot(PlayerSlot.getEquipSlot(mod.getInventory()), 0, SlotActionType.QUICK_MOVE);
                     return;
                  }
               }
            }

            Optional<Slot> garbage = StorageHelper.getGarbageSlot(mod);
            garbage.ifPresent(slot -> mod.getSlotHandler().forceEquipItem(StorageHelper.getItemStackInSlot(slot).getItem()));
         }
      }

      mod.getInputControls().hold(Input.SNEAK);
      mod.getInputControls().hold(Input.CLICK_RIGHT);
   }

   public void stopShielding(AltoClefController mod) {
      if (this.shielding) {
         ItemStack cursor = StorageHelper.getItemStackInCursorSlot(mod);
         if (ItemVer.isFood(cursor)) {
            Optional<Slot> toMoveTo = mod.getItemStorage().getSlotThatCanFitInPlayerInventory(cursor, false).or(() -> StorageHelper.getGarbageSlot(mod));
            if (toMoveTo.isPresent()) {
               Slot garbageSlot = toMoveTo.get();
               mod.getSlotHandler().clickSlot(garbageSlot, 0, SlotActionType.PICKUP);
            }
         }

         mod.getInputControls().release(Input.SNEAK);
         mod.getInputControls().release(Input.CLICK_RIGHT);
         mod.getInputControls().release(Input.JUMP);
         mod.getExtraBaritoneSettings().setInteractionPaused(false);
         this.shielding = false;
      }
   }

   public boolean isShielding() {
      return this.shielding;
   }

   public static enum Strategy {
      OFF,
      FASTEST,
      DELAY,
      SMART;
   }
}
