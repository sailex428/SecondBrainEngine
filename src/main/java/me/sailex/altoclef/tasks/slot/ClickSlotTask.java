package me.sailex.altoclef.tasks.slot;

import me.sailex.altoclef.control.SlotHandler;
import me.sailex.altoclef.tasksystem.Task;
import me.sailex.altoclef.util.slots.Slot;
import net.minecraft.screen.slot.SlotActionType;

public class ClickSlotTask extends Task {
   private final Slot slot;
   private final int mouseButton;
   private final SlotActionType type;
   private boolean clicked = false;

   public ClickSlotTask(Slot slot, int mouseButton, SlotActionType type) {
      this.slot = slot;
      this.mouseButton = mouseButton;
      this.type = type;
   }

   public ClickSlotTask(Slot slot, SlotActionType type) {
      this(slot, 0, type);
   }

   public ClickSlotTask(Slot slot, int mouseButton) {
      this(slot, mouseButton, SlotActionType.PICKUP);
   }

   public ClickSlotTask(Slot slot) {
      this(slot, SlotActionType.PICKUP);
   }

   @Override
   protected void onStart() {
      this.clicked = false;
   }

   @Override
   protected Task onTick() {
      SlotHandler slotHandler = this.controller.getSlotHandler();
      if (slotHandler.canDoSlotAction()) {
         slotHandler.clickSlot(this.slot, this.mouseButton, this.type);
         slotHandler.registerSlotAction();
         this.clicked = true;
      }

      return null;
   }

   @Override
   protected void onStop(Task interruptTask) {
   }

   @Override
   protected boolean isEqual(Task obj) {
      return !(obj instanceof ClickSlotTask task) ? false : task.mouseButton == this.mouseButton && task.type == this.type && task.slot.equals(this.slot);
   }

   @Override
   protected String toDebugString() {
      return "Clicking " + this.slot.toString();
   }

   @Override
   public boolean isFinished() {
      return this.clicked;
   }
}
