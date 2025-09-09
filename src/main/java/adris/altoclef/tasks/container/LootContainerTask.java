package adris.altoclef.tasks.container;

import adris.altoclef.Debug;
import adris.altoclef.tasks.movement.GetToBlockTask;
import adris.altoclef.tasks.slot.EnsureFreeInventorySlotTask;
import adris.altoclef.tasksystem.Task;
import baritone.api.entity.IInventoryProvider;
import baritone.api.entity.LivingEntityInventory;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

public class LootContainerTask extends Task {
   private final BlockPos containerPos;
   private final List<Item> targets;
   private final Predicate<ItemStack> check;
   private boolean finished = false;

   public LootContainerTask(BlockPos chestPos, List<Item> items, Predicate<ItemStack> pred) {
      this.containerPos = chestPos;
      this.targets = items;
      this.check = pred;
   }

   public LootContainerTask(BlockPos chestPos, List<Item> items) {
      this(chestPos, items, itemStack -> true);
   }

   @Override
   protected void onStart() {
      this.controller.getBehaviour().push();
      this.controller.getBehaviour().addProtectedItems(this.targets.toArray(new Item[0]));
   }

   @Override
   protected Task onTick() {
      if (this.finished) {
         return null;
      } else if (!this.containerPos
         .isWithinDistance(
            new Vec3i(
               (int)this.controller.getEntity().getPos().x, (int)this.controller.getEntity().getPos().y, (int)this.controller.getEntity().getPos().z
            ),
            4.5
         )) {
         this.setDebugState("Going to container");
         return new GetToBlockTask(this.containerPos);
      } else if (this.controller.getWorld().getBlockEntity(this.containerPos) instanceof LootableContainerBlockEntity container) {
         LootableContainerBlockEntity containerInventory = container;
         LivingEntityInventory playerInventory = ((IInventoryProvider)this.controller.getEntity()).getLivingInventory();
         this.controller.getItemStorage().containers.WritableCache(this.controller, this.containerPos);
         boolean somethingToLoot = false;
         this.setDebugState("Looting items: " + this.targets);

         for (int i = 0; i < containerInventory.size(); i++) {
            ItemStack stack = containerInventory.getStack(i);
            if (!stack.isEmpty() && this.targets.contains(stack.getItem()) && this.check.test(stack)) {
               somethingToLoot = true;
               if (!playerInventory.insertStack(new ItemStack(stack.getItem()))) {
                  this.setDebugState("Inventory is full, ensuring space.");
                  return new EnsureFreeInventorySlotTask();
               }

               if (playerInventory.insertStack(stack.copy())) {
                  containerInventory.setStack(i, ItemStack.EMPTY);
                  container.markDirty();
                  this.controller.getItemStorage().registerSlotAction();
                  return null;
               }

               Debug.logWarning("Failed to insert stack even after checking for space.");
            }
         }

         if (!somethingToLoot) {
            this.setDebugState("Container empty or has no desired items.");
            this.finished = true;
         }

         return null;
      } else {
         Debug.logWarning("Block at " + this.containerPos + " is not a lootable container. Stopping.");
         this.finished = true;
         return null;
      }
   }

   @Override
   public boolean isFinished() {
      return this.finished
         || !this.controller.getChunkTracker().isChunkLoaded(this.containerPos)
         || !(this.controller.getWorld().getBlockEntity(this.containerPos) instanceof LootableContainerBlockEntity);
   }

   @Override
   protected void onStop(Task interruptTask) {
      this.controller.getBehaviour().pop();
   }

   @Override
   protected boolean isEqual(Task other) {
      return !(other instanceof LootContainerTask task)
         ? false
         : Objects.equals(task.containerPos, this.containerPos) && new ArrayList<>(task.targets).equals(new ArrayList<>(this.targets));
   }

   @Override
   protected String toDebugString() {
      return "Looting container at " + this.containerPos.toShortString();
   }
}
