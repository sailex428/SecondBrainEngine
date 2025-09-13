package me.sailex.altoclef.tasks.container;

import me.sailex.altoclef.AltoClefController;
import me.sailex.altoclef.Debug;
import me.sailex.altoclef.TaskCatalogue;
import me.sailex.altoclef.mixins.MixinAbstractFurnaceBlockEntity;
import me.sailex.altoclef.tasks.ResourceTask;
import me.sailex.altoclef.tasks.construction.PlaceBlockNearbyTask;
import me.sailex.altoclef.tasks.movement.GetCloseToBlockTask;
import me.sailex.altoclef.tasks.movement.TimeoutWanderTask;
import me.sailex.altoclef.tasks.resources.CollectFuelTask;
import me.sailex.altoclef.tasksystem.Task;
import me.sailex.altoclef.util.ItemTarget;
import me.sailex.altoclef.util.SmeltTarget;
import me.sailex.altoclef.util.helpers.StorageHelper;
import me.sailex.altoclef.util.time.TimerGame;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class SmeltInFurnaceTask extends ResourceTask {
   private final SmeltTarget[] targets;
   private final TimerGame smeltTimer = new TimerGame(10.0);
   private BlockPos furnacePos = null;
   private boolean isSmelting = false;

   public SmeltInFurnaceTask(SmeltTarget... targets) {
      super(extractItemTargets(targets));
      this.targets = targets;
   }

   public SmeltInFurnaceTask(SmeltTarget target) {
      this(new SmeltTarget[]{target});
   }

   private static ItemTarget[] extractItemTargets(SmeltTarget[] recipeTargets) {
      List<ItemTarget> result = new ArrayList<>(recipeTargets.length);

      for (SmeltTarget target : recipeTargets) {
         result.add(target.getItem());
      }

      return result.toArray(ItemTarget[]::new);
   }

   @Override
   protected boolean shouldAvoidPickingUp(AltoClefController controller) {
      return false;
   }

   @Override
   protected void onResourceStart(AltoClefController controller) {
      controller.getBehaviour().push();
      controller.getBehaviour().addProtectedItems(Items.FURNACE);

      for (SmeltTarget target : this.targets) {
         controller.getBehaviour().addProtectedItems(target.getMaterial().getMatches());
      }
   }

   @Override
   protected Task onResourceTick(AltoClefController controller) {
      boolean allDone = Arrays.stream(this.targets)
         .allMatch(target -> controller.getItemStorage().getItemCount(target.getItem()) >= target.getItem().getTargetCount());
      if (allDone) {
         this.setDebugState("Done smelting.");
         return null;
      } else {
         SmeltTarget currentTarget = null;

         for (SmeltTarget target : this.targets) {
            if (controller.getItemStorage().getItemCount(target.getItem()) < target.getItem().getTargetCount()) {
               currentTarget = target;
               break;
            }
         }

         if (currentTarget == null) {
            Debug.logWarning("Smelting task is running, but all targets are met. This should not happen.");
            return null;
         } else {
            this.smeltTimer.setInterval(10 * currentTarget.getItem().getTargetCount());
            int fuelNeeded = (int)Math.ceil(currentTarget.getItem().getTargetCount() / 8.0);
            if (!this.isSmelting) {
               if (controller.getItemStorage().getItemCount(currentTarget.getMaterial()) < currentTarget.getMaterial().getTargetCount()) {
                  this.setDebugState("Collecting materials for smelting: " + currentTarget.getMaterial());
                  return TaskCatalogue.getItemTask(currentTarget.getMaterial());
               }

               if (StorageHelper.calculateInventoryFuelCount(controller) < fuelNeeded) {
                  this.setDebugState("Collecting fuel.");
                  return new CollectFuelTask(fuelNeeded);
               }
            }

            if (this.furnacePos == null || !controller.getWorld().getBlockState(this.furnacePos).isOf(Blocks.FURNACE)) {
               Optional<BlockPos> nearestFurnace = controller.getBlockScanner().getNearestBlock(Blocks.FURNACE);
               if (nearestFurnace.isPresent()
                  && !nearestFurnace.get()
                     .isWithinDistance(
                        new Vec3i((int)controller.getEntity().getPos().x, (int)controller.getEntity().getPos().y, (int)controller.getEntity().getPos().z),
                        100.0
                     )) {
                  nearestFurnace = Optional.empty();
               }

               if (!nearestFurnace.isPresent()) {
                  if (controller.getItemStorage().hasItem(Items.FURNACE)) {
                     this.setDebugState("Placing furnace.");
                     return new PlaceBlockNearbyTask(Blocks.FURNACE);
                  }

                  this.setDebugState("Obtaining furnace.");
                  return TaskCatalogue.getItemTask(Items.FURNACE, 1);
               }

               this.furnacePos = nearestFurnace.get();
            }

            if (!this.furnacePos
               .isWithinDistance(
                  new Vec3i((int)controller.getEntity().getPos().x, (int)controller.getEntity().getPos().y, (int)controller.getEntity().getPos().z), 4.5
               )) {
               this.setDebugState("Going to furnace.");
               return new GetCloseToBlockTask(this.furnacePos);
            } else if (controller.getWorld().getBlockEntity(this.furnacePos) instanceof AbstractFurnaceBlockEntity furnace) {
               ItemStack outputStack = furnace.getStack(2);
               if (!outputStack.isEmpty()) {
                  this.setDebugState("Taking smelted items.");
                  PlayerInventory playerInv = this.controller.getInventory();
                  if (!playerInv.insertStack(outputStack)) {
                     this.setDebugState("Inventory is full, cannot take smelted items.");
                     return null;
                  }

                  furnace.setStack(2, ItemStack.EMPTY);
                  furnace.markDirty();
               }

               if (this.isSmelting) {
                  this.setDebugState("Waiting for items to smelt...");
                  if (this.smeltTimer.elapsed()) {
                     this.isSmelting = false;
                  }

                  return null;
               } else {
                  ItemStack materialSlot = furnace.getStack(0);
                  ItemStack fuelSlot = furnace.getStack(1);
                  PlayerInventory playerInv = this.controller.getInventory();
                  if (((MixinAbstractFurnaceBlockEntity)furnace).getPropertyDelegate().get(0) <= 1 && fuelSlot.isEmpty()) {
                     this.setDebugState("Adding fuel.");
                     Item fuelItem = controller.getModSettings().getSupportedFuelItems()[0];
                     int fuelSlotIndex = playerInv.getSlotWithStack(new ItemStack(fuelItem));
                     if (fuelSlotIndex != -1) {
                        furnace.setStack(1, playerInv.removeStack(fuelSlotIndex, fuelNeeded));
                        furnace.markDirty();
                     }
                  }

                  if (materialSlot.isEmpty()) {
                     this.setDebugState("Adding material.");
                     Item materialItem = currentTarget.getMaterial().getMatches()[0];
                     int materialSlotIndex = playerInv.getSlotWithStack(new ItemStack(materialItem));
                     if (materialSlotIndex != -1) {
                        furnace.setStack(0, playerInv.removeStack(materialSlotIndex,
                                currentTarget.getMaterial().getTargetCount()));
                        this.isSmelting = true;
                        this.smeltTimer.reset();
                        furnace.markDirty();
                        return null;
                     }
                  }

                  this.isSmelting = true;
                  this.smeltTimer.reset();
                  this.setDebugState("Waiting for furnace...");
                  return null;
               }
            } else {
               Debug.logWarning("Block at furnace position is not a furnace BE. Resetting.");
               this.furnacePos = null;
               return new TimeoutWanderTask(1.0F);
            }
         }
      }
   }

   @Override
   protected void onResourceStop(AltoClefController controller, Task interruptTask) {
      controller.getBehaviour().pop();
   }

   @Override
   protected boolean isEqualResource(ResourceTask other) {
      return other instanceof SmeltInFurnaceTask task ? Arrays.equals((Object[])task.targets, (Object[])this.targets) : false;
   }

   @Override
   protected String toDebugStringName() {
      return "Smelting in Furnace";
   }
}
