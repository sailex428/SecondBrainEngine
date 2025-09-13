package me.sailex.altoclef.tasks.movement;

import me.sailex.altoclef.tasksystem.Task;
import net.minecraft.util.math.BlockPos;

public class GetCloseToBlockTask extends Task {
   private final BlockPos toApproach;
   private int currentRange;

   public GetCloseToBlockTask(BlockPos toApproach) {
      this.toApproach = toApproach;
   }

   @Override
   protected void onStart() {
      this.currentRange = Integer.MAX_VALUE;
   }

   @Override
   protected Task onTick() {
      if (this.inRange()) {
         this.currentRange = this.getCurrentDistance() - 1;
      }

      return new GetWithinRangeOfBlockTask(this.toApproach, this.currentRange);
   }

   @Override
   protected void onStop(Task interruptTask) {
   }

   private int getCurrentDistance() {
      return (int)Math.sqrt(this.controller.getPlayer().getBlockPos().getSquaredDistance(this.toApproach));
   }

   private boolean inRange() {
      return this.controller.getPlayer().getBlockPos().getSquaredDistance(this.toApproach) <= this.currentRange * this.currentRange;
   }

   @Override
   protected boolean isEqual(Task other) {
      return other instanceof GetCloseToBlockTask task ? task.toApproach.equals(this.toApproach) : false;
   }

   @Override
   protected String toDebugString() {
      return "Approaching " + this.toApproach.toShortString();
   }
}
