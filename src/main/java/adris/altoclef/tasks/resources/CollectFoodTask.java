package adris.altoclef.tasks.resources;

import adris.altoclef.AltoClefController;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.multiversion.item.ItemVer;
import adris.altoclef.tasks.CraftInInventoryTask;
import adris.altoclef.tasks.container.CraftInTableTask;
import adris.altoclef.tasks.container.SmeltInSmokerTask;
import adris.altoclef.tasks.movement.PickupDroppedItemTask;
import adris.altoclef.tasks.movement.TimeoutWanderTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.*;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.time.TimerGame;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.passive.RabbitEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

public class CollectFoodTask extends Task {
   public static final CookableFoodTarget[] COOKABLE_FOODS = new CookableFoodTarget[]{
      new CookableFoodTarget("beef", CowEntity.class),
      new CookableFoodTarget("porkchop", PigEntity.class),
      new CookableFoodTarget("chicken", ChickenEntity.class),
      new CookableFoodTarget("mutton", SheepEntity.class),
      new CookableFoodTarget("rabbit", RabbitEntity.class)
   };
   public static final Item[] ITEMS_TO_PICK_UP = new Item[]{
      Items.ENCHANTED_GOLDEN_APPLE, Items.GOLDEN_APPLE, Items.GOLDEN_CARROT, Items.BREAD, Items.BAKED_POTATO
   };
   public static final CropTarget[] CROPS = new CropTarget[]{
      new CropTarget(Items.WHEAT, Blocks.WHEAT), new CropTarget(Items.CARROT, Blocks.CARROTS)
   };
   private final double unitsNeeded;
   private final TimerGame checkNewOptionsTimer = new TimerGame(10.0);
   private Task currentResourceTask = null;

   public CollectFoodTask(double unitsNeeded) {
      this.unitsNeeded = unitsNeeded;
   }

   @Override
   protected void onStart() {
      this.controller.getBehaviour().push();
      this.controller.getBehaviour().addProtectedItems(ITEMS_TO_PICK_UP);
      this.controller.getBehaviour().addProtectedItems(Items.HAY_BLOCK, Items.SWEET_BERRIES);
   }

   @Override
   protected Task onTick() {
      blackListChickenJockeys(this.controller);
      blacklistPillagerHayBales(this.controller);
      SmeltTarget toSmelt = this.getBestSmeltTarget(this.controller);
      if (toSmelt != null) {
         this.setDebugState("Smelting food");
         return new SmeltInSmokerTask(toSmelt);
      } else {
         if (this.checkNewOptionsTimer.elapsed()) {
            this.checkNewOptionsTimer.reset();
            this.currentResourceTask = null;
         }

         if (this.currentResourceTask != null
            && this.currentResourceTask.isActive()
            && !this.currentResourceTask.isFinished()
            && !this.currentResourceTask.thisOrChildAreTimedOut()) {
            return this.currentResourceTask;
         } else {
            double potentialFood = StorageHelper.calculateInventoryFoodScore(this.controller);
            if (potentialFood >= this.unitsNeeded) {
               if (this.controller.getItemStorage().getItemCount(Items.HAY_BLOCK) >= 1) {
                  this.setDebugState("Crafting wheat from hay");
                  this.currentResourceTask = new CraftInInventoryTask(
                     new RecipeTarget(Items.WHEAT, 9, CraftingRecipe.newShapedRecipe("wheat", new ItemTarget[]{new ItemTarget(Items.HAY_BLOCK, 1)}, 9))
                  );
                  return this.currentResourceTask;
               }

               if (this.controller.getItemStorage().getItemCount(Items.WHEAT) >= 3) {
                  this.setDebugState("Crafting bread");
                  this.currentResourceTask = new CraftInTableTask(
                     new RecipeTarget(Items.BREAD, 1, CraftingRecipe.newShapedRecipe("bread", new ItemTarget[]{new ItemTarget(Items.WHEAT, 3)}, 1))
                  );
                  return this.currentResourceTask;
               }
            }

            for (Item item : ITEMS_TO_PICK_UP) {
               if (this.controller.getEntityTracker().itemDropped(item)) {
                  this.setDebugState("Picking up high-value food: " + item.getName().getString());
                  this.currentResourceTask = new PickupDroppedItemTask(new ItemTarget(item), true);
                  return this.currentResourceTask;
               }
            }

            for (CookableFoodTarget cookable : COOKABLE_FOODS) {
               if (this.controller.getEntityTracker().itemDropped(cookable.getRaw(), cookable.getCooked())) {
                  this.setDebugState("Picking up dropped meat");
                  this.currentResourceTask = new PickupDroppedItemTask(new ItemTarget(cookable.getRaw(), cookable.getCooked()), true);
                  return this.currentResourceTask;
               }
            }

            if (this.controller.getBlockScanner().anyFound(Blocks.HAY_BLOCK)) {
               this.setDebugState("Collecting hay bales");
               this.currentResourceTask = new MineAndCollectTask(new ItemTarget(Items.HAY_BLOCK, 9999), new Block[]{Blocks.HAY_BLOCK}, MiningRequirement.HAND);
               return this.currentResourceTask;
            } else {
               for (CropTarget crop : CROPS) {
                  if (this.controller.getBlockScanner().anyFound(pos -> isCropMature(this.controller, pos, crop.cropBlock), crop.cropBlock)) {
                     this.setDebugState("Harvesting " + crop.cropItem.getName().getString());
                     this.currentResourceTask = new CollectCropTask(new ItemTarget(crop.cropItem, 9999), new Block[]{crop.cropBlock}, crop.cropItem);
                     return this.currentResourceTask;
                  }
               }

               Entity bestEntityToKill = this.getBestAnimalToKill(this.controller);
               if (bestEntityToKill != null) {
                  this.setDebugState("Killing " + bestEntityToKill.getType().getName().getString());
                  Item rawFood = Arrays.stream(COOKABLE_FOODS).filter(c -> c.mobToKill == bestEntityToKill.getClass()).findFirst().get().getRaw();
                  this.currentResourceTask = new KillAndLootTask(bestEntityToKill.getClass(), new ItemTarget(rawFood, 1));
                  return this.currentResourceTask;
               } else if (this.controller.getBlockScanner().anyFound(Blocks.SWEET_BERRY_BUSH)) {
                  this.setDebugState("Collecting sweet berries");
                  this.currentResourceTask = new MineAndCollectTask(
                     new ItemTarget(Items.SWEET_BERRIES, 9999), new Block[]{Blocks.SWEET_BERRY_BUSH}, MiningRequirement.HAND
                  );
                  return this.currentResourceTask;
               } else {
                  this.setDebugState("Searching for food source...");
                  return new TimeoutWanderTask();
               }
            }
         }
      }
   }

   @Override
   protected void onStop(Task interruptTask) {
      this.controller.getBehaviour().pop();
   }

   @Override
   public boolean isFinished() {
      return StorageHelper.calculateInventoryFoodScore(this.controller) >= this.unitsNeeded;
   }

   @Override
   protected boolean isEqual(Task other) {
      return other instanceof CollectFoodTask task ? task.unitsNeeded == this.unitsNeeded : false;
   }

   @Override
   protected String toDebugString() {
      return "Collecting " + this.unitsNeeded + " food units.";
   }

   private SmeltTarget getBestSmeltTarget(AltoClefController controller) {
      for (CookableFoodTarget cookable : COOKABLE_FOODS) {
         int rawCount = controller.getItemStorage().getItemCount(cookable.getRaw());
         if (rawCount > 0) {
            return new SmeltTarget(new ItemTarget(cookable.getCooked(), rawCount), new ItemTarget(cookable.getRaw(), rawCount));
         }
      }

      return null;
   }

   private Entity getBestAnimalToKill(AltoClefController controller) {
      double bestScore = -1.0;
      Entity bestEntity = null;
      Predicate<Entity> notBaby = entity -> entity instanceof LivingEntity && !((LivingEntity)entity).isBaby();

      for (CookableFoodTarget cookable : COOKABLE_FOODS) {
         if (controller.getEntityTracker().entityFound(cookable.mobToKill)) {
            Optional<Entity> nearest = controller.getEntityTracker().getClosestEntity(controller.getEntity().getPos(), notBaby, cookable.mobToKill);
            if (nearest.isPresent()) {
               double distanceSq = nearest.get().getPos().squaredDistanceTo(controller.getEntity().getPos());
               if (distanceSq != 0.0) {
                  double score = cookable.getCookedUnits() / distanceSq;
                  if (score > bestScore) {
                     bestScore = score;
                     bestEntity = nearest.get();
                  }
               }
            }
         }
      }

      return bestEntity;
   }

   public static void blackListChickenJockeys(AltoClefController controller) {
      for (ChickenEntity chicken : controller.getEntityTracker().getTrackedEntities(ChickenEntity.class)) {
         if (chicken.hasPassengers()) {
            controller.getEntityTracker().requestEntityUnreachable(chicken);
         }
      }
   }

   private static void blacklistPillagerHayBales(AltoClefController controller) {
      for (BlockPos pos : controller.getBlockScanner().getKnownLocations(Blocks.HAY_BLOCK)) {
         if (controller.getWorld().getBlockState(pos.up()).isOf(Blocks.CARVED_PUMPKIN)) {
            controller.getBlockScanner().requestBlockUnreachable(pos, 0);
         }
      }
   }

   private static boolean isCropMature(AltoClefController controller, BlockPos pos, Block block) {
      if (!controller.getChunkTracker().isChunkLoaded(pos)) {
         return false;
      } else {
         return controller.getWorld().getBlockState(pos).getBlock() instanceof CropBlock crop ? crop.isMature(controller.getWorld().getBlockState(pos)) : true;
      }
   }

   public static double calculateFoodPotential(AltoClefController mod) {
      double potentialFood = 0.0;

      for (ItemStack food : mod.getItemStorage().getItemStacksPlayerInventory(true)) {
         potentialFood += CollectMeatTask.getFoodPotential(food);
      }

      int potentialBread = mod.getItemStorage().getItemCount(Items.WHEAT) / 3 + mod.getItemStorage().getItemCount(Items.HAY_BLOCK) * 3;
      return potentialFood + Objects.requireNonNull(ItemVer.getFoodComponent(Items.BREAD)).getHunger() * potentialBread;
   }

   public static class CookableFoodTarget {
      public final String rawFood;
      public final String cookedFood;
      public final Class<? extends Entity> mobToKill;

      public CookableFoodTarget(String rawFood, Class<? extends Entity> mobToKill) {
         this(rawFood, "cooked_" + rawFood, mobToKill);
      }

      public CookableFoodTarget(String rawFood, String cookedFood, Class<? extends Entity> mobToKill) {
         this.rawFood = rawFood;
         this.cookedFood = cookedFood;
         this.mobToKill = mobToKill;
      }

      public Item getRaw() {
         return TaskCatalogue.getItemMatches(this.rawFood)[0];
      }

      public Item getCooked() {
         return TaskCatalogue.getItemMatches(this.cookedFood)[0];
      }

      public int getCookedUnits() {
         return ItemVer.getFoodComponent(this.getCooked()).getHunger();
      }
   }

   public static class CropTarget {
      public final Item cropItem;
      public final Block cropBlock;

      public CropTarget(Item cropItem, Block cropBlock) {
         this.cropItem = cropItem;
         this.cropBlock = cropBlock;
      }
   }
}
