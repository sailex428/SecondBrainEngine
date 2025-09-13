package me.sailex.altoclef.tasks.speedrun;

import me.sailex.altoclef.AltoClefController;
import me.sailex.altoclef.Debug;
import me.sailex.altoclef.TaskCatalogue;
import me.sailex.altoclef.tasks.entity.DoToClosestEntityTask;
import me.sailex.altoclef.tasks.entity.KillEntitiesTask;
import me.sailex.altoclef.tasks.movement.GetToBlockTask;
import me.sailex.altoclef.tasks.movement.GetToXZTask;
import me.sailex.altoclef.tasks.movement.GetToYTask;
import me.sailex.altoclef.tasks.movement.ThrowEnderPearlSimpleProjectileTask;
import me.sailex.altoclef.tasks.resources.GetBuildingMaterialsTask;
import me.sailex.altoclef.tasksystem.Task;
import me.sailex.altoclef.util.helpers.LookHelper;
import me.sailex.altoclef.util.helpers.StorageHelper;
import me.sailex.altoclef.util.helpers.WorldHelper;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.projectile.DragonFireballEntity;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;
import java.util.function.Predicate;

public class WaitForDragonAndPearlTask extends Task {
   private static final double XZ_RADIUS = 30.0;
   private static final double XZ_RADIUS_TOO_FAR = 38.0;
   private static final int HEIGHT = 42;
   private static final int CLOSE_ENOUGH_DISTANCE = 15;
   private final int Y_COORDINATE = 75;
   private static final double DRAGON_FIREBALL_TOO_CLOSE_RANGE = 40.0;
   private final Task buildingMaterialsTask = new GetBuildingMaterialsTask(52);
   boolean inCenter;
   private Task heightPillarTask;
   private Task throwPearlTask;
   private BlockPos targetToPearl;
   private boolean dragonIsPerching;
   private Task pillarUpFurther;
   private boolean hasPillar = false;

   public void setExitPortalTop(BlockPos top) {
      BlockPos actualTarget = top.down();
      if (!actualTarget.equals(this.targetToPearl)) {
         this.targetToPearl = actualTarget;
         this.throwPearlTask = new ThrowEnderPearlSimpleProjectileTask(actualTarget);
      }
   }

   public void setPerchState(boolean perching) {
      this.dragonIsPerching = perching;
   }

   @Override
   protected void onStart() {
   }

   @Override
   protected Task onTick() {
      AltoClefController mod = this.controller;
      Optional<Entity> enderMen = mod.getEntityTracker().getClosestEntity(EndermanEntity.class);
      if (enderMen.isPresent()) {
         EndermanEntity endermanEntity = (EndermanEntity)enderMen.get();
         if (endermanEntity instanceof EndermanEntity && endermanEntity.getTarget() == mod.getPlayer()) {
            this.setDebugState("Killing angry endermen");
            Predicate<Entity> angry = entity -> endermanEntity.getTarget() == mod.getPlayer();
            return new KillEntitiesTask(angry, enderMen.get().getClass());
         }
      }

      if (this.throwPearlTask != null && this.throwPearlTask.isActive() && !this.throwPearlTask.isFinished()) {
         this.setDebugState("Throwing pearl!");
         return this.throwPearlTask;
      } else {
         if (this.pillarUpFurther != null
            && this.pillarUpFurther.isActive()
            && !this.pillarUpFurther.isFinished()
            && mod.getEntityTracker().getClosestEntity(AreaEffectCloudEntity.class).isPresent()) {
            Optional<Entity> cloud = mod.getEntityTracker().getClosestEntity(AreaEffectCloudEntity.class);
            if (cloud.isPresent() && cloud.get().isInRange(mod.getPlayer(), 4.0)) {
               this.setDebugState("PILLAR UP FURTHER to avoid dragon's breath");
               return this.pillarUpFurther;
            }

            Optional<Entity> fireball = mod.getEntityTracker().getClosestEntity(DragonFireballEntity.class);
            if (this.isFireballDangerous(mod, fireball)) {
               this.setDebugState("PILLAR UP FURTHER to avoid dragon's breath");
               return this.pillarUpFurther;
            }
         }

         if (!mod.getItemStorage().hasItem(Items.ENDER_PEARL) && this.inCenter) {
            this.setDebugState("First get ender pearls.");
            return TaskCatalogue.getItemTask(Items.ENDER_PEARL, 1);
         } else {
            int minHeight = this.targetToPearl.getY() + 42 - 3;
            int deltaY = minHeight - mod.getPlayer().getBlockPos().getY();
            if (StorageHelper.getBuildingMaterialCount(this.controller) >= Math.min(deltaY - 10, 37)
               && (!this.buildingMaterialsTask.isActive() || this.buildingMaterialsTask.isFinished())) {
               if (this.dragonIsPerching && this.canThrowPearl(mod)) {
                  Debug.logMessage("THROWING PEARL!!");
                  return this.throwPearlTask;
               } else if (mod.getPlayer().getBlockPos().getY() < minHeight) {
                  if (mod.getEntityTracker().entityFound(entity -> mod.getPlayer().getPos().isInRange(entity.getPos(), 4.0), AreaEffectCloudEntity.class)) {
                     if (mod.getEntityTracker().getClosestEntity(EnderDragonEntity.class).isPresent() && !mod.getBaritone().getPathingBehavior().isPathing()) {
                        LookHelper.lookAt(mod, mod.getEntityTracker().getClosestEntity(EnderDragonEntity.class).get().getEyePos());
                     }

                     return null;
                  } else if (this.heightPillarTask != null && this.heightPillarTask.isActive() && !this.heightPillarTask.isFinished()) {
                     this.setDebugState("Pillaring up!");
                     this.inCenter = true;
                     return (Task)(mod.getEntityTracker().entityFound(EndCrystalEntity.class) ? new DoToClosestEntityTask(toDestroy -> {
                        if (toDestroy.isInRange(mod.getPlayer(), 7.0)) {
                           mod.getControllerExtras().attack(toDestroy);
                        }

                        if (mod.getPlayer().getBlockPos().getY() < minHeight) {
                           return this.heightPillarTask;
                        } else {
                           if (mod.getEntityTracker().getClosestEntity(EnderDragonEntity.class).isPresent() && !mod.getBaritone().getPathingBehavior().isPathing()) {
                              LookHelper.lookAt(mod, mod.getEntityTracker().getClosestEntity(EnderDragonEntity.class).get().getEyePos());
                           }

                           return null;
                        }
                     }, EndCrystalEntity.class) : this.heightPillarTask);
                  } else if (!WorldHelper.inRangeXZ(mod.getPlayer(), this.targetToPearl, 38.0) && mod.getPlayer().getPos().getY() < minHeight && !this.hasPillar
                     )
                   {
                     if (mod.getEntityTracker().entityFound(entity -> mod.getPlayer().getPos().isInRange(entity.getPos(), 4.0), AreaEffectCloudEntity.class)) {
                        if (mod.getEntityTracker().getClosestEntity(EnderDragonEntity.class).isPresent() && !mod.getBaritone().getPathingBehavior().isPathing()) {
                           LookHelper.lookAt(mod, mod.getEntityTracker().getClosestEntity(EnderDragonEntity.class).get().getEyePos());
                        }

                        return null;
                     } else {
                        this.setDebugState("Moving in (too far, might hit pillars)");
                        return new GetToXZTask(0, 0);
                     }
                  } else {
                     if (!this.hasPillar) {
                        this.hasPillar = true;
                     }

                     this.heightPillarTask = new GetToBlockTask(new BlockPos(0, minHeight, 75));
                     return this.heightPillarTask;
                  }
               } else {
                  this.setDebugState("We're high enough.");
                  Optional<Entity> dragonFireball = mod.getEntityTracker().getClosestEntity(DragonFireballEntity.class);
                  if (dragonFireball.isPresent()
                     && dragonFireball.get().isInRange(mod.getPlayer(), 40.0)
                     && LookHelper.cleanLineOfSight(mod.getPlayer(), dragonFireball.get().getPos(), 40.0)) {
                     this.pillarUpFurther = new GetToYTask(mod.getPlayer().getBlockY() + 5);
                     Debug.logMessage("HOLDUP");
                     return this.pillarUpFurther;
                  } else if (mod.getEntityTracker().entityFound(EndCrystalEntity.class)) {
                     return new DoToClosestEntityTask(toDestroy -> {
                        if (toDestroy.isInRange(mod.getPlayer(), 7.0)) {
                           mod.getControllerExtras().attack(toDestroy);
                        }

                        if (mod.getPlayer().getBlockPos().getY() < minHeight) {
                           return this.heightPillarTask;
                        } else {
                           if (mod.getEntityTracker().getClosestEntity(EnderDragonEntity.class).isPresent() && !mod.getBaritone().getPathingBehavior().isPathing()) {
                              LookHelper.lookAt(mod, mod.getEntityTracker().getClosestEntity(EnderDragonEntity.class).get().getEyePos());
                           }

                           return null;
                        }
                     }, EndCrystalEntity.class);
                  } else {
                     if (mod.getEntityTracker().getClosestEntity(EnderDragonEntity.class).isPresent() && !mod.getBaritone().getPathingBehavior().isPathing()) {
                        LookHelper.lookAt(mod, mod.getEntityTracker().getClosestEntity(EnderDragonEntity.class).get().getEyePos());
                     }

                     return null;
                  }
               }
            } else {
               this.setDebugState("Collecting building materials...");
               return this.buildingMaterialsTask;
            }
         }
      }
   }

   private boolean canThrowPearl(AltoClefController mod) {
      Vec3d targetPosition = WorldHelper.toVec3d(this.targetToPearl.up());
      BlockHitResult hitResult = LookHelper.raycast(mod.getPlayer(), LookHelper.getCameraPos(mod.getPlayer()), targetPosition, 300.0);
      if (hitResult == null) {
         return true;
      } else {
         return switch (hitResult.getType()) {
            case MISS -> true;
            case BLOCK -> hitResult.getBlockPos().isWithinDistance(this.targetToPearl.up(), 10.0);
            case ENTITY -> false;
            default -> throw new IncompatibleClassChangeError();
         };
      }
   }

   private boolean isFireballDangerous(AltoClefController mod, Optional<Entity> fireball) {
      if (fireball.isEmpty()) {
         return false;
      } else {
         boolean fireballTooClose = fireball.get().isInRange(mod.getPlayer(), 40.0);
         boolean fireballInSight = LookHelper.cleanLineOfSight(mod.getPlayer(), fireball.get().getPos(), 40.0);
         return fireballTooClose && fireballInSight;
      }
   }

   @Override
   protected void onStop(Task interruptTask) {
   }

   @Override
   protected boolean isEqual(Task other) {
      return other instanceof WaitForDragonAndPearlTask;
   }

   @Override
   public boolean isFinished() {
      return this.dragonIsPerching
         && (
            this.throwPearlTask == null
               || this.throwPearlTask.isActive() && this.throwPearlTask.isFinished()
               || WorldHelper.inRangeXZ(this.controller.getPlayer(), this.targetToPearl, 15.0)
         );
   }

   @Override
   protected String toDebugString() {
      return "Waiting for Dragon Perch + Pearling";
   }
}
