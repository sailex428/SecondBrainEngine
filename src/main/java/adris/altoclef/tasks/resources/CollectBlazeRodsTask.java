package adris.altoclef.tasks.resources;

import adris.altoclef.AltoClefController;
import adris.altoclef.Debug;
import adris.altoclef.multiversion.blockpos.BlockPosVer;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasks.construction.PutOutFireTask;
import adris.altoclef.tasks.entity.KillEntitiesTask;
import adris.altoclef.tasks.movement.DefaultGoToDimensionTask;
import adris.altoclef.tasks.movement.GetToBlockTask;
import adris.altoclef.tasks.movement.RunAwayFromHostilesTask;
import adris.altoclef.tasks.movement.SearchChunkForBlockTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.helpers.WorldHelper;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.BlazeEntity;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public class CollectBlazeRodsTask extends ResourceTask {
   private static final double SPAWNER_BLAZE_RADIUS = 32.0;
   private static final double TOO_LITTLE_HEALTH_BLAZE = 10.0;
   private static final int TOO_MANY_BLAZES = 5;
   private final int count;
   private final Task searcher = new SearchChunkForBlockTask(Blocks.NETHER_BRICKS);
   private BlockPos foundBlazeSpawner = null;

   public CollectBlazeRodsTask(int count) {
      super(Items.BLAZE_ROD, count);
      this.count = count;
   }

   private static boolean isHoveringAboveLavaOrTooHigh(AltoClefController mod, Entity entity) {
      int MAX_HEIGHT = 11;

      for (BlockPos check = entity.getBlockPos(); entity.getBlockPos().getY() - check.getY() < MAX_HEIGHT; check = check.down()) {
         if (mod.getWorld().getBlockState(check).getBlock() == Blocks.LAVA) {
            return true;
         }

         if (WorldHelper.isSolidBlock(mod, check)) {
            return false;
         }
      }

      return true;
   }

   @Override
   protected void onResourceStart(AltoClefController mod) {
   }

   @Override
   protected Task onResourceTick(AltoClefController mod) {
      if (WorldHelper.getCurrentDimension(this.controller) != Dimension.NETHER) {
         this.setDebugState("Going to nether");
         return new DefaultGoToDimensionTask(Dimension.NETHER);
      } else {
         Optional<Entity> toKill = Optional.empty();
         if (mod.getEntityTracker().entityFound(BlazeEntity.class)) {
            toKill = mod.getEntityTracker().getClosestEntity(BlazeEntity.class);
            if (toKill.isPresent() && mod.getPlayer().getHealth() <= 10.0 && mod.getEntityTracker().getTrackedEntities(BlazeEntity.class).size() >= 5) {
               this.setDebugState("Running away as there are too many blazes nearby.");
               return new RunAwayFromHostilesTask(30.0, true);
            }

            if (this.foundBlazeSpawner != null && toKill.isPresent()) {
               Entity kill = toKill.get();
               Vec3d nearest = kill.getPos();
               double sqDistanceToPlayer = nearest.squaredDistanceTo(mod.getPlayer().getPos());
               if (sqDistanceToPlayer > 1024.0) {
                  BlockHitResult hit = mod.getWorld()
                     .raycast(new RaycastContext(mod.getPlayer().getCameraPosVec(1.0F), kill.getCameraPosVec(1.0F), ShapeType.OUTLINE, FluidHandling.NONE, mod.getPlayer()));
                  if (hit != null && BlockPosVer.getSquaredDistance(hit.getBlockPos(), mod.getPlayer().getPos()) < sqDistanceToPlayer) {
                     toKill = Optional.empty();
                  }
               }
            }
         }

         if (toKill.isPresent() && toKill.get().isAlive() && !isHoveringAboveLavaOrTooHigh(mod, toKill.get())) {
            this.setDebugState("Killing blaze");
            Predicate<Entity> safeToPursue = entity -> !isHoveringAboveLavaOrTooHigh(mod, entity);
            return new KillEntitiesTask(safeToPursue, toKill.get().getClass());
         } else {
            if (this.foundBlazeSpawner != null
               && mod.getChunkTracker().isChunkLoaded(this.foundBlazeSpawner)
               && !this.isValidBlazeSpawner(mod, this.foundBlazeSpawner)) {
               Debug.logMessage("Blaze spawner at " + this.foundBlazeSpawner + " too far away or invalid. Re-searching.");
               this.foundBlazeSpawner = null;
            }

            if (this.foundBlazeSpawner != null) {
               if (!this.foundBlazeSpawner.isWithinDistance(mod.getPlayer().getPos(), 4.0)) {
                  this.setDebugState("Going to blaze spawner");
                  return new GetToBlockTask(this.foundBlazeSpawner.up(), false);
               } else {
                  Optional<BlockPos> nearestFire = mod.getBlockScanner().getNearestWithinRange(this.foundBlazeSpawner, 5.0, Blocks.FIRE);
                  if (nearestFire.isPresent()) {
                     this.setDebugState("Clearing fire around spawner to prevent loss of blaze rods.");
                     return new PutOutFireTask(nearestFire.get());
                  } else {
                     this.setDebugState("Waiting near blaze spawner for blazes to spawn");
                     return null;
                  }
               }
            } else {
               Optional<BlockPos> pos = mod.getBlockScanner()
                  .getNearestBlock((Predicate<BlockPos>)(blockPos -> this.isValidBlazeSpawner(mod, blockPos)), Blocks.SPAWNER);
               pos.ifPresent(blockPos -> this.foundBlazeSpawner = blockPos);
               this.setDebugState("Searching for fortress/Traveling around fortress");
               return this.searcher;
            }
         }
      }
   }

   private boolean isValidBlazeSpawner(AltoClefController mod, BlockPos pos) {
      return !mod.getChunkTracker().isChunkLoaded(pos) ? false : WorldHelper.getSpawnerEntity(mod, pos) instanceof BlazeEntity;
   }

   @Override
   protected void onResourceStop(AltoClefController mod, Task interruptTask) {
   }

   @Override
   protected boolean isEqualResource(ResourceTask other) {
      return other instanceof CollectBlazeRodsTask;
   }

   @Override
   protected String toDebugStringName() {
      return "Collect blaze rods - " + this.controller.getItemStorage().getItemCount(Items.BLAZE_ROD) + "/" + this.count;
   }

   @Override
   protected boolean shouldAvoidPickingUp(AltoClefController mod) {
      return false;
   }
}
