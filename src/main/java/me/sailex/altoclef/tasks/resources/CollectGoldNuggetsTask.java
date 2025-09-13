package me.sailex.altoclef.tasks.resources;

import me.sailex.altoclef.AltoClefController;
import me.sailex.altoclef.TaskCatalogue;
import me.sailex.altoclef.tasks.CraftInInventoryTask;
import me.sailex.altoclef.tasks.ResourceTask;
import me.sailex.altoclef.tasks.movement.DefaultGoToDimensionTask;
import me.sailex.altoclef.tasksystem.Task;
import me.sailex.altoclef.util.CraftingRecipe;
import me.sailex.altoclef.util.Dimension;
import me.sailex.altoclef.util.ItemTarget;
import me.sailex.altoclef.util.MiningRequirement;
import me.sailex.altoclef.util.RecipeTarget;
import me.sailex.altoclef.util.helpers.WorldHelper;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

public class CollectGoldNuggetsTask extends ResourceTask {
   private final int count;

   public CollectGoldNuggetsTask(int count) {
      super(Items.GOLD_NUGGET, count);
      this.count = count;
   }

   @Override
   protected boolean shouldAvoidPickingUp(AltoClefController mod) {
      return false;
   }

   @Override
   protected void onResourceStart(AltoClefController mod) {
   }

    protected Task onResourceTick(AltoClefController mod) {
        int potentialNuggies, nuggiesStillNeeded;
        switch (WorldHelper.getCurrentDimension(controller).ordinal()) {
            case 1:
                setDebugState("Getting gold ingots to convert to nuggets");
                potentialNuggies = mod.getItemStorage().getItemCount(new Item[]{Items.GOLD_NUGGET}) + mod.getItemStorage().getItemCount(new Item[]{Items.GOLD_INGOT}) * 9;
                if (potentialNuggies >= this.count && mod.getItemStorage().hasItem(new Item[]{Items.GOLD_INGOT}))
                    return (Task) new CraftInInventoryTask(new RecipeTarget(Items.GOLD_NUGGET, this.count, CraftingRecipe.newShapedRecipe("golden_nuggets", new ItemTarget[]{new ItemTarget(Items.GOLD_INGOT, 1), null, null, null}, 9)));
                nuggiesStillNeeded = this.count - potentialNuggies;
                return (Task) TaskCatalogue.getItemTask(Items.GOLD_INGOT, (int) Math.ceil(nuggiesStillNeeded / 9.0D));
            case 2:
                setDebugState("Mining nuggies");
                return (Task) new MineAndCollectTask(Items.GOLD_NUGGET, this.count, new Block[]{Blocks.NETHER_GOLD_ORE, Blocks.GILDED_BLACKSTONE}, MiningRequirement.WOOD);
            case 3:
                setDebugState("Going to overworld");
                return (Task) new DefaultGoToDimensionTask(Dimension.OVERWORLD);
        }
        setDebugState("INVALID DIMENSION??: " + String.valueOf(WorldHelper.getCurrentDimension(controller)));
        return null;
    }

   @Override
   protected void onResourceStop(AltoClefController mod, Task interruptTask) {
   }

   @Override
   protected boolean isEqualResource(ResourceTask other) {
      return other instanceof CollectGoldNuggetsTask;
   }

   @Override
   protected String toDebugStringName() {
      return "Collecting " + this.count + " nuggets";
   }
}
