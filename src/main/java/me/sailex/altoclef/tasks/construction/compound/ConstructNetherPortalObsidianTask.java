package me.sailex.altoclef.tasks.construction.compound;

import me.sailex.altoclef.AltoClefController;
import me.sailex.altoclef.Debug;
import me.sailex.altoclef.TaskCatalogue;
import me.sailex.altoclef.tasks.InteractWithBlockTask;
import me.sailex.altoclef.tasks.construction.DestroyBlockTask;
import me.sailex.altoclef.tasks.construction.PlaceBlockTask;
import me.sailex.altoclef.tasks.construction.PlaceStructureBlockTask;
import me.sailex.altoclef.tasks.movement.TimeoutWanderTask;
import me.sailex.altoclef.tasksystem.Task;
import me.sailex.altoclef.util.ItemTarget;
import me.sailex.altoclef.util.helpers.WorldHelper;
import me.sailex.altoclef.util.time.TimerGame;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;

import java.util.LinkedList;
import java.util.function.Predicate;

public class ConstructNetherPortalObsidianTask extends Task {
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
   private static final Vec3i PORTALABLE_REGION_SIZE = new Vec3i(3, 6, 6);
   private final TimerGame areaSearchTimer = new TimerGame(5.0);
   private BlockPos origin;
   private BlockPos destroyTarget;

   private BlockPos getBuildableAreaNearby(AltoClefController mod) {
      BlockPos checkOrigin = mod.getPlayer().getBlockPos();

      for (BlockPos toCheck : WorldHelper.scanRegion(checkOrigin, checkOrigin.add(PORTALABLE_REGION_SIZE))) {
         if (this.controller.getWorld() == null) {
            return null;
         }

         BlockState state = this.controller.getWorld().getBlockState(toCheck);
         boolean validToWorld = WorldHelper.canPlace(this.controller, toCheck) || WorldHelper.canBreak(this.controller, toCheck);
         if (!validToWorld || state.getBlock() == Blocks.LAVA || state.getBlock() == Blocks.WATER || state.getBlock() == Blocks.BEDROCK) {
            return null;
         }
      }

      return checkOrigin;
   }

   @Override
   protected void onStart() {
      AltoClefController mod = this.controller;
      mod.getBehaviour().push();
      mod.getBehaviour().avoidBlockBreaking((Predicate<BlockPos>)(block -> {
         if (this.origin != null) {
            for (Vec3i framePosRelative : PORTAL_FRAME) {
               BlockPos framePos = this.origin.add(framePosRelative);
               if (block.equals(framePos)) {
                  return mod.getWorld().getBlockState(framePos).getBlock() == Blocks.OBSIDIAN;
               }
            }
         }

         return false;
      }));
      mod.getBehaviour().addProtectedItems(Items.FLINT_AND_STEEL);
   }

   @Override
   protected Task onTick() {
      AltoClefController mod = this.controller;
      if (this.origin != null && mod.getWorld().getBlockState(this.origin.up()).getBlock() == Blocks.NETHER_PORTAL) {
         this.setDebugState("Done constructing nether portal.");
         mod.getBlockScanner().addBlock(Blocks.NETHER_PORTAL, this.origin.up());
         return null;
      } else {
         int neededObsidian = 10;
         BlockPos placeTarget = null;
         if (this.origin != null) {
            for (Vec3i frameOffs : PORTAL_FRAME) {
               BlockPos framePos = this.origin.add(frameOffs);
               if (!mod.getBlockScanner().isBlockAtPosition(framePos, Blocks.OBSIDIAN)) {
                  placeTarget = framePos;
                  break;
               }

               neededObsidian--;
            }
         }

         if (mod.getItemStorage().getItemCount(Items.OBSIDIAN) < neededObsidian) {
            this.setDebugState("Getting obsidian");
            return TaskCatalogue.getItemTask(Items.OBSIDIAN, neededObsidian);
         } else if (this.origin == null) {
            if (this.areaSearchTimer.elapsed()) {
               this.areaSearchTimer.reset();
               Debug.logMessage("(Searching for area to build portal nearby...)");
               this.origin = this.getBuildableAreaNearby(mod);
            }

            this.setDebugState("Looking for portalable area...");
            return new TimeoutWanderTask();
         } else if (!mod.getItemStorage().hasItem(Items.FLINT_AND_STEEL)) {
            this.setDebugState("Getting flint and steel");
            return TaskCatalogue.getItemTask(Items.FLINT_AND_STEEL, 1);
         } else if (placeTarget == null) {
            if (this.destroyTarget != null && !WorldHelper.isAir(this.controller.getWorld().getBlockState(this.destroyTarget).getBlock())) {
               return new DestroyBlockTask(this.destroyTarget);
            } else {
               for (Vec3i middleOffs : PORTAL_INTERIOR) {
                  BlockPos middlePos = this.origin.add(middleOffs);
                  if (!WorldHelper.isAir(this.controller.getWorld().getBlockState(middlePos).getBlock())) {
                     this.destroyTarget = middlePos;
                     return new DestroyBlockTask(this.destroyTarget);
                  }
               }

               return new InteractWithBlockTask(new ItemTarget(Items.FLINT_AND_STEEL, 1), Direction.UP, this.origin.down(), true);
            }
         } else {
            World clientWorld = mod.getWorld();
            if (this.surroundedByAir(clientWorld, placeTarget)) {
               LinkedList<BlockPos> queue = new LinkedList<>();
               queue.add(placeTarget);

               while (this.surroundedByAir(clientWorld, placeTarget)) {
                  BlockPos pos = queue.removeFirst();
                  if (!this.surroundedByAir(clientWorld, pos)) {
                     return new PlaceStructureBlockTask(pos);
                  }

                  queue.add(pos.up());
                  queue.add(pos.down());
                  queue.add(pos.east());
                  queue.add(pos.west());
                  queue.add(pos.north());
                  queue.add(pos.south());
               }

               mod.logWarning("Did not find any block to place obsidian on");
            }

            if (!clientWorld.getBlockState(placeTarget).isAir() && !clientWorld.getBlockState(placeTarget).getBlock().equals(Blocks.OBSIDIAN)) {
               return new DestroyBlockTask(placeTarget);
            } else {
               this.setDebugState("Placing frame...");
               return new PlaceBlockTask(placeTarget, Blocks.OBSIDIAN);
            }
         }
      }
   }

   private boolean surroundedByAir(World world, BlockPos pos) {
      return world.getBlockState(pos.west()).isAir()
         && world.getBlockState(pos.south()).isAir()
         && world.getBlockState(pos.east()).isAir()
         && world.getBlockState(pos.up()).isAir()
         && world.getBlockState(pos.down()).isAir()
         && world.getBlockState(pos.north()).isAir();
   }

   @Override
   protected void onStop(Task interruptTask) {
      this.controller.getBehaviour().pop();
   }

   @Override
   protected boolean isEqual(Task other) {
      return other instanceof ConstructNetherPortalObsidianTask;
   }

   @Override
   protected String toDebugString() {
      return "Building nether portal with obsidian";
   }
}
