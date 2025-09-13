package me.sailex.altoclef.tasks.speedrun.beatgame.prioritytask.tasks;

import me.sailex.altoclef.AltoClefController;
import me.sailex.altoclef.tasks.resources.MineAndCollectTask;
import me.sailex.altoclef.tasks.speedrun.beatgame.prioritytask.prioritycalculators.DistancePriorityCalculator;
import me.sailex.altoclef.tasksystem.Task;
import me.sailex.altoclef.util.ItemTarget;
import me.sailex.altoclef.util.MiningRequirement;
import me.sailex.altoclef.util.helpers.StorageHelper;
import net.minecraft.block.Block;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;

public class MineBlockPriorityTask extends PriorityTask {
   public final Block[] toMine;
   public final Item[] droppedItem;
   public final ItemTarget[] droppedItemTargets;
   private final MiningRequirement miningRequirement;
   private final DistancePriorityCalculator prioritySupplier;

   public MineBlockPriorityTask(Block[] toMine, Item[] droppedItem, MiningRequirement miningRequirement, DistancePriorityCalculator prioritySupplier) {
      this(toMine, droppedItem, miningRequirement, prioritySupplier, false, true, false);
   }

   public MineBlockPriorityTask(
      Block[] toMine,
      Item[] droppedItem,
      MiningRequirement miningRequirement,
      DistancePriorityCalculator prioritySupplier,
      Function<AltoClefController, Boolean> canCall
   ) {
      this(toMine, droppedItem, miningRequirement, prioritySupplier, canCall, false, true, false);
   }

   public MineBlockPriorityTask(
      Block[] toMine,
      Item[] droppedItem,
      MiningRequirement miningRequirement,
      DistancePriorityCalculator prioritySupplier,
      boolean shouldForce,
      boolean canCache,
      boolean bypassForceCooldown
   ) {
      this(toMine, droppedItem, miningRequirement, prioritySupplier, mod -> true, shouldForce, canCache, bypassForceCooldown);
   }

   public MineBlockPriorityTask(
      Block[] toMine,
      Item[] droppedItem,
      MiningRequirement miningRequirement,
      DistancePriorityCalculator prioritySupplier,
      Function<AltoClefController, Boolean> canCall,
      boolean shouldForce,
      boolean canCache,
      boolean bypassForceCooldown
   ) {
      super(canCall, shouldForce, canCache, bypassForceCooldown);
      this.toMine = toMine;
      this.droppedItem = droppedItem;
      this.droppedItemTargets = ItemTarget.of(droppedItem);
      this.miningRequirement = miningRequirement;
      this.prioritySupplier = prioritySupplier;
   }

   @Override
   public Task getTask(AltoClefController mod) {
      return new MineAndCollectTask(this.droppedItemTargets, this.toMine, this.miningRequirement);
   }

   @Override
   public String getDebugString() {
      return "Gathering resource: " + Arrays.toString((Object[])this.droppedItem);
   }

   @Override
   protected double getPriority(AltoClefController mod) {
      if (!StorageHelper.miningRequirementMet(mod, this.miningRequirement)) {
         return Double.NEGATIVE_INFINITY;
      } else {
         double closestDist = this.getClosestDist(mod);
         int itemCount = mod.getItemStorage().getItemCount(this.droppedItem);
         this.prioritySupplier.update(itemCount);
         return this.prioritySupplier.getPriority(closestDist);
      }
   }

   private double getClosestDist(AltoClefController mod) {
      Vec3d pos = mod.getPlayer().getPos();
      Pair<Double, Optional<BlockPos>> closestBlock = MineAndCollectTask.MineOrCollectTask.getClosestBlock(mod, pos, this.toMine);
      Pair<Double, Optional<ItemEntity>> closestDrop = MineAndCollectTask.MineOrCollectTask.getClosestItemDrop(mod, pos, this.droppedItemTargets);
      return Math.min((Double)closestBlock.getLeft(), (Double)closestDrop.getLeft());
   }
}
