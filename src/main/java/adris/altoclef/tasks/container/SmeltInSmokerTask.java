package adris.altoclef.tasks.container;

import adris.altoclef.AltoClefController;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.mixins.MixinAbstractFurnaceBlockEntity;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasks.construction.PlaceBlockNearbyTask;
import adris.altoclef.tasks.movement.GetCloseToBlockTask;
import adris.altoclef.tasks.movement.TimeoutWanderTask;
import adris.altoclef.tasks.resources.CollectFuelTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.SmeltTarget;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.time.TimerGame;
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

public class SmeltInSmokerTask extends ResourceTask {
   private final SmeltTarget[] targets;
   private final TimerGame smeltTimer = new TimerGame(5.0);
   private BlockPos smokerPos = null;
   private boolean isSmelting = false;
   private SmokerCache cache;

   public SmeltInSmokerTask(SmeltTarget... targets) {
      super(extractItemTargets(targets));
      this.targets = targets;
   }

   public SmeltInSmokerTask(SmeltTarget target) {
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
      controller.getBehaviour().addProtectedItems(Items.SMOKER);

      for (SmeltTarget target : this.targets) {
         controller.getBehaviour().addProtectedItems(target.getMaterial().getMatches());
      }
   }

   @Override
   protected Task onResourceTick(AltoClefController controller) {
      boolean allDone = Arrays.stream(this.targets)
         .allMatch(target -> controller.getItemStorage().getItemCount(target.getItem()) >= target.getItem().getTargetCount());
      if (allDone) {
         this.setDebugState("Done smoking.");
         return null;
      } else {
         SmeltTarget currentTarget = Arrays.stream(this.targets)
            .filter(t -> controller.getItemStorage().getItemCount(t.getItem()) < t.getItem().getTargetCount())
            .findFirst()
            .orElse(null);
         if (currentTarget == null) {
            return null;
         } else {
            this.smeltTimer.setInterval(10 * currentTarget.getItem().getTargetCount());
            int fuelNeeded = (int)Math.ceil(currentTarget.getItem().getTargetCount() / 8.0);
            if (!this.isSmelting) {
               if (!controller.getItemStorage().hasItem(currentTarget.getMaterial())) {
                  this.setDebugState("Collecting raw food: " + currentTarget.getMaterial());
                  return TaskCatalogue.getItemTask(currentTarget.getMaterial());
               }

               if (StorageHelper.calculateInventoryFuelCount(controller) < fuelNeeded) {
                  this.setDebugState("Collecting fuel.");
                  return new CollectFuelTask(fuelNeeded);
               }
            }

            if (this.smokerPos == null || !controller.getWorld().getBlockState(this.smokerPos).isOf(Blocks.SMOKER)) {
               Optional<BlockPos> nearestSmoker = controller.getBlockScanner().getNearestBlock(Blocks.SMOKER);
               if (!nearestSmoker.isPresent()) {
                  if (controller.getItemStorage().hasItem(Items.SMOKER)) {
                     this.setDebugState("Placing smoker.");
                     return new PlaceBlockNearbyTask(Blocks.SMOKER);
                  }

                  this.setDebugState("Obtaining smoker.");
                  return TaskCatalogue.getItemTask(Items.SMOKER, 1);
               }

               this.smokerPos = nearestSmoker.get();
            }

            if (!this.smokerPos
               .isWithinDistance(
                  new Vec3i((int)controller.getEntity().getPos().x, (int)controller.getEntity().getPos().y, (int)controller.getEntity().getPos().z), 4.5
               )) {
               this.setDebugState("Going to smoker.");
               return new GetCloseToBlockTask(this.smokerPos);
            } else if (controller.getWorld().getBlockEntity(this.smokerPos) instanceof AbstractFurnaceBlockEntity smoker) {
               ItemStack outputStack = smoker.getStack(2);
               if (!outputStack.isEmpty()) {
                  this.setDebugState("Taking smoked items.");
                  PlayerInventory playerInv = this.controller.getInventory();
                  if (!playerInv.insertStack(outputStack)) {
                     this.setDebugState("Inventory full.");
                     return null;
                  }

                  smoker.setStack(2, ItemStack.EMPTY);
                  smoker.markDirty();
               }

               if (this.isSmelting) {
                  this.setDebugState("Waiting for items to smoke...");
                  if (this.smeltTimer.elapsed()) {
                     this.isSmelting = false;
                  }

                  return null;
               } else {
                  PlayerInventory playerInv = this.controller.getInventory();
                  if (((MixinAbstractFurnaceBlockEntity)smoker).getPropertyDelegate().get(0) <= 1 && smoker.getStack(1).isEmpty()) {
                     this.setDebugState("Adding fuel.");
                     Item fuelItem = controller.getModSettings().getSupportedFuelItems()[0];
                     int fuelSlotIndex = playerInv.getSlotWithStack(new ItemStack(fuelItem));
                     if (fuelSlotIndex != -1) {
                        smoker.setStack(1, playerInv.removeStack(fuelSlotIndex, fuelNeeded));
                        smoker.markDirty();
                     }
                  }

                  if (smoker.getStack(0).isEmpty()) {
                     this.setDebugState("Adding raw food.");
                     Item materialItem = currentTarget.getMaterial().getMatches()[0];
                     int materialSlotIndex = playerInv.getSlotWithStack(new ItemStack(materialItem));
                     if (materialSlotIndex != -1) {
                        smoker.setStack(0, playerInv.removeStack(materialSlotIndex,
                                currentTarget.getMaterial().getTargetCount()));
                        this.isSmelting = true;
                        this.smeltTimer.reset();
                        smoker.markDirty();
                        return null;
                     }
                  }

                  this.isSmelting = true;
                  this.smeltTimer.reset();
                  this.setDebugState("Waiting for smoker...");
                  return null;
               }
            } else {
               Debug.logWarning("Block at smoker position is not a smoker BE. Resetting.");
               this.smokerPos = null;
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
      return other instanceof SmeltInSmokerTask task ? Arrays.equals((Object[])task.targets, (Object[])this.targets) : false;
   }

   @Override
   protected String toDebugStringName() {
      return "Smelting in Smoker";
   }

   static class SmokerCache {
      public ItemStack materialSlot = ItemStack.EMPTY;
      public ItemStack fuelSlot = ItemStack.EMPTY;
      public ItemStack outputSlot = ItemStack.EMPTY;
      public double burningFuelCount;
      public double burnPercentage;
   }
}
