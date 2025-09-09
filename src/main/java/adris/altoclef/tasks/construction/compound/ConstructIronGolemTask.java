package adris.altoclef.tasks.construction.compound;

import adris.altoclef.AltoClefController;
import adris.altoclef.tasks.construction.DestroyBlockTask;
import adris.altoclef.tasks.construction.PlaceBlockTask;
import adris.altoclef.tasks.squashed.CataloguedResourceTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.helpers.WorldHelper;
import java.util.Optional;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class ConstructIronGolemTask extends Task {
   private BlockPos position;
   private boolean canBeFinished = false;

   public ConstructIronGolemTask() {
   }

   public ConstructIronGolemTask(BlockPos pos) {
      this.position = pos;
   }

   @Override
   protected void onStart() {
      this.controller.getBehaviour().push();
      this.controller.getBehaviour().addProtectedItems(Items.IRON_BLOCK, Items.CARVED_PUMPKIN);
      this.controller.getBaritoneSettings().blocksToAvoidBreaking.get().add(Blocks.IRON_BLOCK);
   }

   @Override
   protected Task onTick() {
      AltoClefController mod = this.controller;
      if (!StorageHelper.itemTargetsMetInventory(mod, this.golemMaterials(mod))) {
         this.setDebugState("Getting materials for the iron golem");
         return new CataloguedResourceTask(this.golemMaterials(mod));
      } else {
         if (this.position == null) {
            for (BlockPos pos : WorldHelper.scanRegion(
               new BlockPos(mod.getPlayer().getBlockX(), 64, mod.getPlayer().getBlockZ()),
               new BlockPos(mod.getPlayer().getBlockX(), 128, mod.getPlayer().getBlockZ())
            )) {
               if (mod.getWorld().getBlockState(pos).getBlock() == Blocks.AIR) {
                  this.position = pos;
                  break;
               }
            }

            if (this.position == null) {
               this.position = mod.getPlayer().getBlockPos();
            }
         }

         if (!WorldHelper.isBlock(this.controller, this.position, Blocks.IRON_BLOCK)) {
            if (!WorldHelper.isBlock(this.controller, this.position, Blocks.AIR)) {
               this.setDebugState("Destroying block in way of base iron block");
               return new DestroyBlockTask(this.position);
            } else {
               this.setDebugState("Placing the base iron block");
               return new PlaceBlockTask(this.position, Blocks.IRON_BLOCK);
            }
         } else if (!WorldHelper.isBlock(this.controller, this.position.up(), Blocks.IRON_BLOCK)) {
            if (!WorldHelper.isBlock(this.controller, this.position.up(), Blocks.AIR)) {
               this.setDebugState("Destroying block in way of center iron block");
               return new DestroyBlockTask(this.position.up());
            } else {
               this.setDebugState("Placing the center iron block");
               return new PlaceBlockTask(this.position.up(), Blocks.IRON_BLOCK);
            }
         } else if (!WorldHelper.isBlock(this.controller, this.position.up().east(), Blocks.IRON_BLOCK)) {
            if (!WorldHelper.isBlock(this.controller, this.position.up().east(), Blocks.AIR)) {
               this.setDebugState("Destroying block in way of east iron block");
               return new DestroyBlockTask(this.position.up().east());
            } else {
               this.setDebugState("Placing the east iron block");
               return new PlaceBlockTask(this.position.up().east(), Blocks.IRON_BLOCK);
            }
         } else if (!WorldHelper.isBlock(this.controller, this.position.up().west(), Blocks.IRON_BLOCK)) {
            if (!WorldHelper.isBlock(this.controller, this.position.up().west(), Blocks.AIR)) {
               this.setDebugState("Destroying block in way of west iron block");
               return new DestroyBlockTask(this.position.up().west());
            } else {
               this.setDebugState("Placing the west iron block");
               return new PlaceBlockTask(this.position.up().west(), Blocks.IRON_BLOCK);
            }
         } else if (!WorldHelper.isBlock(this.controller, this.position.east(), Blocks.AIR)) {
            this.setDebugState("Clearing area on east side...");
            return new DestroyBlockTask(this.position.east());
         } else if (!WorldHelper.isBlock(this.controller, this.position.west(), Blocks.AIR)) {
            this.setDebugState("Clearing area on west side...");
            return new DestroyBlockTask(this.position.west());
         } else if (!WorldHelper.isBlock(this.controller, this.position.up(2), Blocks.AIR)) {
            this.setDebugState("Destroying block in way of pumpkin");
            return new DestroyBlockTask(this.position.up(2));
         } else {
            this.canBeFinished = true;
            this.setDebugState("Placing the pumpkin (I think)");
            return new PlaceBlockTask(this.position.up(2), Blocks.CARVED_PUMPKIN);
         }
      }
   }

   @Override
   protected void onStop(Task interruptTask) {
      this.controller.getBaritoneSettings().blocksToAvoidBreaking.get().remove(Blocks.IRON_BLOCK);
      this.controller.getBehaviour().pop();
   }

   @Override
   protected boolean isEqual(Task other) {
      return other instanceof ConstructIronGolemTask;
   }

   @Override
   public boolean isFinished() {
      if (this.position == null) {
         return false;
      } else {
         Optional<Entity> closestIronGolem = this.controller
            .getEntityTracker()
            .getClosestEntity(new Vec3d(this.position.getX(), this.position.getY(), this.position.getZ()), IronGolemEntity.class);
         return closestIronGolem.isPresent() && closestIronGolem.get().getBlockPos().isWithinDistance(this.position, 2.0) && this.canBeFinished;
      }
   }

   @Override
   protected String toDebugString() {
      return "Construct Iron Golem";
   }

   private int ironBlocksNeeded(AltoClefController mod) {
      if (this.position == null) {
         return 4;
      } else {
         int needed = 0;
         if (mod.getWorld().getBlockState(this.position).getBlock() != Blocks.IRON_BLOCK) {
            needed++;
         }

         if (mod.getWorld().getBlockState(this.position.up().west()).getBlock() != Blocks.IRON_BLOCK) {
            needed++;
         }

         if (mod.getWorld().getBlockState(this.position.up().east()).getBlock() != Blocks.IRON_BLOCK) {
            needed++;
         }

         if (mod.getWorld().getBlockState(this.position.up()).getBlock() != Blocks.IRON_BLOCK) {
            needed++;
         }

         return needed;
      }
   }

   private ItemTarget[] golemMaterials(AltoClefController mod) {
      return this.position != null && mod.getWorld().getBlockState(this.position.up(2)).getBlock() == Blocks.CARVED_PUMPKIN
         ? new ItemTarget[]{new ItemTarget(Items.IRON_BLOCK, this.ironBlocksNeeded(mod))}
         : new ItemTarget[]{new ItemTarget(Items.IRON_BLOCK, this.ironBlocksNeeded(mod)), new ItemTarget(Items.CARVED_PUMPKIN, 1)};
   }
}
