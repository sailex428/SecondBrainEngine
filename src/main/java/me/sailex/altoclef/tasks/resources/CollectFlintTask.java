package me.sailex.altoclef.tasks.resources;

import me.sailex.altoclef.AltoClefController;
import me.sailex.altoclef.TaskCatalogue;
import me.sailex.altoclef.tasks.DoToClosestBlockTask;
import me.sailex.altoclef.tasks.ResourceTask;
import me.sailex.altoclef.tasks.construction.DestroyBlockTask;
import me.sailex.altoclef.tasks.construction.PlaceBlockNearbyTask;
import me.sailex.altoclef.tasksystem.Task;
import me.sailex.altoclef.util.helpers.WorldHelper;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

public class CollectFlintTask extends ResourceTask {
   private static final float CLOSE_ENOUGH_FLINT = 10.0F;
   private final int count;

   public CollectFlintTask(int targetCount) {
      super(Items.FLINT, targetCount);
      this.count = targetCount;
   }

   @Override
   protected boolean shouldAvoidPickingUp(AltoClefController mod) {
      return false;
   }

   @Override
   protected void onResourceStart(AltoClefController mod) {
   }

   @Override
   protected Task onResourceTick(AltoClefController mod) {
      Optional<BlockPos> closest = mod.getBlockScanner()
         .getNearestBlock(
            mod.getPlayer().getPos(),
            validGravel -> WorldHelper.fallingBlockSafeToBreak(this.controller, validGravel) && WorldHelper.canBreak(this.controller, validGravel),
            Blocks.GRAVEL
         );
      if (closest.isPresent() && closest.get().isWithinDistance(mod.getPlayer().getPos(), 10.0)) {
         return new DoToClosestBlockTask(DestroyBlockTask::new, Blocks.GRAVEL);
      } else {
         return (Task)(mod.getItemStorage().hasItem(Items.GRAVEL) ? new PlaceBlockNearbyTask(Blocks.GRAVEL) : TaskCatalogue.getItemTask(Items.GRAVEL, 1));
      }
   }

   @Override
   protected void onResourceStop(AltoClefController mod, Task interruptTask) {
   }

   @Override
   protected boolean isEqualResource(ResourceTask other) {
      return other instanceof CollectFlintTask task ? task.count == this.count : false;
   }

   @Override
   protected String toDebugStringName() {
      return "Collect " + this.count + " flint";
   }
}
