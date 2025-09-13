package me.sailex.altoclef.tasks.slot;

import me.sailex.altoclef.tasksystem.Task;
import me.sailex.altoclef.util.slots.Slot;

public class ThrowCursorTask extends Task {
   private final Task throwTask = new ClickSlotTask(Slot.UNDEFINED);

   @Override
   protected void onStart() {
   }

   @Override
   protected Task onTick() {
      return this.throwTask;
   }

   @Override
   protected void onStop(Task interruptTask) {
   }

   @Override
   protected boolean isEqual(Task obj) {
      return obj instanceof ThrowCursorTask;
   }

   @Override
   protected String toDebugString() {
      return "Throwing Cursor";
   }

   @Override
   public boolean isFinished() {
      return this.throwTask.isFinished();
   }
}
