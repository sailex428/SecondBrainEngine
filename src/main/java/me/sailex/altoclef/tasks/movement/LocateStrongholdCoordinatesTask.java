package me.sailex.altoclef.tasks.movement;

import me.sailex.altoclef.AltoClefController;
import me.sailex.altoclef.Debug;
import me.sailex.altoclef.TaskCatalogue;
import me.sailex.altoclef.multiversion.blockpos.BlockPosVer;
import me.sailex.altoclef.tasksystem.Task;
import me.sailex.altoclef.util.Dimension;
import me.sailex.altoclef.util.helpers.LookHelper;
import me.sailex.altoclef.util.helpers.WorldHelper;
import me.sailex.altoclef.util.time.TimerGame;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EyeOfEnderEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.StructureTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.event.GameEvent.Emitter;

import java.util.List;
import java.util.Optional;

public class LocateStrongholdCoordinatesTask extends Task {
   private static final int EYE_RETHROW_DISTANCE = 10;
   private static final int SECOND_EYE_THROW_DISTANCE = 30;
   private final int targetEyes;
   private final int minimumEyes;
   private final TimerGame throwTimer = new TimerGame(5.0);
   private EyeDirection cachedEyeDirection = null;
   private EyeDirection cachedEyeDirection2 = null;
   private Entity currentThrownEye = null;
   private Vec3i strongholdEstimatePos = null;

   public LocateStrongholdCoordinatesTask(int targetEyes, int minimumEyes) {
      this.targetEyes = targetEyes;
      this.minimumEyes = minimumEyes;
   }

   public LocateStrongholdCoordinatesTask(int targetEyes) {
      this(targetEyes, 12);
   }

   static Vec3i calculateIntersection(Vec3d start1, Vec3d direction1, Vec3d start2, Vec3d direction2) {
      double t2 = (direction1.z * start2.x - direction1.z * start1.x - direction1.x * start2.z + direction1.x * start1.z)
         / (direction1.x * direction2.z - direction1.z * direction2.x);
      BlockPos blockPos = BlockPosVer.ofFloored(start2.add(direction2.multiply(t2)));
      return new Vec3i(blockPos.getX(), 0, blockPos.getZ());
   }

   @Override
   protected void onStart() {
   }

   public boolean isSearching() {
      return this.cachedEyeDirection != null;
   }

   @Override
   protected Task onTick() {
      AltoClefController mod = this.controller;
      if (WorldHelper.getCurrentDimension(this.controller) != Dimension.OVERWORLD) {
         this.setDebugState("Going to overworld");
         return new DefaultGoToDimensionTask(Dimension.OVERWORLD);
      } else if (mod.getItemStorage().getItemCount(Items.ENDER_EYE) < this.minimumEyes
         && mod.getEntityTracker().itemDropped(Items.ENDER_EYE)
         && !mod.getEntityTracker().entityFound(EyeOfEnderEntity.class)) {
         this.setDebugState("Picking up dropped ender eye.");
         return new PickupDroppedItemTask(Items.ENDER_EYE, this.targetEyes);
      } else if (mod.getEntityTracker().entityFound(EyeOfEnderEntity.class)) {
         if (this.currentThrownEye == null || !this.currentThrownEye.isAlive()) {
            Debug.logMessage("New eye direction");
            Debug.logMessage(this.currentThrownEye == null ? "null" : "is not alive");
            List<EyeOfEnderEntity> enderEyes = mod.getEntityTracker().getTrackedEntities(EyeOfEnderEntity.class);
            if (!enderEyes.isEmpty()) {
               for (EyeOfEnderEntity enderEye : enderEyes) {
                  this.currentThrownEye = enderEye;
               }
            }

            if (this.cachedEyeDirection2 != null) {
               this.cachedEyeDirection = null;
               this.cachedEyeDirection2 = null;
            } else if (this.cachedEyeDirection == null) {
               this.cachedEyeDirection = new EyeDirection(this.currentThrownEye.getPos());
            } else {
               this.cachedEyeDirection2 = new EyeDirection(this.currentThrownEye.getPos());
            }
         }

         if (this.cachedEyeDirection2 != null) {
            this.cachedEyeDirection2.updateEyePos(this.currentThrownEye.getPos());
         } else if (this.cachedEyeDirection != null) {
            this.cachedEyeDirection.updateEyePos(this.currentThrownEye.getPos());
         }

         if (mod.getEntityTracker().getClosestEntity(EyeOfEnderEntity.class).isPresent() && !mod.getBaritone().getPathingBehavior().isPathing()) {
            LookHelper.lookAt(mod, mod.getEntityTracker().getClosestEntity(EyeOfEnderEntity.class).get().getEyePos());
         }

         this.setDebugState("Waiting for eye to travel.");
         return null;
      } else {
         if (this.cachedEyeDirection2 != null && !mod.getEntityTracker().entityFound(EyeOfEnderEntity.class) && this.strongholdEstimatePos == null) {
            if (this.cachedEyeDirection2.getAngle() >= this.cachedEyeDirection.getAngle()) {
               Debug.logMessage("2nd eye thrown at wrong position, or points to different stronghold. Rethrowing");
               this.cachedEyeDirection = this.cachedEyeDirection2;
               this.cachedEyeDirection2 = null;
            } else {
               Vec3d throwOrigin = this.cachedEyeDirection.getOrigin();
               Vec3d throwOrigin2 = this.cachedEyeDirection2.getOrigin();
               Vec3d throwDelta = this.cachedEyeDirection.getDelta();
               Vec3d throwDelta2 = this.cachedEyeDirection2.getDelta();
               this.strongholdEstimatePos = calculateIntersection(throwOrigin, throwDelta, throwOrigin2, throwDelta2);
               Debug.logMessage(
                  "Stronghold is at "
                     + this.strongholdEstimatePos.getX()
                     + ", "
                     + this.strongholdEstimatePos.getZ()
                     + " ("
                     + (int)mod.getPlayer().getPos().distanceTo(Vec3d.of(this.strongholdEstimatePos))
                     + " blocks away)"
               );
            }
         }

         if (this.strongholdEstimatePos != null
            && mod.getPlayer().getPos().distanceTo(Vec3d.of(this.strongholdEstimatePos)) < 10.0
            && WorldHelper.getCurrentDimension(this.controller) == Dimension.OVERWORLD) {
            this.strongholdEstimatePos = null;
            this.cachedEyeDirection = null;
            this.cachedEyeDirection2 = null;
         }

         if (!mod.getEntityTracker().entityFound(EyeOfEnderEntity.class) && this.strongholdEstimatePos == null) {
            if (WorldHelper.getCurrentDimension(this.controller) == Dimension.NETHER) {
               this.setDebugState("Going to overworld.");
               return new DefaultGoToDimensionTask(Dimension.OVERWORLD);
            } else if (!mod.getItemStorage().hasItem(Items.ENDER_EYE)) {
               this.setDebugState("Collecting eye of ender.");
               return TaskCatalogue.getItemTask(Items.ENDER_EYE, 1);
            } else {
               if (this.cachedEyeDirection == null) {
                  this.setDebugState("Throwing first eye.");
               } else {
                  this.setDebugState("Throwing second eye.");
                  double sqDist = mod.getPlayer().squaredDistanceTo(this.cachedEyeDirection.getOrigin());
                  if (sqDist < 900.0 && this.cachedEyeDirection != null) {
                     return new GoInDirectionXZTask(this.cachedEyeDirection.getOrigin(), this.cachedEyeDirection.getDelta().rotateY((float) (Math.PI / 2)), 1.0);
                  }
               }

               if (mod.getSlotHandler().forceEquipItem(Items.ENDER_EYE)) {
                  this.throwEye(this.controller.getWorld(), this.controller.getEntity());
               } else {
                  Debug.logWarning("Failed to equip eye of ender to throw.");
               }

               return null;
            }
         } else if ((this.cachedEyeDirection == null || this.cachedEyeDirection.hasDelta())
            && (this.cachedEyeDirection2 == null || this.cachedEyeDirection2.hasDelta())) {
            return null;
         } else {
            this.setDebugState("Waiting for thrown eye to appear...");
            return null;
         }
      }
   }

   private void throwEye(ServerWorld world, LivingEntity user) {
      BlockPos blockPos = world.locateStructure(StructureTags.EYE_OF_ENDER_LOCATED, user.getBlockPos(), 100, false);
      if (blockPos != null) {
         EyeOfEnderEntity eyeOfEnderEntity = new EyeOfEnderEntity(world, user.getX(), user.getBodyY(0.5), user.getZ());
         eyeOfEnderEntity.setItem(user.getMainHandStack());
         eyeOfEnderEntity.initTargetPos(blockPos);
         world.emitGameEvent(GameEvent.PROJECTILE_SHOOT, eyeOfEnderEntity.getPos(), Emitter.of(user));
         world.spawnEntity(eyeOfEnderEntity);
         world.playSound(
            (PlayerEntity)null,
            user.getX(),
            user.getY(),
            user.getZ(),
            SoundEvents.ENTITY_ENDER_EYE_LAUNCH,
            SoundCategory.NEUTRAL,
            0.5F,
            0.4F / (world.getRandom().nextFloat() * 0.4F + 0.8F)
         );
         world.syncWorldEvent((PlayerEntity)null, 1003, user.getBlockPos(), 0);
         user.getMainHandStack().decrement(1);
         user.swingHand(Hand.MAIN_HAND, true);
      }
   }

   @Override
   protected void onStop(Task interruptTask) {
   }

   public Optional<BlockPos> getStrongholdCoordinates() {
      return this.strongholdEstimatePos == null ? Optional.empty() : Optional.of(new BlockPos(this.strongholdEstimatePos));
   }

   @Override
   protected boolean isEqual(Task other) {
      return other instanceof LocateStrongholdCoordinatesTask;
   }

   @Override
   protected String toDebugString() {
      return "Locating stronghold coordinates";
   }

   @Override
   public boolean isFinished() {
      return this.strongholdEstimatePos != null;
   }

   private static class EyeDirection {
      private final Vec3d start;
      private Vec3d end;

      public EyeDirection(Vec3d startPos) {
         this.start = startPos;
      }

      public void updateEyePos(Vec3d endPos) {
         this.end = endPos;
      }

      public Vec3d getOrigin() {
         return this.start;
      }

      public Vec3d getDelta() {
         return this.end == null ? Vec3d.ZERO : this.end.subtract(this.start);
      }

      public double getAngle() {
         return this.end == null ? 0.0 : Math.atan2(this.getDelta().getX(), this.getDelta().getZ());
      }

      public boolean hasDelta() {
         return this.end != null && this.getDelta().lengthSquared() > 1.0E-5;
      }
   }
}
