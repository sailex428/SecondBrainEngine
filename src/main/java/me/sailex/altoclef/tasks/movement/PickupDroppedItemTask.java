package me.sailex.altoclef.tasks.movement;

import me.sailex.altoclef.AltoClefController;
import me.sailex.altoclef.Debug;
import me.sailex.altoclef.tasks.AbstractDoToClosestObjectTask;
import me.sailex.altoclef.tasks.resources.SatisfyMiningRequirementTask;
import me.sailex.altoclef.tasks.slot.EnsureFreeInventorySlotTask;
import me.sailex.altoclef.tasksystem.ITaskRequiresGrounded;
import me.sailex.altoclef.tasksystem.Task;
import me.sailex.altoclef.util.ItemTarget;
import me.sailex.altoclef.util.MiningRequirement;
import me.sailex.altoclef.util.helpers.StlHelper;
import me.sailex.altoclef.util.helpers.StorageHelper;
import me.sailex.altoclef.util.helpers.WorldHelper;
import me.sailex.altoclef.util.progresscheck.MovementProgressChecker;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.FenceBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.FlowerBlock;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class PickupDroppedItemTask extends AbstractDoToClosestObjectTask<ItemEntity> implements ITaskRequiresGrounded {
   private static final Task getPickaxeFirstTask = new SatisfyMiningRequirementTask(MiningRequirement.STONE);
   private static boolean isGettingPickaxeFirstFlag = false;
   private final TimeoutWanderTask wanderTask = new TimeoutWanderTask(5.0F, true);
   private final MovementProgressChecker stuckCheck = new MovementProgressChecker();
   private final MovementProgressChecker progressChecker = new MovementProgressChecker();
   private final ItemTarget[] itemTargets;
   private final Set<ItemEntity> blacklist = new HashSet<>();
   private final boolean freeInventoryIfFull;
   Block[] annoyingBlocks = new Block[]{
      Blocks.VINE,
      Blocks.NETHER_SPROUTS,
      Blocks.CAVE_VINES,
      Blocks.CAVE_VINES_PLANT,
      Blocks.TWISTING_VINES,
      Blocks.TWISTING_VINES_PLANT,
      Blocks.WEEPING_VINES_PLANT,
      Blocks.LADDER,
      Blocks.BIG_DRIPLEAF,
      Blocks.BIG_DRIPLEAF_STEM,
      Blocks.SMALL_DRIPLEAF,
      Blocks.TALL_GRASS,
      Blocks.GRASS
   };
   private Task unstuckTask = null;
   private AltoClefController mod;
   private boolean collectingPickaxeForThisResource = false;
   private ItemEntity currentDrop = null;

   public PickupDroppedItemTask(ItemTarget[] itemTargets, boolean freeInventoryIfFull) {
      this.itemTargets = itemTargets;
      this.freeInventoryIfFull = freeInventoryIfFull;
   }

   public PickupDroppedItemTask(ItemTarget target, boolean freeInventoryIfFull) {
      this(new ItemTarget[]{target}, freeInventoryIfFull);
   }

   public PickupDroppedItemTask(Item item, int targetCount, boolean freeInventoryIfFull) {
      this(new ItemTarget(item, targetCount), freeInventoryIfFull);
   }

   public PickupDroppedItemTask(Item item, int targetCount) {
      this(item, targetCount, true);
   }

   private static BlockPos[] generateSides(BlockPos pos) {
      return new BlockPos[]{
         pos.add(1, 0, 0),
         pos.add(-1, 0, 0),
         pos.add(0, 0, 1),
         pos.add(0, 0, -1),
         pos.add(1, 0, -1),
         pos.add(1, 0, 1),
         pos.add(-1, 0, -1),
         pos.add(-1, 0, 1)
      };
   }

   public static boolean isIsGettingPickaxeFirst(AltoClefController mod) {
      return isGettingPickaxeFirstFlag && mod.getModSettings().shouldCollectPickaxeFirst();
   }

   private boolean isAnnoying(AltoClefController mod, BlockPos pos) {
      if (this.annoyingBlocks != null) {
         Block[] var3 = this.annoyingBlocks;
         int var4 = var3.length;
         byte var5 = 0;
         if (var5 < var4) {
            Block AnnoyingBlocks = var3[var5];
            return mod.getWorld().getBlockState(pos).getBlock() == AnnoyingBlocks
               || mod.getWorld().getBlockState(pos).getBlock() instanceof DoorBlock
               || mod.getWorld().getBlockState(pos).getBlock() instanceof FenceBlock
               || mod.getWorld().getBlockState(pos).getBlock() instanceof FenceGateBlock
               || mod.getWorld().getBlockState(pos).getBlock() instanceof FlowerBlock;
         }
      }

      return false;
   }

   private BlockPos stuckInBlock(AltoClefController mod) {
      BlockPos p = mod.getPlayer().getBlockPos();
      if (this.isAnnoying(mod, p)) {
         return p;
      } else if (this.isAnnoying(mod, p.up())) {
         return p.up();
      } else {
         BlockPos[] toCheck = generateSides(p);

         for (BlockPos check : toCheck) {
            if (this.isAnnoying(mod, check)) {
               return check;
            }
         }

         BlockPos[] toCheckHigh = generateSides(p.up());

         for (BlockPos checkx : toCheckHigh) {
            if (this.isAnnoying(mod, checkx)) {
               return checkx;
            }
         }

         return null;
      }
   }

   private Task getFenceUnstuckTask() {
      return new SafeRandomShimmyTask();
   }

   public boolean isCollectingPickaxeForThis() {
      return this.collectingPickaxeForThisResource;
   }

   @Override
   protected void onStart() {
      this.wanderTask.reset();
      this.progressChecker.reset();
      this.stuckCheck.reset();
   }

   @Override
   protected void onStop(Task interruptTask) {
   }

   @Override
   protected Task onTick() {
      if (this.wanderTask.isActive() && !this.wanderTask.isFinished()) {
         this.setDebugState("Wandering.");
         return this.wanderTask;
      } else {
         AltoClefController mod = this.controller;
         if (mod.getBaritone().getPathingBehavior().isPathing()) {
            this.progressChecker.reset();
         }

         if (this.unstuckTask != null && this.unstuckTask.isActive() && !this.unstuckTask.isFinished() && this.stuckInBlock(mod) != null) {
            this.setDebugState("Getting unstuck from block.");
            this.stuckCheck.reset();
            mod.getBaritone().getCustomGoalProcess().onLostControl();
            mod.getBaritone().getExploreProcess().onLostControl();
            return this.unstuckTask;
         } else {
            if (!this.progressChecker.check(mod) || !this.stuckCheck.check(mod)) {
               BlockPos blockStuck = this.stuckInBlock(mod);
               if (blockStuck != null) {
                  this.unstuckTask = this.getFenceUnstuckTask();
                  return this.unstuckTask;
               }

               this.stuckCheck.reset();
            }

            this.mod = mod;
            if (isIsGettingPickaxeFirst(mod)
               && this.collectingPickaxeForThisResource
               && !StorageHelper.miningRequirementMetInventory(this.controller, MiningRequirement.STONE)) {
               this.progressChecker.reset();
               this.setDebugState("Collecting pickaxe first");
               return getPickaxeFirstTask;
            } else {
               if (StorageHelper.miningRequirementMetInventory(this.controller, MiningRequirement.STONE)) {
                  isGettingPickaxeFirstFlag = false;
               }

               this.collectingPickaxeForThisResource = false;
               if (!this.progressChecker.check(mod)) {
                  mod.getBaritone().getPathingBehavior().forceCancel();
                  if (this.currentDrop != null && !this.currentDrop.getStack().isEmpty()) {
                     if (!isGettingPickaxeFirstFlag
                        && mod.getModSettings().shouldCollectPickaxeFirst()
                        && !StorageHelper.miningRequirementMetInventory(this.controller, MiningRequirement.STONE)) {
                        Debug.logMessage("Failed to pick up drop, will try to collect a stone pickaxe first and try again!");
                        this.collectingPickaxeForThisResource = true;
                        isGettingPickaxeFirstFlag = true;
                        return getPickaxeFirstTask;
                     }

                     Debug.logMessage(
                        StlHelper.toString(this.blacklist, element -> element == null ? "(null)" : element.getStack().getItem().getTranslationKey())
                     );
                     Debug.logMessage("Failed to pick up drop, suggesting it's unreachable.");
                     this.blacklist.add(this.currentDrop);
                     mod.getEntityTracker().requestEntityUnreachable(this.currentDrop);
                     return this.wanderTask;
                  }
               }

               return super.onTick();
            }
         }
      }
   }

   @Override
   protected boolean isEqual(Task other) {
      return !(other instanceof PickupDroppedItemTask task)
         ? false
         : Arrays.equals((Object[])task.itemTargets, (Object[])this.itemTargets) && task.freeInventoryIfFull == this.freeInventoryIfFull;
   }

   @Override
   protected String toDebugString() {
      StringBuilder result = new StringBuilder();
      result.append("Pickup Dropped Items: [");
      int c = 0;

      for (ItemTarget target : this.itemTargets) {
         result.append(target.toString());
         if (++c != this.itemTargets.length) {
            result.append(", ");
         }
      }

      result.append("]");
      return result.toString();
   }

   protected Vec3d getPos(AltoClefController mod, ItemEntity obj) {
      if (!obj.isOnGround() && !obj.isTouchingWater()) {
         BlockPos p = obj.getBlockPos();
         return !WorldHelper.isSolidBlock(this.controller, p.down(3)) ? obj.getPos().subtract(0.0, 2.0, 0.0) : obj.getPos().subtract(0.0, 1.0, 0.0);
      } else {
         return obj.getPos();
      }
   }

   @Override
   protected Optional<ItemEntity> getClosestTo(AltoClefController mod, Vec3d pos) {
      return mod.getEntityTracker().getClosestItemDrop(pos, this.itemTargets);
   }

   @Override
   protected Vec3d getOriginPos(AltoClefController mod) {
      return mod.getPlayer().getPos();
   }

   protected Task getGoalTask(ItemEntity itemEntity) {
      if (!itemEntity.equals(this.currentDrop)) {
         this.currentDrop = itemEntity;
         this.progressChecker.reset();
         if (isGettingPickaxeFirstFlag && this.collectingPickaxeForThisResource) {
            Debug.logMessage("New goal, no longer collecting a pickaxe.");
            this.collectingPickaxeForThisResource = false;
            isGettingPickaxeFirstFlag = false;
         }
      }

      boolean touching = this.mod.getEntityTracker().isCollidingWithPlayer(itemEntity);
      return (Task)(touching
            && this.freeInventoryIfFull
            && this.mod.getItemStorage().getSlotsThatCanFitInPlayerInventory(itemEntity.getStack(), false).isEmpty()
         ? new EnsureFreeInventorySlotTask()
         : new GetToEntityTask(itemEntity));
   }

   protected boolean isValid(AltoClefController mod, ItemEntity obj) {
      return obj.isAlive() && !this.blacklist.contains(obj);
   }
}
