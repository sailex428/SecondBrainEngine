package adris.altoclef.tasks.container;

import adris.altoclef.AltoClefController;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasks.construction.PlaceBlockNearbyTask;
import adris.altoclef.tasks.movement.GetCloseToBlockTask;
import adris.altoclef.tasks.resources.CollectRecipeCataloguedResourcesTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.RecipeTarget;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.time.TimerGame;
import java.util.Arrays;
import java.util.Optional;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

public class CraftInTableTask extends ResourceTask {
   private final RecipeTarget[] targets;
   private BlockPos craftingTablePos = null;
   private final TimerGame craftTimer = new TimerGame(2.0);
   private boolean isCrafting = false;

   public CraftInTableTask(RecipeTarget[] targets) {
      super(extractItemTargets(targets));
      this.targets = targets;
   }

   public CraftInTableTask(RecipeTarget target) {
      this(new RecipeTarget[]{target});
   }

   private static ItemTarget[] extractItemTargets(RecipeTarget[] recipeTargets) {
      return Arrays.stream(recipeTargets).map(t -> new ItemTarget(t.getOutputItem(), t.getTargetCount())).toArray(ItemTarget[]::new);
   }

   @Override
   protected boolean shouldAvoidPickingUp(AltoClefController controller) {
      return false;
   }

   @Override
   protected void onResourceStart(AltoClefController controller) {
      controller.getBehaviour().push();

      for (RecipeTarget target : this.targets) {
         for (ItemTarget ingredient : target.getRecipe().getSlots()) {
            if (ingredient != null && !ingredient.isEmpty()) {
               controller.getBehaviour().addProtectedItems(ingredient.getMatches());
            }
         }
      }
   }

   @Override
   protected Task onResourceTick(AltoClefController controller) {
      boolean allDone = Arrays.stream(this.targets)
         .allMatch(targetx -> controller.getItemStorage().getItemCount(targetx.getOutputItem()) >= targetx.getTargetCount());
      if (allDone) {
         return null;
      } else if (!StorageHelper.hasRecipeMaterialsOrTarget(controller, this.targets)) {
         this.setDebugState("Collecting ingredients");
         return new CollectRecipeCataloguedResourcesTask(false, this.targets);
      } else {
         if (this.craftingTablePos == null || !controller.getWorld().getBlockState(this.craftingTablePos).isOf(Blocks.CRAFTING_TABLE)) {
            Optional<BlockPos> nearestTable = controller.getBlockScanner().getNearestBlock(Blocks.CRAFTING_TABLE);
            if (nearestTable.isPresent()) {
               this.craftingTablePos = nearestTable.get();
               this.setDebugState("Found crafting table: " + this.craftingTablePos.toShortString());
            } else {
               this.craftingTablePos = null;
               this.setDebugState("Crafting table not found.");
            }
         }

         if (this.craftingTablePos == null) {
            if (controller.getItemStorage().hasItem(Items.CRAFTING_TABLE)) {
               this.setDebugState("Placing crafting table.");
               return new PlaceBlockNearbyTask(Blocks.CRAFTING_TABLE);
            } else {
               this.setDebugState("Obtaining crafting table.");
               return TaskCatalogue.getItemTask(Items.CRAFTING_TABLE, 1);
            }
         } else if (!this.craftingTablePos
            .isWithinDistance(
               new Vec3i((int)controller.getEntity().getPos().x, (int)controller.getEntity().getPos().y, (int)controller.getEntity().getPos().z), 3.5
            )) {
            this.setDebugState("Going to crafting table at: " + this.craftingTablePos.toShortString());
            return new GetCloseToBlockTask(this.craftingTablePos);
         } else {
            this.setDebugState("Crafting...");
            if (!this.isCrafting) {
               this.craftTimer.reset();
               this.isCrafting = true;
            }

            if (!this.craftTimer.elapsed()) {
               return null;
            } else {
               for (RecipeTarget target : this.targets) {
                  int currentAmount = controller.getItemStorage().getItemCount(target.getOutputItem());
                  if (currentAmount < target.getTargetCount()) {
                     int craftsNeeded = (int)Math.ceil((double)(target.getTargetCount() - currentAmount) / target.getRecipe().outputCount());

                     for (int i = 0; i < craftsNeeded; i++) {
                        if (!StorageHelper.hasRecipeMaterialsOrTarget(
                           controller, new RecipeTarget(target.getOutputItem(), target.getRecipe().outputCount(), target.getRecipe())
                        )) {
                           Debug.logWarning("Not enough ingredients to craft, even though the check passed. Aborting.");
                           return new CollectRecipeCataloguedResourcesTask(false, this.targets);
                        }

                        PlayerInventory inventory = this.controller.getInventory();

                        for (ItemTarget ingredient : target.getRecipe().getSlots()) {
                           if (ingredient != null && !ingredient.isEmpty()) {
                              inventory.remove(stack -> ingredient.matches(stack.getItem()), ingredient.getTargetCount(), inventory);
                           }
                        }

                        ItemStack result = new ItemStack(target.getOutputItem(), target.getRecipe().outputCount());
                        inventory.insertStack(result);
                        controller.getItemStorage().registerSlotAction();
                     }
                  }
               }

               controller.getEntity().swingHand(Hand.MAIN_HAND);
               return null;
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
      return other instanceof CraftInTableTask task ? Arrays.equals((Object[])task.targets, (Object[])this.targets) : false;
   }

   @Override
   protected String toDebugStringName() {
      return "Craft on table: " + Arrays.toString(Arrays.stream(this.targets).map(t -> t.getOutputItem().getName().getString()).toArray());
   }

   public RecipeTarget[] getRecipeTargets() {
      return this.targets;
   }
}
