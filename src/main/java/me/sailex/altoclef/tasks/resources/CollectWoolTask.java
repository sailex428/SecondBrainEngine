package me.sailex.altoclef.tasks.resources;

import me.sailex.altoclef.AltoClefController;
import me.sailex.altoclef.tasks.ResourceTask;
import me.sailex.altoclef.tasks.entity.ShearSheepTask;
import me.sailex.altoclef.tasksystem.Task;
import me.sailex.altoclef.util.ItemTarget;
import me.sailex.altoclef.util.MiningRequirement;
import me.sailex.altoclef.util.helpers.ItemHelper;
import net.minecraft.block.Block;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.DyeColor;

import java.util.Arrays;
import java.util.HashSet;

public class CollectWoolTask extends ResourceTask {
   private final int count;
   private final HashSet<DyeColor> colors;
   private final Item[] wools;

   public CollectWoolTask(DyeColor[] colors, int count) {
      super(new ItemTarget(ItemHelper.WOOL, count));
      this.colors = new HashSet<>(Arrays.asList(colors));
      this.count = count;
      this.wools = getWoolColorItems(colors);
   }

   public CollectWoolTask(DyeColor color, int count) {
      this(new DyeColor[]{color}, count);
   }

   public CollectWoolTask(int count) {
      this(DyeColor.values(), count);
   }

   private static Item[] getWoolColorItems(DyeColor[] colors) {
      Item[] result = new Item[colors.length];

      for (int i = 0; i < result.length; i++) {
         result[i] = ItemHelper.getColorfulItems(colors[i]).wool;
      }

      return result;
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
      Block[] woolBlocks = ItemHelper.itemsToBlocks(this.wools);
      if (mod.getBlockScanner().anyFound(woolBlocks)) {
         return new MineAndCollectTask(new ItemTarget(this.wools), woolBlocks, MiningRequirement.HAND);
      } else if (this.isInWrongDimension(mod) && !mod.getEntityTracker().entityFound(SheepEntity.class)) {
         return this.getToCorrectDimensionTask(mod);
      } else {
         return (Task)(mod.getItemStorage().hasItem(Items.SHEARS)
            ? new ShearSheepTask()
            : new KillAndLootTask(
               SheepEntity.class,
               entity -> !(entity instanceof SheepEntity sheep) ? false : this.colors.contains(sheep.getColor()) && !sheep.isSheared(),
               new ItemTarget(this.wools, this.count)
            ));
      }
   }

   @Override
   protected void onResourceStop(AltoClefController mod, Task interruptTask) {
   }

   @Override
   protected boolean isEqualResource(ResourceTask other) {
      return other instanceof CollectWoolTask && ((CollectWoolTask)other).count == this.count;
   }

   @Override
   protected String toDebugStringName() {
      return "Collect " + this.count + " wool.";
   }
}
