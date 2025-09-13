package me.sailex.altoclef.tasks.movement;

import me.sailex.altoclef.tasksystem.Task;
import me.sailex.altoclef.util.helpers.WorldHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.BiomeKeys;

public class LocateDesertTempleTask extends Task {
   private BlockPos finalPos;

   @Override
   protected void onStart() {
   }

   @Override
   protected Task onTick() {
      BlockPos desertTemplePos = WorldHelper.getADesertTemple(this.controller);
      if (desertTemplePos != null) {
         this.finalPos = desertTemplePos.up(14);
      }

      if (this.finalPos != null) {
         this.setDebugState("Going to found desert temple");
         return new GetToBlockTask(this.finalPos, false);
      } else {
         return new SearchWithinBiomeTask(BiomeKeys.DESERT);
      }
   }

   @Override
   protected void onStop(Task interruptTask) {
   }

   @Override
   protected boolean isEqual(Task other) {
      return other instanceof LocateDesertTempleTask;
   }

   @Override
   protected String toDebugString() {
      return "Searchin' for temples";
   }

   @Override
   public boolean isFinished() {
      return this.controller.getPlayer().getBlockPos().equals(this.finalPos);
   }
}
