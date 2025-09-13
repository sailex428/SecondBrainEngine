package me.sailex.altoclef.tasks.misc;

import me.sailex.altoclef.tasks.construction.DestroyBlockTask;
import me.sailex.altoclef.tasks.container.LootContainerTask;
import me.sailex.altoclef.tasksystem.Task;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

import java.util.List;

public class LootDesertTempleTask extends Task {
   public final Vec3i[] CHEST_POSITIONS_RELATIVE = new Vec3i[]{new Vec3i(2, 0, 0), new Vec3i(-2, 0, 0), new Vec3i(0, 0, 2), new Vec3i(0, 0, -2)};
   private final BlockPos temple;
   private final List<Item> wanted;
   private Task lootTask;
   private short looted = 0;

   public LootDesertTempleTask(BlockPos temple, List<Item> wanted) {
      this.temple = temple;
      this.wanted = wanted;
   }

   @Override
   protected void onStart() {
      this.controller.getBaritoneSettings().blocksToAvoid.get().add(Blocks.STONE_PRESSURE_PLATE);
   }

   @Override
   protected Task onTick() {
      if (this.lootTask != null) {
         if (!this.lootTask.isFinished()) {
            this.setDebugState("Looting a desert temple chest");
            return this.lootTask;
         }

         this.looted++;
      }

      if (this.controller.getWorld().getBlockState(this.temple).getBlock() == Blocks.STONE_PRESSURE_PLATE) {
         this.setDebugState("Breaking pressure plate");
         return new DestroyBlockTask(this.temple);
      } else if (this.looted < 4) {
         this.setDebugState("Looting a desert temple chest");
         this.lootTask = new LootContainerTask(this.temple.add(this.CHEST_POSITIONS_RELATIVE[this.looted]), this.wanted);
         return this.lootTask;
      } else {
         this.setDebugState("Why is this still running? Report this");
         return null;
      }
   }

   @Override
   protected void onStop(Task task) {
      this.controller.getBaritoneSettings().blocksToAvoid.get().remove(Blocks.STONE_PRESSURE_PLATE);
   }

   @Override
   protected boolean isEqual(Task other) {
      return other instanceof LootDesertTempleTask && ((LootDesertTempleTask)other).getTemplePos() == this.temple;
   }

   @Override
   public boolean isFinished() {
      return this.looted == 4;
   }

   @Override
   protected String toDebugString() {
      return "Looting Desert Temple";
   }

   public BlockPos getTemplePos() {
      return this.temple;
   }
}
