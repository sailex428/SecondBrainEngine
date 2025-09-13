package me.sailex.altoclef.tasks.construction;

import me.sailex.altoclef.tasks.InteractWithBlockTask;
import me.sailex.altoclef.tasksystem.Task;
import me.sailex.altoclef.util.ItemTarget;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.RaycastContext.FluidHandling;

public class ClearLiquidTask extends Task {
   private final BlockPos liquidPos;

   public ClearLiquidTask(BlockPos liquidPos) {
      this.liquidPos = liquidPos;
   }

   @Override
   protected void onStart() {
   }

   @Override
   protected Task onTick() {
      if (this.controller.getItemStorage().hasItem(Items.BUCKET)) {
         this.controller.getBehaviour().setRayTracingFluidHandling(FluidHandling.SOURCE_ONLY);
         return new InteractWithBlockTask(new ItemTarget(Items.BUCKET, 1), this.liquidPos, false);
      } else {
         return new PlaceStructureBlockTask(this.liquidPos);
      }
   }

   @Override
   protected void onStop(Task interruptTask) {
   }

   @Override
   public boolean isFinished() {
      return this.controller.getChunkTracker().isChunkLoaded(this.liquidPos)
         ? this.controller.getWorld().getBlockState(this.liquidPos).getFluidState().isEmpty()
         : false;
   }

   @Override
   protected boolean isEqual(Task other) {
      return other instanceof ClearLiquidTask task ? task.liquidPos.equals(this.liquidPos) : false;
   }

   @Override
   protected String toDebugString() {
      return "Clear liquid at " + this.liquidPos;
   }
}
