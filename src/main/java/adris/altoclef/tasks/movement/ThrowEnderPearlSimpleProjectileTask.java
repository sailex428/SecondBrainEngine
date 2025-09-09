package adris.altoclef.tasks.movement;

import adris.altoclef.AltoClefController;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.helpers.LookHelper;
import adris.altoclef.util.helpers.ProjectileHelper;
import adris.altoclef.util.helpers.WorldHelper;
import adris.altoclef.util.time.TimerGame;
import baritone.api.utils.Rotation;
import baritone.api.utils.input.Input;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class ThrowEnderPearlSimpleProjectileTask extends Task {
   private final TimerGame thrownTimer = new TimerGame(5.0);
   private final BlockPos target;
   private boolean thrown = false;

   public ThrowEnderPearlSimpleProjectileTask(BlockPos target) {
      this.target = target;
   }

   private static boolean cleanThrow(AltoClefController mod, float yaw, float pitch) {
      Rotation rotation = new Rotation(yaw, -1.0F * pitch);
      float range = 3.0F;
      Vec3d delta = LookHelper.toVec3d(rotation).multiply(range);
      Vec3d start = LookHelper.getCameraPos(mod);
      return LookHelper.cleanLineOfSight(mod, start.add(delta), (double)range);
   }

   private static Rotation calculateThrowLook(AltoClefController mod, BlockPos end) {
      Vec3d start = ProjectileHelper.getThrowOrigin(mod.getPlayer());
      Vec3d endCenter = WorldHelper.toVec3d(end);
      double gravity = 0.03;
      double speed = 1.5;
      float yaw = LookHelper.getLookRotation(mod, end).getYaw();
      double flatDistance = WorldHelper.distanceXZ(start, endCenter);
      double[] pitches = ProjectileHelper.calculateAnglesForSimpleProjectileMotion(start.y - endCenter.y, flatDistance, speed, gravity);
      double pitch = cleanThrow(mod, yaw, (float)pitches[0]) ? pitches[0] : pitches[1];
      return new Rotation(yaw, -1.0F * (float)pitch);
   }

   @Override
   protected void onStart() {
      this.thrownTimer.forceElapse();
      this.thrown = false;
   }

   @Override
   protected Task onTick() {
      AltoClefController mod = this.controller;
      if (mod.getEntityTracker().entityFound(EnderPearlEntity.class)) {
         this.thrownTimer.reset();
      }

      if (this.thrownTimer.elapsed() && mod.getSlotHandler().forceEquipItem(Items.ENDER_PEARL)) {
         Rotation lookTarget = calculateThrowLook(mod, this.target);
         LookHelper.lookAt(this.controller, lookTarget);
         if (LookHelper.isLookingAt(mod, lookTarget)) {
            mod.getInputControls().tryPress(Input.CLICK_RIGHT);
            this.thrown = true;
            this.thrownTimer.reset();
         }
      }

      return null;
   }

   @Override
   protected void onStop(Task interruptTask) {
   }

   @Override
   public boolean isFinished() {
      return this.thrown && this.thrownTimer.elapsed() || !this.thrown && !this.controller.getItemStorage().hasItem(Items.ENDER_PEARL);
   }

   @Override
   protected boolean isEqual(Task other) {
      return other instanceof ThrowEnderPearlSimpleProjectileTask task ? task.target.equals(this.target) : false;
   }

   @Override
   protected String toDebugString() {
      return "Simple Ender Pearling to " + this.target;
   }
}
