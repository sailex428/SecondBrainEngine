package me.sailex.altoclef.tasks.resources;

import me.sailex.altoclef.AltoClefController;
import me.sailex.altoclef.tasks.CraftInInventoryTask;
import me.sailex.altoclef.tasks.ResourceTask;
import me.sailex.altoclef.tasksystem.Task;
import me.sailex.altoclef.util.CraftingRecipe;
import me.sailex.altoclef.util.Dimension;
import me.sailex.altoclef.util.ItemTarget;
import me.sailex.altoclef.util.MiningRequirement;
import me.sailex.altoclef.util.RecipeTarget;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;

public class CollectSandstoneTask extends ResourceTask {
   private final int count;

   public CollectSandstoneTask(int targetCount) {
      super(Items.SANDSTONE, targetCount);
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
      if (mod.getItemStorage().getItemCount(Items.SAND) >= 4) {
         int target = mod.getItemStorage().getItemCount(Items.SANDSTONE) + 1;
         ItemTarget s = new ItemTarget(Items.SAND, 1);
         return new CraftInInventoryTask(
            new RecipeTarget(Items.SANDSTONE, target, CraftingRecipe.newShapedRecipe("sandstone", new ItemTarget[]{s, s, s, s}, 1))
         );
      } else {
         return new MineAndCollectTask(new ItemTarget(Items.SANDSTONE, Items.SAND), new Block[]{Blocks.SANDSTONE, Blocks.SAND}, MiningRequirement.WOOD)
            .forceDimension(Dimension.OVERWORLD);
      }
   }

   @Override
   protected void onResourceStop(AltoClefController mod, Task interruptTask) {
   }

   @Override
   protected boolean isEqualResource(ResourceTask other) {
      return other instanceof CollectSandstoneTask;
   }

   @Override
   protected String toDebugStringName() {
      return "Collecting " + this.count + " sandstone.";
   }
}
