package me.sailex.altoclef.tasks.construction;

import me.sailex.altoclef.tasksystem.ITaskRequiresGrounded;
import me.sailex.altoclef.tasksystem.Task;
import me.sailex.automatone.api.process.IBuilderProcess;
import net.minecraft.util.math.BlockPos;

import java.util.Objects;

public class DestroyBlockTask extends Task implements ITaskRequiresGrounded {
   private final BlockPos pos;
   private boolean isClear;

   public DestroyBlockTask(BlockPos pos) {
      this.pos = pos;
   }

   @Override
   protected void onStart() {
      this.isClear = false;
      IBuilderProcess builder = this.controller.getBaritone().getBuilderProcess();
      builder.clearArea(this.pos, this.pos);
   }

   @Override
   protected Task onTick() {
      IBuilderProcess builder = this.controller.getBaritone().getBuilderProcess();
      if (!builder.isActive()) {
         this.isClear = true;
         return null;
      } else {
         this.setDebugState("Automatone is breaking the block.");
         return null;
      }
   }

   @Override
   protected void onStop(Task interruptTask) {
      IBuilderProcess builder = this.controller.getBaritone().getBuilderProcess();
      if (builder.isActive()) {
         builder.onLostControl();
      }
   }

   @Override
   public boolean isFinished() {
      return this.isClear || this.controller.getWorld().isAir(this.pos);
   }

   @Override
   protected boolean isEqual(Task other) {
      return other instanceof DestroyBlockTask task ? Objects.equals(task.pos, this.pos) : false;
   }

   @Override
   protected String toDebugString() {
      return "Destroying block at " + this.pos.toShortString();
   }
}
