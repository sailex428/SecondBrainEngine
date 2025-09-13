package me.sailex.altoclef.tasks.resources;

import me.sailex.altoclef.AltoClefController;
import me.sailex.altoclef.tasks.CraftInInventoryTask;
import me.sailex.altoclef.tasks.ResourceTask;
import me.sailex.altoclef.tasksystem.Task;
import me.sailex.altoclef.util.CraftingRecipe;
import me.sailex.altoclef.util.ItemTarget;
import me.sailex.altoclef.util.MiningRequirement;
import me.sailex.altoclef.util.RecipeTarget;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;

public class CollectWheatTask extends ResourceTask {
   private final int count;

   public CollectWheatTask(int targetCount) {
      super(Items.WHEAT, targetCount);
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
      int potentialCount = mod.getItemStorage().getItemCount(Items.WHEAT) + 9 * mod.getItemStorage().getItemCount(Items.HAY_BLOCK);
      if (potentialCount >= this.count) {
         this.setDebugState("Crafting wheat");
         return new CraftInInventoryTask(
            new RecipeTarget(
               Items.WHEAT, this.count, CraftingRecipe.newShapedRecipe("wheat", new ItemTarget[]{new ItemTarget(Items.HAY_BLOCK, 1), null, null, null}, 9)
            )
         );
      } else {
         return (Task)(!mod.getBlockScanner().anyFound(Blocks.HAY_BLOCK) && !mod.getEntityTracker().itemDropped(Items.HAY_BLOCK)
            ? new CollectCropTask(new ItemTarget(Items.WHEAT, this.count), new Block[]{Blocks.WHEAT}, Items.WHEAT_SEEDS)
            : new MineAndCollectTask(Items.HAY_BLOCK, 99999999, new Block[]{Blocks.HAY_BLOCK}, MiningRequirement.HAND));
      }
   }

   @Override
   protected void onResourceStop(AltoClefController mod, Task interruptTask) {
   }

   @Override
   protected boolean isEqualResource(ResourceTask other) {
      return other instanceof CollectWheatTask;
   }

   @Override
   protected String toDebugStringName() {
      return "Collecting " + this.count + " wheat.";
   }
}
