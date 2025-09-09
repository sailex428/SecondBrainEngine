package adris.altoclef.tasks.speedrun;

import adris.altoclef.AltoClefController;
import adris.altoclef.Debug;
import adris.altoclef.tasks.construction.DestroyBlockTask;
import adris.altoclef.tasks.construction.PlaceBlockTask;
import adris.altoclef.tasks.movement.GetToBlockTask;
import adris.altoclef.tasks.movement.GetToXZTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.LookHelper;
import adris.altoclef.util.helpers.WorldHelper;
import adris.altoclef.util.time.TimerGame;
import baritone.api.utils.input.Input;
import java.util.Arrays;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.EnderDragonPart;
import net.minecraft.entity.boss.dragon.phase.LandingApproachPhase;
import net.minecraft.entity.boss.dragon.phase.LandingPhase;
import net.minecraft.entity.boss.dragon.phase.Phase;
import net.minecraft.entity.boss.dragon.phase.PhaseType;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Vec3d;

public class KillEnderDragonWithBedsTask extends Task {
   private final WaitForDragonAndPearlTask whenNotPerchingTask;
   TimerGame placeBedTimer = new TimerGame(0.6);
   TimerGame waiTimer = new TimerGame(0.3);
   TimerGame waitBeforePlaceTimer = new TimerGame(0.5);
   boolean waited = false;
   double prevDist = 100.0;
   private BlockPos endPortalTop;
   private Task freePortalTopTask = null;
   private Task placeObsidianTask = null;
   private boolean dragonDead = false;

   public KillEnderDragonWithBedsTask() {
      this.whenNotPerchingTask = new WaitForDragonAndPearlTask();
   }

   public static BlockPos locateExitPortalTop(AltoClefController mod) {
      if (!mod.getChunkTracker().isChunkLoaded(new BlockPos(0, 64, 0))) {
         return null;
      } else {
         int height = WorldHelper.getGroundHeight(mod, 0, 0, Blocks.BEDROCK);
         return height != -1 ? new BlockPos(0, height, 0) : null;
      }
   }

   @Override
   protected void onStart() {
      this.controller.getBehaviour().avoidBlockPlacing(pos -> pos.getZ() == 0 && Math.abs(pos.getX()) < 5);
   }

   @Override
   protected Task onTick() {
      AltoClefController mod = this.controller;
      if (this.endPortalTop == null) {
         this.endPortalTop = locateExitPortalTop(mod);
         if (this.endPortalTop != null) {
            this.whenNotPerchingTask.setExitPortalTop(this.endPortalTop);
         }
      }

      if (this.endPortalTop == null) {
         this.setDebugState("Searching for end portal top.");
         return new GetToXZTask(0, 0);
      } else {
         BlockPos obsidianTarget = this.endPortalTop.up().offset(Direction.NORTH);
         if (!mod.getWorld().getBlockState(obsidianTarget).getBlock().equals(Blocks.OBSIDIAN)) {
            if (WorldHelper.inRangeXZ(mod.getPlayer().getPos(), new Vec3d(0.0, 0.0, 0.0), 10.0)) {
               if (this.placeObsidianTask == null) {
                  this.placeObsidianTask = new PlaceBlockTask(obsidianTarget, Blocks.OBSIDIAN);
               }

               return this.placeObsidianTask;
            } else {
               return new GetToXZTask(0, 0);
            }
         } else {
            BlockState stateAtPortal = mod.getWorld().getBlockState(this.endPortalTop.up());
            if (!stateAtPortal.isAir()
               && !stateAtPortal.getBlock().equals(Blocks.FIRE)
               && !Arrays.stream(ItemHelper.itemsToBlocks(ItemHelper.BED)).toList().contains(stateAtPortal.getBlock())) {
               if (this.freePortalTopTask == null) {
                  this.freePortalTopTask = new DestroyBlockTask(this.endPortalTop.up());
               }

               return this.freePortalTopTask;
            } else if (this.dragonDead) {
               this.setDebugState("Waiting for overworld portal to spawn.");
               return new GetToBlockTask(this.endPortalTop.down(4).west());
            } else {
               if (!mod.getEntityTracker().entityFound(EnderDragonEntity.class) || this.dragonDead) {
                  this.setDebugState("No dragon found.");
                  if (!WorldHelper.inRangeXZ(mod.getPlayer(), this.endPortalTop, 1.0)) {
                     this.setDebugState("Going to end portal top at" + this.endPortalTop.toString() + ".");
                     return new GetToBlockTask(this.endPortalTop);
                  }
               }

               for (EnderDragonEntity dragon : mod.getEntityTracker().getTrackedEntities(EnderDragonEntity.class)) {
                  Phase dragonPhase = dragon.getPhaseManager().getCurrent();
                  if (dragonPhase.getType() == PhaseType.DYING) {
                     Debug.logMessage("Dragon is dead.");
                     if (mod.getPlayer().getPitch() != -90.0F) {
                        mod.getPlayer().setPitch(-90.0F);
                     }

                     this.dragonDead = true;
                     return null;
                  }

                  boolean perching = dragonPhase instanceof LandingPhase || dragonPhase instanceof LandingApproachPhase || dragonPhase.isSittingOrHovering();
                  if (dragon.getY() < this.endPortalTop.getY() + 2) {
                     perching = false;
                  }

                  this.whenNotPerchingTask.setPerchState(perching);
                  if (this.whenNotPerchingTask.isActive() && !this.whenNotPerchingTask.isFinished()) {
                     this.setDebugState("Dragon not perching, performing special behavior...");
                     return this.whenNotPerchingTask;
                  }

                  if (perching) {
                     return this.performOneCycle(mod, dragon);
                  }
               }

               mod.getFoodChain().shouldStop(false);
               return this.whenNotPerchingTask;
            }
         }
      }
   }

   private Task performOneCycle(AltoClefController mod, EnderDragonEntity dragon) {
      mod.getFoodChain().shouldStop(true);
      if (mod.getInputControls().isHeldDown(Input.SNEAK)) {
         mod.getInputControls().release(Input.SNEAK);
      }

      mod.getSlotHandler().forceEquipItemToOffhand(Items.AIR);
      BlockPos endPortalTop = locateExitPortalTop(mod).up();
      BlockPos obsidian = null;
      Direction dir = null;

      for (Direction direction : new Direction[]{Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH}) {
         if (mod.getWorld().getBlockState(endPortalTop.offset(direction)).getBlock().equals(Blocks.OBSIDIAN)) {
            obsidian = endPortalTop.offset(direction);
            dir = direction.getOpposite();
            break;
         }
      }

      if (dir == null) {
         mod.log("no obisidan? :(");
         return null;
      } else {
         Direction offsetDir = dir.getAxis() == Axis.X ? Direction.SOUTH : Direction.WEST;
         BlockPos targetBlock = endPortalTop.down(3).offset(offsetDir, 3).offset(dir);
         double d = this.distanceIgnoreY(WorldHelper.toVec3d(targetBlock), mod.getPlayer().getPos());
         if (!(d > 0.7) && mod.getPlayer().getBlockPos().down().getY() <= endPortalTop.getY() - 4) {
            if (!this.waited) {
               this.waited = true;
               this.waitBeforePlaceTimer.reset();
            }

            if (!this.waitBeforePlaceTimer.elapsed()) {
               mod.log(this.waitBeforePlaceTimer.getDuration() + " waiting...");
               return null;
            } else {
               LookHelper.lookAt(mod, obsidian, dir);
               BlockPos bedHead = WorldHelper.getBedHead(mod, endPortalTop);
               mod.getSlotHandler().forceEquipItem(ItemHelper.BED);
               if (bedHead == null) {
                  if (this.placeBedTimer.elapsed() && Math.abs(dragon.getY() - endPortalTop.getY()) < 10.0) {
                     mod.getInputControls().tryPress(Input.CLICK_RIGHT);
                     this.waiTimer.reset();
                  }

                  return null;
               } else if (!this.waiTimer.elapsed()) {
                  return null;
               } else {
                  Vec3d dragonHeadPos = dragon.head.getBoundingBox().getCenter();
                  Vec3d bedHeadPos = WorldHelper.toVec3d(bedHead);
                  double dist = dragonHeadPos.distanceTo(bedHeadPos);
                  double distXZ = this.distanceIgnoreY(dragonHeadPos, bedHeadPos);
                  EnderDragonPart body = dragon.getBodyParts()[2];
                  double destroyDistance = Math.abs(body.getBoundingBox().getMin(Axis.Y) - bedHeadPos.getY());
                  boolean tooClose = destroyDistance < 1.1;
                  boolean skip = destroyDistance > 3.0 && dist > 4.5 && distXZ > 2.5;
                  mod.log(destroyDistance + " : " + destroyDistance + " : " + dist);
                  if ((
                        dist < 1.5
                           || this.prevDist < distXZ && destroyDistance < 4.0 && this.prevDist < 2.9
                           || destroyDistance < 2.0 && dist < 4.0
                           || destroyDistance < 1.7 && dist < 4.5
                           || tooClose
                           || destroyDistance < 2.4 && distXZ < 3.7
                           || destroyDistance < 3.5 && distXZ < 2.4
                     )
                     && !skip) {
                     mod.getInputControls().tryPress(Input.CLICK_RIGHT);
                     this.placeBedTimer.reset();
                  }

                  this.prevDist = distXZ;
                  return null;
               }
            }
         } else {
            mod.log(d + "");
            return new GetToBlockTask(targetBlock);
         }
      }
   }

   public double distanceIgnoreY(Vec3d vec, Vec3d vec1) {
      double d = vec.x - vec1.x;
      double f = vec.z - vec1.z;
      return Math.sqrt(d * d + f * f);
   }

   @Override
   protected void onStop(Task interruptTask) {
      this.controller.getFoodChain().shouldStop(false);
   }

   @Override
   public boolean isFinished() {
      return super.isFinished();
   }

   @Override
   protected boolean isEqual(Task other) {
      return other instanceof KillEnderDragonWithBedsTask;
   }

   @Override
   protected String toDebugString() {
      return "Bedding the Ender Dragon";
   }
}
