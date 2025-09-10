package adris.altoclef.chains;

import adris.altoclef.AltoClefController;
import adris.altoclef.Debug;
import adris.altoclef.tasks.construction.DestroyBlockTask;
import adris.altoclef.tasks.movement.GetOutOfWaterTask;
import adris.altoclef.tasks.movement.GetToBlockTask;
import adris.altoclef.tasks.movement.SafeRandomShimmyTask;
import adris.altoclef.tasksystem.TaskRunner;
import adris.altoclef.util.time.TimerGame;
import baritone.api.utils.input.Input;
import java.util.LinkedList;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.EndPortalFrameBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class UnstuckChain extends SingleTaskChain {
   private final LinkedList<Vec3d> posHistory = new LinkedList<>();
   private final TimerGame shimmyTimer = new TimerGame(5.0);
   private final TimerGame placeBlockGoToBlockTimeout = new TimerGame(5.0);
   private boolean isProbablyStuck = false;
   private int eatingTicks = 0;
   private boolean interruptedEating = false;
   private boolean startedShimmying = false;
   private BlockPos placeBlockGoToBlock = null;

   public UnstuckChain(TaskRunner runner) {
      super(runner);
   }

   @Override
   public float getPriority() {
      if (this.controller != null && this.controller.getTaskRunner().isActive()) {
         this.isProbablyStuck = false;
         LivingEntity player = this.controller.getEntity();
         this.posHistory.addFirst(player.getPos());
         if (this.posHistory.size() > 500) {
            this.posHistory.removeLast();
         }

         this.checkStuckInWater();
         this.checkStuckInPowderSnow();
         this.checkEatingGlitch();
         this.checkStuckOnEndPortalFrame();
         if (this.isProbablyStuck) {
            return 65.0F;
         } else if (this.startedShimmying && !this.shimmyTimer.elapsed()) {
            this.setTask(new SafeRandomShimmyTask());
            return 65.0F;
         } else {
            this.startedShimmying = false;
            if (this.placeBlockGoToBlockTimeout.elapsed()) {
               this.placeBlockGoToBlock = null;
            }

            if (this.placeBlockGoToBlock != null) {
               this.setTask(new GetToBlockTask(this.placeBlockGoToBlock, false));
               return 65.0F;
            } else {
               return Float.NEGATIVE_INFINITY;
            }
         }
      } else {
         return Float.NEGATIVE_INFINITY;
      }
   }

   private void checkStuckInWater() {
      if (this.posHistory.size() >= 100) {
         LivingEntity player = this.controller.getEntity();
         World world = this.controller.getWorld();
         if (world.getBlockState(player.getBlockPos()).isOf(Blocks.WATER)) {
            if (!player.isOnGround() && player.getAir() >= player.getMaxAir()) {
               Vec3d firstPos = this.posHistory.get(0);

               for (int i = 1; i < 100; i++) {
                  Vec3d nextPos = this.posHistory.get(i);
                  if (Math.abs(firstPos.getX() - nextPos.getX()) > 0.75 || Math.abs(firstPos.getZ() - nextPos.getZ()) > 0.75) {
                     return;
                  }
               }

               this.posHistory.clear();
               this.setTask(new GetOutOfWaterTask());
               this.isProbablyStuck = true;
            } else {
               this.posHistory.clear();
            }
         }
      }
   }

   private void checkStuckInPowderSnow() {
      LivingEntity player = this.controller.getEntity();
      if (player.inPowderSnow) {
         this.isProbablyStuck = true;
         BlockPos playerPos = player.getBlockPos();
         BlockPos toBreak = null;
         if (player.getWorld().getBlockState(playerPos).isOf(Blocks.POWDER_SNOW)) {
            toBreak = playerPos;
         } else if (player.getWorld().getBlockState(playerPos.up()).isOf(Blocks.POWDER_SNOW)) {
            toBreak = playerPos.up();
         }

         if (toBreak != null) {
            this.setTask(new DestroyBlockTask(toBreak));
         } else {
            this.setTask(new SafeRandomShimmyTask());
         }
      }
   }

   private void checkStuckOnEndPortalFrame() {
      BlockState standingOn = this.controller.getWorld().getBlockState(this.controller.getEntity().getSteppingPos());
      if (standingOn.isOf(Blocks.END_PORTAL_FRAME)
         && !(Boolean)standingOn.get(EndPortalFrameBlock.EYE)
         && !this.controller.getFoodChain().isTryingToEat()) {
         this.isProbablyStuck = true;
         this.controller.getBaritone().getInputOverrideHandler().setInputForceState(Input.MOVE_FORWARD, true);
      }
   }

   private void checkEatingGlitch() {
      FoodChain foodChain = this.controller.getFoodChain();
      if (this.interruptedEating) {
         foodChain.shouldStop(false);
         this.interruptedEating = false;
      }

      if (foodChain.isTryingToEat()) {
         this.eatingTicks++;
      } else {
         this.eatingTicks = 0;
      }

      if (this.eatingTicks > 140) {
         Debug.logMessage("Bot is probably stuck trying to eat. Resetting action.");
         foodChain.shouldStop(true);
         this.eatingTicks = 0;
         this.interruptedEating = true;
         this.isProbablyStuck = true;
      }
   }

   @Override
   public boolean isActive() {
      return true;
   }

   @Override
   protected void onTaskFinish(AltoClefController controller) {
   }

   @Override
   public String getName() {
      return "Unstuck Chain";
   }
}
