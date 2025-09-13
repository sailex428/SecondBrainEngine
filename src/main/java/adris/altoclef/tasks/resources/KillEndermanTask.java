package adris.altoclef.tasks.resources;

import adris.altoclef.AltoClefController;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasks.entity.KillEntitiesTask;
import adris.altoclef.tasks.entity.KillEntityTask;
import adris.altoclef.tasks.movement.GetWithinRangeOfBlockTask;
import adris.altoclef.tasks.movement.TimeoutWanderTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.helpers.WorldHelper;
import adris.altoclef.util.time.TimerGame;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;
import java.util.function.Predicate;

public class KillEndermanTask extends ResourceTask {
   private final int count;
   private final TimerGame lookDelay = new TimerGame(0.2);

   public KillEndermanTask(int count) {
      super(new ItemTarget(Items.ENDER_PEARL, count));
      this.count = count;
      this.forceDimension(Dimension.NETHER);
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
      if (!mod.getEntityTracker().entityFound(EndermanEntity.class)) {
         if (WorldHelper.getCurrentDimension(mod) != Dimension.NETHER) {
            return this.getToCorrectDimensionTask(mod);
         } else {
            Optional<BlockPos> nearest = mod.getBlockScanner()
               .getNearestBlock(Blocks.TWISTING_VINES, Blocks.TWISTING_VINES_PLANT, Blocks.WARPED_HYPHAE, Blocks.WARPED_NYLIUM);
            if (nearest.isPresent()) {
               if (WorldHelper.inRangeXZ(nearest.get(), mod.getPlayer().getBlockPos(), 40.0)) {
                  this.setDebugState("Waiting for endermen to spawn...");
                  return null;
               } else {
                  this.setDebugState("Getting to warped forest biome");
                  return new GetWithinRangeOfBlockTask(nearest.get(), 35);
               }
            } else {
               this.setDebugState("Warped forest biome not found");
               return new TimeoutWanderTask();
            }
         }
      } else {
         Predicate<Entity> belowNetherRoof = entityx -> WorldHelper.getCurrentDimension(mod) != Dimension.NETHER || entityx.getY() < 125.0;
         int TOO_FAR_AWAY = WorldHelper.getCurrentDimension(mod) == Dimension.NETHER ? 10 : 256;

         for (EndermanEntity entity : mod.getEntityTracker().getTrackedEntities(EndermanEntity.class)) {
            if (entity.isAlive() && belowNetherRoof.test(entity) && entity.isAngry() && entity.getPos().isInRange(mod.getPlayer().getPos(), TOO_FAR_AWAY)
               )
             {
               return new KillEntityTask(entity);
            }
         }

         return new KillEntitiesTask(belowNetherRoof, EndermanEntity.class);
      }
   }

   @Override
   protected void onResourceStop(AltoClefController mod, Task interruptTask) {
   }

   @Override
   protected boolean isEqualResource(ResourceTask other) {
      return other instanceof KillEndermanTask task ? task.count == this.count : false;
   }

   @Override
   protected String toDebugStringName() {
      return "Hunting endermen for pearls - " + this.controller.getItemStorage().getItemCount(Items.ENDER_PEARL) + "/" + this.count;
   }
}
