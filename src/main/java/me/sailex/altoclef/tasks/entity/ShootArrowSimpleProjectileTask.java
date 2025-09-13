package me.sailex.altoclef.tasks.entity;

import me.sailex.altoclef.AltoClefController;
import me.sailex.altoclef.Debug;
import me.sailex.altoclef.tasksystem.Task;
import me.sailex.altoclef.util.helpers.LookHelper;
import me.sailex.altoclef.util.time.TimerGame;
import me.sailex.automatone.api.utils.Rotation;
import me.sailex.automatone.api.utils.input.Input;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Arrays;
import java.util.List;

public class ShootArrowSimpleProjectileTask extends Task {
   private final Entity target;
   private boolean shooting = false;
   private boolean shot = false;
   private final TimerGame shotTimer = new TimerGame(1.0);

   public ShootArrowSimpleProjectileTask(Entity target) {
      this.target = target;
   }

   @Override
   protected void onStart() {
      this.shooting = false;
   }

   private static Rotation calculateThrowLook(AltoClefController mod, Entity target) {
      float velocity = (mod.getPlayer().getItemUseTime() - mod.getPlayer().getItemUseTimeLeft()) / 20.0F;
      velocity = (velocity * velocity + velocity * 2.0F) / 3.0F;
      if (velocity > 1.0F) {
         velocity = 1.0F;
      }

      Vec3d targetCenter = target.getBoundingBox().getCenter();
      double posX = targetCenter.getX();
      double posY = targetCenter.getY();
      double posZ = targetCenter.getZ();
      posY -= 1.9F - target.getHeight();
      double relativeX = posX - mod.getPlayer().getX();
      double relativeY = posY - mod.getPlayer().getY();
      double relativeZ = posZ - mod.getPlayer().getZ();
      double hDistance = Math.sqrt(relativeX * relativeX + relativeZ * relativeZ);
      double hDistanceSq = hDistance * hDistance;
      float g = 0.006F;
      float velocitySq = velocity * velocity;
      float pitch = (float)(
         -Math.toDegrees(
            Math.atan((velocitySq - Math.sqrt(velocitySq * velocitySq - 0.006F * (0.006F * hDistanceSq + 2.0 * relativeY * velocitySq))) / 0.006F * hDistance)
         )
      );
      return Float.isNaN(pitch) ? new Rotation(target.getYaw(), target.getPitch()) : new Rotation(Vec3dToYaw(mod, new Vec3d(posX, posY, posZ)), pitch);
   }

   private static float Vec3dToYaw(AltoClefController mod, Vec3d vec) {
      return mod.getPlayer().getYaw()
         + MathHelper.wrapDegrees(
            (float)Math.toDegrees(Math.atan2(vec.getZ() - mod.getPlayer().getZ(), vec.getX() - mod.getPlayer().getX())) - 90.0F - mod.getPlayer().getYaw()
         );
   }

   @Override
   protected Task onTick() {
      AltoClefController mod = this.controller;
      this.setDebugState("Shooting projectile");
      List<Item> requiredArrows = Arrays.asList(Items.ARROW, Items.SPECTRAL_ARROW, Items.TIPPED_ARROW);
      if (mod.getItemStorage().hasItem(Items.BOW) && requiredArrows.stream().anyMatch(mod.getItemStorage()::hasItem)) {
         Rotation lookTarget = calculateThrowLook(mod, this.target);
         LookHelper.lookAt(this.controller, lookTarget);
         boolean charged = mod.getPlayer().getItemUseTime() > 20 && mod.getPlayer().getActiveItem().getItem() == Items.BOW;
         mod.getSlotHandler().forceEquipItem(Items.BOW);
         if (LookHelper.isLookingAt(mod, lookTarget) && !this.shooting) {
            mod.getInputControls().hold(Input.CLICK_RIGHT);
            this.shooting = true;
            this.shotTimer.reset();
         }

         if (this.shooting && charged) {
            for (ArrowEntity arrow : mod.getEntityTracker().getTrackedEntities(ArrowEntity.class)) {
               if (arrow.getOwner() == mod.getPlayer()) {
                  Vec3d velocity = arrow.getVelocity();
                  Vec3d delta = this.target.getPos().subtract(arrow.getPos());
                  boolean isMovingTowardsTarget = velocity.dotProduct(delta) > 0.0;
                  if (isMovingTowardsTarget) {
                     return null;
                  }
               }
            }

            mod.getInputControls().release(Input.CLICK_RIGHT);
            this.shot = true;
         }

         return null;
      } else {
         Debug.logMessage("Missing items, stopping.");
         return null;
      }
   }

   @Override
   protected void onStop(Task interruptTask) {
      this.controller.getInputControls().release(Input.CLICK_RIGHT);
   }

   @Override
   public boolean isFinished() {
      return this.shot;
   }

   @Override
   protected boolean isEqual(Task other) {
      return false;
   }

   @Override
   protected String toDebugString() {
      return "Shooting arrow at " + this.target.getType().getTranslationKey();
   }
}
