package me.sailex.altoclef.chains;

import me.sailex.altoclef.AltoClefController;
import me.sailex.altoclef.TaskCatalogue;
import me.sailex.altoclef.tasks.movement.MLGBucketTask;
import me.sailex.altoclef.tasksystem.ITaskOverridesGrounded;
import me.sailex.altoclef.tasksystem.TaskRunner;
import me.sailex.altoclef.util.helpers.LookHelper;
import me.sailex.altoclef.util.time.TimerGame;
import me.sailex.automatone.api.utils.Rotation;
import me.sailex.automatone.api.utils.input.Input;
import net.minecraft.block.Blocks;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.RaycastContext.FluidHandling;

import java.util.Optional;

public class MLGBucketFallChain extends SingleTaskChain implements ITaskOverridesGrounded {
   private final TimerGame tryCollectWaterTimer = new TimerGame(4.0);
   private final TimerGame pickupRepeatTimer = new TimerGame(0.25);
   private MLGBucketTask lastMLG = null;
   private boolean wasPickingUp = false;
   private boolean doingChorusFruit = false;

   public MLGBucketFallChain(TaskRunner runner) {
      super(runner);
   }

   @Override
   protected void onTaskFinish(AltoClefController mod) {
   }

   @Override
   public float getPriority() {
      if (!AltoClefController.inGame()) {
         return Float.NEGATIVE_INFINITY;
      } else {
         AltoClefController mod = this.controller;
         if (this.isFalling(mod)) {
            this.tryCollectWaterTimer.reset();
            this.setTask(new MLGBucketTask());
            this.lastMLG = (MLGBucketTask)this.mainTask;
            return 100.0F;
         } else {
            if (!this.tryCollectWaterTimer.elapsed()
               && mod.getItemStorage().hasItem(Items.BUCKET)
               && !mod.getItemStorage().hasItem(Items.WATER_BUCKET)
               && this.lastMLG != null) {
               BlockPos placed = this.lastMLG.getWaterPlacedPos();

               boolean isPlacedWater;
               try {
                  isPlacedWater = mod.getWorld().getBlockState(placed).getBlock() == Blocks.WATER;
               } catch (Exception var6) {
                  isPlacedWater = false;
               }

               if (placed != null && placed.isWithinDistance(mod.getPlayer().getPos(), 5.5) && isPlacedWater) {
                  mod.getBehaviour().push();
                  mod.getBehaviour().setRayTracingFluidHandling(FluidHandling.SOURCE_ONLY);
                  Optional<Rotation> reach = LookHelper.getReach(this.controller, placed, Direction.UP);
                  if (reach.isPresent()) {
                     mod.getBaritone().getLookBehavior().updateTarget(reach.get(), true);
                     if (mod.getBaritone().getPlayerContext().isLookingAt(placed) && mod.getSlotHandler().forceEquipItem(Items.BUCKET)) {
                        if (this.pickupRepeatTimer.elapsed()) {
                           this.pickupRepeatTimer.reset();
                           mod.getInputControls().tryPress(Input.CLICK_RIGHT);
                           this.wasPickingUp = true;
                        } else if (this.wasPickingUp) {
                           this.wasPickingUp = false;
                        }
                     }
                  } else {
                     this.setTask(TaskCatalogue.getItemTask(Items.WATER_BUCKET, 1));
                  }

                  mod.getBehaviour().pop();
                  return 60.0F;
               }
            }

            if (this.wasPickingUp) {
               this.wasPickingUp = false;
               this.lastMLG = null;
            }

            if (mod.getPlayer().hasStatusEffect(StatusEffects.LEVITATION)
               && ((StatusEffectInstance)mod.getPlayer().getActiveStatusEffects().get(StatusEffects.LEVITATION)).getDuration() <= 70
               && mod.getItemStorage().hasItemInventoryOnly(Items.CHORUS_FRUIT)
               && !mod.getItemStorage().hasItemInventoryOnly(Items.WATER_BUCKET)) {
               this.doingChorusFruit = true;
               mod.getSlotHandler().forceEquipItem(Items.CHORUS_FRUIT);
               mod.getInputControls().hold(Input.CLICK_RIGHT);
               mod.getExtraBaritoneSettings().setInteractionPaused(true);
            } else if (this.doingChorusFruit) {
               this.doingChorusFruit = false;
               mod.getInputControls().release(Input.CLICK_RIGHT);
               mod.getExtraBaritoneSettings().setInteractionPaused(false);
            }

            this.lastMLG = null;
            return Float.NEGATIVE_INFINITY;
         }
      }
   }

   @Override
   public String getName() {
      return "MLG Water Bucket Fall Chain";
   }

   @Override
   public boolean isActive() {
      return true;
   }

   public boolean doneMLG() {
      return this.lastMLG == null;
   }

   public boolean isChorusFruiting() {
      return this.doingChorusFruit;
   }

   public boolean isFalling(AltoClefController mod) {
      if (!mod.getModSettings().shouldAutoMLGBucket()) {
         return false;
      } else if (!mod.getPlayer().isSwimming() && !mod.getPlayer().isTouchingWater() && !mod.getPlayer().isOnGround() && !mod.getPlayer().isClimbing()) {
         double ySpeed = mod.getPlayer().getVelocity().y;
         return ySpeed < -0.7;
      } else {
         return false;
      }
   }
}
