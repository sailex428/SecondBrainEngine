package adris.altoclef.tasks.resources;

import adris.altoclef.AltoClefController;
import adris.altoclef.Debug;
import adris.altoclef.multiversion.ToolMaterialVer;
import adris.altoclef.multiversion.blockpos.BlockPosVer;
import adris.altoclef.tasks.AbstractDoToClosestObjectTask;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasks.construction.DestroyBlockTask;
import adris.altoclef.tasks.movement.PickupDroppedItemTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.MiningRequirement;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.helpers.WorldHelper;
import adris.altoclef.util.progresscheck.MovementProgressChecker;
import adris.altoclef.util.slots.CursorSlot;
import adris.altoclef.util.slots.PlayerSlot;
import adris.altoclef.util.time.TimerGame;
import java.util.*;
import net.minecraft.block.Block;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.MiningToolItem;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class MineAndCollectTask extends ResourceTask {
   private final Block[] blocksToMine;
   private final MiningRequirement requirement;
   private final TimerGame cursorStackTimer = new TimerGame(3.0);
   private final MineOrCollectTask subtask;

   public MineAndCollectTask(ItemTarget[] itemTargets, Block[] blocksToMine, MiningRequirement requirement) {
      super(itemTargets);
      this.requirement = requirement;
      this.blocksToMine = blocksToMine;
      this.subtask = new MineOrCollectTask(this.blocksToMine, itemTargets);
   }

   public MineAndCollectTask(ItemTarget[] blocksToMine, MiningRequirement requirement) {
      this(blocksToMine, itemTargetToBlockList(blocksToMine), requirement);
   }

   public MineAndCollectTask(ItemTarget target, Block[] blocksToMine, MiningRequirement requirement) {
      this(new ItemTarget[]{target}, blocksToMine, requirement);
   }

   public MineAndCollectTask(Item item, int count, Block[] blocksToMine, MiningRequirement requirement) {
      this(new ItemTarget(item, count), blocksToMine, requirement);
   }

   public static Block[] itemTargetToBlockList(ItemTarget[] targets) {
      List<Block> result = new ArrayList<>(targets.length);

      for (ItemTarget target : targets) {
         for (Item item : target.getMatches()) {
            Block block = Block.getBlockFromItem(item);
            if (block != null && !WorldHelper.isAir(block)) {
               result.add(block);
            }
         }
      }

      return result.toArray(Block[]::new);
   }

   @Override
   protected void onResourceStart(AltoClefController mod) {
      mod.getBehaviour().push();
      mod.getBehaviour().addProtectedItems(Items.WOODEN_PICKAXE, Items.STONE_PICKAXE, Items.IRON_PICKAXE, Items.DIAMOND_PICKAXE, Items.NETHERITE_PICKAXE);
      this.subtask.resetSearch();
   }

   @Override
   protected boolean shouldAvoidPickingUp(AltoClefController mod) {
      return true;
   }

   @Override
   protected Task onResourceTick(AltoClefController mod) {
      if (!StorageHelper.miningRequirementMet(mod, this.requirement)) {
         return new SatisfyMiningRequirementTask(this.requirement);
      } else {
         if (this.subtask.isMining()) {
            this.makeSureToolIsEquipped(mod);
         }

         return (Task)(this.subtask.wasWandering() && this.isInWrongDimension(mod) && !mod.getBlockScanner().anyFound(this.blocksToMine)
            ? this.getToCorrectDimensionTask(mod)
            : this.subtask);
      }
   }

   @Override
   protected void onResourceStop(AltoClefController mod, Task interruptTask) {
      mod.getBehaviour().pop();
   }

   @Override
   protected boolean isEqualResource(ResourceTask other) {
      return other instanceof MineAndCollectTask task ? Arrays.equals((Object[])task.blocksToMine, (Object[])this.blocksToMine) : false;
   }

   @Override
   protected String toDebugStringName() {
      return "Mine And Collect";
   }

   private void makeSureToolIsEquipped(AltoClefController mod) {
      if (this.cursorStackTimer.elapsed() && !mod.getFoodChain().needsToEat()) {
         assert this.controller.getPlayer() != null;

         ItemStack cursorStack = StorageHelper.getItemStackInCursorSlot(this.controller);
         if (cursorStack != null && !cursorStack.isEmpty()) {
            Item item = cursorStack.getItem();
            if (item.isSuitableFor(mod.getWorld().getBlockState(this.subtask.miningPos()))) {
               Item currentlyEquipped = StorageHelper.getItemStackInSlot(PlayerSlot.getEquipSlot(mod.getInventory())).getItem();
               if (item instanceof MiningToolItem) {
                  if (currentlyEquipped instanceof MiningToolItem currentPick) {
                     MiningToolItem swapPick = (MiningToolItem)item;
                     if (ToolMaterialVer.getMiningLevel(swapPick) > ToolMaterialVer.getMiningLevel(currentPick)) {
                        mod.getSlotHandler().forceEquipSlot(this.controller, CursorSlot.SLOT);
                     }
                  } else {
                     mod.getSlotHandler().forceEquipSlot(this.controller, CursorSlot.SLOT);
                  }
               }
            }
         }

         this.cursorStackTimer.reset();
      }
   }

   public static class MineOrCollectTask extends AbstractDoToClosestObjectTask<Object> {
      private final Block[] blocks;
      private final ItemTarget[] targets;
      private final Set<BlockPos> blacklist = new HashSet<>();
      private final MovementProgressChecker progressChecker = new MovementProgressChecker();
      private final Task pickupTask;
      private BlockPos miningPos;

      public MineOrCollectTask(Block[] blocks, ItemTarget[] targets) {
         this.blocks = blocks;
         this.targets = targets;
         this.pickupTask = new PickupDroppedItemTask(targets, true);
      }

      @Override
      protected Vec3d getPos(AltoClefController mod, Object obj) {
         if (obj instanceof BlockPos b) {
            return WorldHelper.toVec3d(b);
         } else if (obj instanceof ItemEntity item) {
            return item.getPos();
         } else {
            throw new UnsupportedOperationException(
               "Shouldn't try to get the position of object " + obj + " of type " + (obj != null ? obj.getClass().toString() : "(null object)")
            );
         }
      }

      @Override
      protected Optional<Object> getClosestTo(AltoClefController mod, Vec3d pos) {
         Pair<Double, Optional<BlockPos>> closestBlock = getClosestBlock(mod, pos, this.blocks);
         Pair<Double, Optional<ItemEntity>> closestDrop = getClosestItemDrop(mod, pos, this.targets);
         double blockSq = (Double)closestBlock.getLeft();
         double dropSq = (Double)closestDrop.getLeft();
         if (mod.getExtraBaritoneSettings().isInteractionPaused()) {
            return ((Optional)closestDrop.getRight()).map(Object.class::cast);
         } else {
            return dropSq <= blockSq ? ((Optional)closestDrop.getRight()).map(Object.class::cast) : ((Optional)closestBlock.getRight()).map(Object.class::cast);
         }
      }

      public static Pair<Double, Optional<ItemEntity>> getClosestItemDrop(AltoClefController mod, Vec3d pos, ItemTarget... items) {
         Optional<ItemEntity> closestDrop = Optional.empty();
         if (mod.getEntityTracker().itemDropped(items)) {
            closestDrop = mod.getEntityTracker().getClosestItemDrop(pos, items);
         }

         return new Pair(closestDrop.<Double>map(itemEntity -> itemEntity.squaredDistanceTo(pos) + 10.0).orElse(Double.POSITIVE_INFINITY), closestDrop);
      }

      public static Pair<Double, Optional<BlockPos>> getClosestBlock(AltoClefController mod, Vec3d pos, Block... blocks) {
         Optional<BlockPos> closestBlock = mod.getBlockScanner()
            .getNearestBlock(pos, check -> mod.getBlockScanner().isUnreachable(check) ? false : WorldHelper.canBreak(mod, check), blocks);
         return new Pair(closestBlock.<Double>map(blockPos -> BlockPosVer.getSquaredDistance(blockPos, pos)).orElse(Double.POSITIVE_INFINITY), closestBlock);
      }

      @Override
      protected Vec3d getOriginPos(AltoClefController mod) {
         return mod.getPlayer().getPos();
      }

      @Override
      protected Task onTick() {
         AltoClefController mod = this.controller;
         if (mod.getBaritone().getPathingBehavior().isPathing()) {
            this.progressChecker.reset();
         }

         if (this.miningPos != null && !this.progressChecker.check(mod)) {
            mod.getBaritone().getPathingBehavior().forceCancel();
            Debug.logMessage("Failed to mine block. Suggesting it may be unreachable.");
            mod.getBlockScanner().requestBlockUnreachable(this.miningPos, 2);
            this.blacklist.add(this.miningPos);
            this.miningPos = null;
            this.progressChecker.reset();
         }

         return super.onTick();
      }

      @Override
      protected Task getGoalTask(Object obj) {
         if (!(obj instanceof BlockPos newPos)) {
            if (obj instanceof ItemEntity) {
               this.miningPos = null;
               return this.pickupTask;
            } else {
               throw new UnsupportedOperationException(
                  "Shouldn't try to get the goal from object " + obj + " of type " + (obj != null ? obj.getClass().toString() : "(null object)")
               );
            }
         } else {
            if (this.miningPos == null || !this.miningPos.equals(newPos)) {
               this.progressChecker.reset();
            }

            this.miningPos = newPos;
            return new DestroyBlockTask(this.miningPos);
         }
      }

      @Override
      protected boolean isValid(AltoClefController mod, Object obj) {
         if (obj instanceof BlockPos b) {
            return mod.getBlockScanner().isBlockAtPosition(b, this.blocks) && WorldHelper.canBreak(this.controller, b);
         } else if (!(obj instanceof ItemEntity drop)) {
            return false;
         } else {
            Item item = drop.getStack().getItem();
            if (this.targets != null) {
               for (ItemTarget target : this.targets) {
                  if (target.matches(item)) {
                     return true;
                  }
               }
            }

            return false;
         }
      }

      @Override
      protected void onStart() {
         this.progressChecker.reset();
         this.miningPos = null;
      }

      @Override
      protected void onStop(Task interruptTask) {
      }

      @Override
      protected boolean isEqual(Task other) {
         return !(other instanceof MineOrCollectTask task)
            ? false
            : Arrays.equals((Object[])task.blocks, (Object[])this.blocks) && Arrays.equals((Object[])task.targets, (Object[])this.targets);
      }

      @Override
      protected String toDebugString() {
         return "Mining or Collecting";
      }

      public boolean isMining() {
         return this.miningPos != null;
      }

      public BlockPos miningPos() {
         return this.miningPos;
      }
   }
}
