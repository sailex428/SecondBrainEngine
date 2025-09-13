package me.sailex.altoclef.tasks;

import me.sailex.altoclef.AltoClefController;
import me.sailex.altoclef.tasks.resources.CollectBucketLiquidTask;
import me.sailex.altoclef.tasksystem.Task;
import me.sailex.altoclef.util.ItemTarget;
import net.minecraft.item.Items;

public class GetRidOfExtraWaterBucketTask extends Task {
   private boolean needsPickup = false;

   @Override
   protected void onStart() {
   }

   @Override
   protected Task onTick() {
      AltoClefController mod = this.controller;
      if (mod.getItemStorage().getItemCount(Items.WATER_BUCKET) != 0 && !this.needsPickup) {
         return new InteractWithBlockTask(new ItemTarget(Items.WATER_BUCKET, 1), mod.getPlayer().getBlockPos().down(), false);
      } else {
         this.needsPickup = true;
         return mod.getItemStorage().getItemCount(Items.WATER_BUCKET) < 1 ? new CollectBucketLiquidTask.CollectWaterBucketTask(1) : null;
      }
   }

   @Override
   public boolean isFinished() {
      return this.controller.getItemStorage().getItemCount(Items.WATER_BUCKET) == 1 && this.needsPickup;
   }

   @Override
   protected void onStop(Task interruptTask) {
   }

   @Override
   protected boolean isEqual(Task other) {
      return other instanceof GetRidOfExtraWaterBucketTask;
   }

   @Override
   protected String toDebugString() {
      return null;
   }
}
