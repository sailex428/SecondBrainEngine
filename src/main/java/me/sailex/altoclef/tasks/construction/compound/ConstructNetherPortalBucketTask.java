package me.sailex.altoclef.tasks.construction.compound;

import me.sailex.altoclef.AltoClefController;
import me.sailex.altoclef.Debug;
import me.sailex.altoclef.TaskCatalogue;
import me.sailex.altoclef.tasks.InteractWithBlockTask;
import me.sailex.altoclef.tasks.construction.ClearLiquidTask;
import me.sailex.altoclef.tasks.construction.DestroyBlockTask;
import me.sailex.altoclef.tasks.construction.PlaceObsidianBucketTask;
import me.sailex.altoclef.tasks.movement.GetWithinRangeOfBlockTask;
import me.sailex.altoclef.tasks.movement.PickupDroppedItemTask;
import me.sailex.altoclef.tasks.movement.TimeoutWanderTask;
import me.sailex.altoclef.tasks.speedrun.beatgame.BeatMinecraftTask;
import me.sailex.altoclef.tasksystem.Task;
import me.sailex.altoclef.util.ItemTarget;
import me.sailex.altoclef.util.helpers.WorldHelper;
import me.sailex.altoclef.util.progresscheck.MovementProgressChecker;
import me.sailex.altoclef.util.time.TimerGame;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.function.Predicate;

public class ConstructNetherPortalBucketTask extends Task {
   private static final Vec3i[] PORTAL_FRAME = new Vec3i[]{
      new Vec3i(0, 0, -1),
      new Vec3i(0, 1, -1),
      new Vec3i(0, 2, -1),
      new Vec3i(0, 0, 2),
      new Vec3i(0, 1, 2),
      new Vec3i(0, 2, 2),
      new Vec3i(0, 3, 0),
      new Vec3i(0, 3, 1),
      new Vec3i(0, -1, 0),
      new Vec3i(0, -1, 1)
   };
   private static final Vec3i[] PORTAL_INTERIOR = new Vec3i[]{
      new Vec3i(0, 0, 0),
      new Vec3i(0, 1, 0),
      new Vec3i(0, 2, 0),
      new Vec3i(0, 0, 1),
      new Vec3i(0, 1, 1),
      new Vec3i(0, 2, 1),
      new Vec3i(1, 0, 0),
      new Vec3i(1, 1, 0),
      new Vec3i(1, 2, 0),
      new Vec3i(1, 0, 1),
      new Vec3i(1, 1, 1),
      new Vec3i(1, 2, 1),
      new Vec3i(-1, 0, 0),
      new Vec3i(-1, 1, 0),
      new Vec3i(-1, 2, 0),
      new Vec3i(-1, 0, 1),
      new Vec3i(-1, 1, 1),
      new Vec3i(-1, 2, 1)
   };
   private static final Vec3i PORTALABLE_REGION_SIZE = new Vec3i(4, 6, 6);
   private static final Vec3i PORTAL_ORIGIN_RELATIVE_TO_REGION = new Vec3i(1, 0, 2);
   private final TimerGame lavaSearchTimer = new TimerGame(5.0);
   private final MovementProgressChecker progressChecker = new MovementProgressChecker();
   private final TimeoutWanderTask wanderTask = new TimeoutWanderTask(5.0F);
   private final Task collectLavaTask = TaskCatalogue.getItemTask(Items.LAVA_BUCKET, 1);
   private final TimerGame refreshTimer = new TimerGame(11.0);
   private BlockPos portalOrigin = null;
   private Task getToLakeTask = null;
   private BlockPos currentDestroyTarget = null;
   private boolean firstSearch = false;

   @Override
   protected void onStart() {
      this.currentDestroyTarget = null;
      AltoClefController mod = this.controller;
      mod.getBehaviour().push();
      mod.getBehaviour().avoidBlockBreaking((Predicate<BlockPos>)(block -> {
         if (this.portalOrigin != null) {
            for (Vec3i framePosRelative : PORTAL_FRAME) {
               BlockPos framePos = this.portalOrigin.add(framePosRelative);
               if (block.equals(framePos)) {
                  return mod.getWorld().getBlockState(framePos).getBlock() == Blocks.OBSIDIAN;
               }
            }
         }

         return false;
      }));
      mod.getBehaviour().addProtectedItems(Items.WATER_BUCKET, Items.LAVA_BUCKET, Items.FLINT_AND_STEEL, Items.FIRE_CHARGE);
      this.progressChecker.reset();
   }

   @Override
   protected Task onTick() {
      AltoClefController mod = this.controller;
      if (this.portalOrigin != null && mod.getWorld().getBlockState(this.portalOrigin.up()).getBlock() == Blocks.NETHER_PORTAL) {
         this.setDebugState("Done constructing nether portal.");
         mod.getBlockScanner().addBlock(Blocks.NETHER_PORTAL, this.portalOrigin.up());
         return null;
      } else {
         if (mod.getBaritone().getPathingBehavior().isPathing()) {
            this.progressChecker.reset();
         }

         if (this.wanderTask.isActive() && !this.wanderTask.isFinished()) {
            this.setDebugState("Trying again.");
            this.progressChecker.reset();
            return this.wanderTask;
         } else {
            if (!this.progressChecker.check(mod)) {
               mod.getBaritone().getPathingBehavior().forceCancel();
               if (this.portalOrigin != null && this.currentDestroyTarget != null) {
                  mod.getBlockScanner().requestBlockUnreachable(this.portalOrigin);
                  mod.getBlockScanner().requestBlockUnreachable(this.currentDestroyTarget);
                  if (mod.getBlockScanner().isUnreachable(this.portalOrigin) && mod.getBlockScanner().isUnreachable(this.currentDestroyTarget)) {
                     this.portalOrigin = null;
                     this.currentDestroyTarget = null;
                  }

                  return this.wanderTask;
               }
            }

            if (this.refreshTimer.elapsed()) {
               Debug.logMessage("Duct tape: Refreshing inventory again just in case");
               this.refreshTimer.reset();
            }

            if (this.portalOrigin != null && !this.portalOrigin.isWithinDistance(mod.getPlayer().getPos(), 2000.0)) {
               this.portalOrigin = null;
               this.currentDestroyTarget = null;
            }

            if (this.currentDestroyTarget != null) {
               if (WorldHelper.isSolidBlock(this.controller, this.currentDestroyTarget)) {
                  return new DestroyBlockTask(this.currentDestroyTarget);
               }

               this.currentDestroyTarget = null;
            }

            if (!mod.getItemStorage().hasItem(Items.FLINT_AND_STEEL) && !mod.getItemStorage().hasItem(Items.FIRE_CHARGE)) {
               this.setDebugState("Getting flint & steel");
               this.progressChecker.reset();
               return TaskCatalogue.getItemTask(Items.FLINT_AND_STEEL, 1);
            } else {
               int bucketCount = mod.getItemStorage().getItemCount(Items.BUCKET, Items.LAVA_BUCKET, Items.WATER_BUCKET);
               if (bucketCount < 2) {
                  this.setDebugState("Getting buckets");
                  this.progressChecker.reset();
                  if (mod.getItemStorage().hasItem(Items.LAVA_BUCKET)) {
                     return TaskCatalogue.getItemTask(Items.WATER_BUCKET, 1);
                  } else if (mod.getItemStorage().hasItem(Items.WATER_BUCKET)) {
                     return TaskCatalogue.getItemTask(Items.LAVA_BUCKET, 1);
                  } else {
                     return (Task)(mod.getEntityTracker().itemDropped(Items.WATER_BUCKET, Items.LAVA_BUCKET)
                        ? new PickupDroppedItemTask(new ItemTarget(new Item[]{Items.WATER_BUCKET, Items.LAVA_BUCKET}, 1), true)
                        : TaskCatalogue.getItemTask(Items.BUCKET, 2));
                  }
               } else {
                  boolean needsToLookForPortal = this.portalOrigin == null;
                  if (needsToLookForPortal) {
                     this.progressChecker.reset();
                     if (!mod.getItemStorage().hasItem(Items.WATER_BUCKET)) {
                        this.setDebugState("Getting water");
                        this.progressChecker.reset();
                        return TaskCatalogue.getItemTask(Items.WATER_BUCKET, 1);
                     }

                     boolean foundSpot = false;
                     if (this.firstSearch || this.lavaSearchTimer.elapsed()) {
                        this.firstSearch = false;
                        this.lavaSearchTimer.reset();
                        Debug.logMessage("(Searching for lava lake with portalable spot nearby...)");
                        BlockPos lavaPos = this.findLavaLake(mod, mod.getPlayer().getBlockPos());
                        if (lavaPos != null) {
                           BlockPos foundPortalRegion = this.getPortalableRegion(
                              mod, lavaPos, mod.getPlayer().getBlockPos(), new Vec3i(-1, 0, 0), PORTALABLE_REGION_SIZE, 20
                           );
                           if (foundPortalRegion != null) {
                              this.portalOrigin = foundPortalRegion.add(PORTAL_ORIGIN_RELATIVE_TO_REGION);
                              foundSpot = true;
                              this.getToLakeTask = new GetWithinRangeOfBlockTask(this.portalOrigin, 7);
                              return this.getToLakeTask;
                           }

                           Debug.logWarning("Failed to find portalable region nearby. Consider increasing the search timeout range");
                        } else {
                           Debug.logMessage("(lava lake not found)");
                        }
                     }

                     if (!foundSpot) {
                        this.setDebugState("(timeout: Looking for lava lake)");
                        return new TimeoutWanderTask();
                     }
                  }

                  if (BeatMinecraftTask.isTaskRunning(mod, this.getToLakeTask)) {
                     return this.getToLakeTask;
                  } else {
                     for (Vec3i framePosRelative : PORTAL_FRAME) {
                        BlockPos framePos = this.portalOrigin.add(framePosRelative);
                        Block frameBlock = mod.getWorld().getBlockState(framePos).getBlock();
                        if (frameBlock != Blocks.OBSIDIAN) {
                           if (!mod.getItemStorage().hasItem(Items.LAVA_BUCKET) && frameBlock != Blocks.LAVA) {
                              this.setDebugState("Collecting lava");
                              this.progressChecker.reset();
                              return this.collectLavaTask;
                           }

                           if (mod.getBlockScanner().isUnreachable(framePos)) {
                              this.portalOrigin = null;
                           }

                           return new PlaceObsidianBucketTask(framePos);
                        }

                        BlockPos waterCheck = framePos.up();
                        if (mod.getWorld().getBlockState(waterCheck).getBlock() == Blocks.WATER && WorldHelper.isSourceBlock(this.controller, waterCheck, true)
                           )
                         {
                           this.setDebugState("Clearing water from cast");
                           return new ClearLiquidTask(waterCheck);
                        }
                     }

                     for (Vec3i offs : PORTAL_INTERIOR) {
                        BlockPos p = this.portalOrigin.add(offs);

                        assert this.controller.getWorld() != null;

                        if (!this.controller.getWorld().getBlockState(p).isAir()) {
                           this.setDebugState("Clearing inside of portal");
                           this.currentDestroyTarget = p;
                           return null;
                        }
                     }

                     this.setDebugState("Flinting and Steeling");
                     return new InteractWithBlockTask(
                        new ItemTarget(new Item[]{Items.FLINT_AND_STEEL, Items.FIRE_CHARGE}, 1), Direction.UP, this.portalOrigin.down(), true
                     );
                  }
               }
            }
         }
      }
   }

   @Override
   protected void onStop(Task interruptTask) {
      this.controller.getBehaviour().pop();
   }

   @Override
   protected boolean isEqual(Task other) {
      return other instanceof ConstructNetherPortalBucketTask;
   }

   @Override
   protected String toDebugString() {
      return "Construct Nether Portal";
   }

   private BlockPos findLavaLake(AltoClefController mod, BlockPos playerPos) {
      HashSet<BlockPos> alreadyExplored = new HashSet<>();
      double nearestSqDistance = Double.POSITIVE_INFINITY;
      BlockPos nearestLake = null;
      List<BlockPos> lavas = mod.getBlockScanner().getKnownLocations(Blocks.LAVA);
      if (!lavas.isEmpty()) {
         for (BlockPos pos : lavas) {
            if (!alreadyExplored.contains(pos)) {
               double sqDist = playerPos.getSquaredDistance(pos);
               if (sqDist < nearestSqDistance) {
                  int depth = this.getNumberOfBlocksAdjacent(alreadyExplored, pos);
                  if (depth != 0) {
                     Debug.logMessage("Found with depth " + depth);
                     if (depth >= 12) {
                        nearestSqDistance = sqDist;
                        nearestLake = pos;
                     }
                  }
               }
            }
         }
      }

      return nearestLake;
   }

   private int getNumberOfBlocksAdjacent(HashSet<BlockPos> alreadyExplored, BlockPos start) {
      Queue<BlockPos> queue = new ArrayDeque<>();
      queue.add(start);
      int bonus = 0;

      while (!queue.isEmpty()) {
         BlockPos origin = queue.poll();
         if (!alreadyExplored.contains(origin)) {
            alreadyExplored.add(origin);

            assert this.controller.getWorld() != null;

            BlockState s = this.controller.getWorld().getBlockState(origin);
            if (s.getBlock() == Blocks.LAVA && s.getFluidState().isStill()) {
               int level = s.getFluidState().getLevel();
               if (level == 8) {
                  queue.addAll(List.of(origin.north(), origin.south(), origin.east(), origin.west(), origin.up(), origin.down()));
                  bonus++;
               }
            }
         }
      }

      return bonus;
   }

   private BlockPos getPortalableRegion(AltoClefController mod, BlockPos lava, BlockPos playerPos, Vec3i sizeOffset, Vec3i sizeAllocation, int timeoutRange) {
      Vec3i[] directions = new Vec3i[]{new Vec3i(1, 0, 0), new Vec3i(-1, 0, 0), new Vec3i(0, 0, 1), new Vec3i(0, 0, -1)};
      double minDistanceToPlayer = Double.POSITIVE_INFINITY;
      BlockPos bestPos = null;

      for (Vec3i direction : directions) {
         for (int offs = 1; offs < timeoutRange; offs++) {
            Vec3i offset = new Vec3i(direction.getX() * offs, direction.getY() * offs, direction.getZ() * offs);
            boolean found = true;
            boolean solidFound = false;

            label73:
            for (int dx = -1; dx < sizeAllocation.getX() + 1; dx++) {
               for (int dz = -1; dz < sizeAllocation.getZ() + 1; dz++) {
                  for (int dy = -1; dy < sizeAllocation.getY(); dy++) {
                     BlockPos toCheck = lava.add(offset).add(sizeOffset).add(dx, dy, dz);

                     assert this.controller.getWorld() != null;

                     BlockState state = this.controller.getWorld().getBlockState(toCheck);
                     if (state.getBlock() == Blocks.LAVA || state.getBlock() == Blocks.WATER || state.getBlock() == Blocks.BEDROCK) {
                        found = false;
                        break label73;
                     }

                     if (dy <= 1 && !solidFound && WorldHelper.isSolidBlock(this.controller, toCheck)) {
                        solidFound = true;
                     }
                  }
               }
            }

            if (!solidFound) {
               break;
            }

            if (found) {
               BlockPos foundBoxCorner = lava.add(offset).add(sizeOffset);
               double sqDistance = foundBoxCorner.getSquaredDistance(playerPos);
               if (sqDistance < minDistanceToPlayer) {
                  minDistanceToPlayer = sqDistance;
                  bestPos = foundBoxCorner;
               }
               break;
            }
         }
      }

      return bestPos;
   }
}
