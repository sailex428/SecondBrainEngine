package me.sailex.altoclef.chains;

import me.sailex.altoclef.AltoClefController;
import me.sailex.altoclef.Settings;
import me.sailex.altoclef.multiversion.FoodComponentWrapper;
import me.sailex.altoclef.multiversion.item.ItemVer;
import me.sailex.altoclef.tasks.resources.CollectFoodTask;
import me.sailex.altoclef.tasks.speedrun.DragonBreathTracker;
import me.sailex.altoclef.tasksystem.TaskRunner;
import me.sailex.altoclef.util.ItemTarget;
import me.sailex.altoclef.util.helpers.ConfigHelper;
import me.sailex.altoclef.util.helpers.WorldHelper;
import me.sailex.automatone.api.utils.input.Input;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

public class FoodChain extends SingleTaskChain {
   private static FoodChainConfig config;
   private static boolean hasFood;
   private final DragonBreathTracker dragonBreathTracker = new DragonBreathTracker();
   private boolean isTryingToEat = false;
   private boolean requestFillup = false;
   private boolean needsToCollectFood = false;
   private Optional<Item> cachedPerfectFood = Optional.empty();
   private boolean shouldStop = false;

   public FoodChain(TaskRunner runner) {
      super(runner);
   }

   @Override
   protected void onTaskFinish(AltoClefController controller) {
   }

   private void startEat(AltoClefController controller, Item food) {
      controller.getSlotHandler().forceEquipItem(new ItemTarget(food), true);
      controller.getBaritone().getInputOverrideHandler().setInputForceState(Input.CLICK_RIGHT, true);
      controller.getExtraBaritoneSettings().setInteractionPaused(true);
      this.isTryingToEat = true;
      this.requestFillup = true;
   }

   private void stopEat(AltoClefController controller) {
      if (this.isTryingToEat) {
         controller.getBaritone().getInputOverrideHandler().setInputForceState(Input.CLICK_RIGHT, false);
         controller.getExtraBaritoneSettings().setInteractionPaused(false);
         this.isTryingToEat = false;
         this.requestFillup = false;
         if (controller.getItemStorage().hasItem(Items.SHIELD) && !controller.getItemStorage().hasItemInOffhand(controller, Items.SHIELD)) {
            controller.getSlotHandler().forceEquipItemToOffhand(Items.SHIELD);
         }
      }
   }

   public boolean isTryingToEat() {
      return this.isTryingToEat;
   }

   @Override
   public float getPriority() {
      if (this.controller == null) {
         return Float.NEGATIVE_INFINITY;
      } else if (WorldHelper.isInNetherPortal(this.controller)) {
         this.stopEat(this.controller);
         return Float.NEGATIVE_INFINITY;
      } else if (this.controller.getMobDefenseChain().isShielding()) {
         this.stopEat(this.controller);
         return Float.NEGATIVE_INFINITY;
      } else {
         this.dragonBreathTracker.updateBreath(this.controller);

         for (BlockPos playerIn : WorldHelper.getBlocksTouchingPlayer(this.controller.getEntity())) {
            if (this.dragonBreathTracker.isTouchingDragonBreath(playerIn)) {
               this.stopEat(this.controller);
               return Float.NEGATIVE_INFINITY;
            }
         }

         if (this.controller.getModSettings().isAutoEat() && !this.controller.getEntity().isInLava() && !this.shouldStop) {
            if (this.controller.getMLGBucketChain().doneMLG() && !this.controller.getMLGBucketChain().isFalling(this.controller)) {
               Pair<Integer, Optional<Item>> calculation = this.calculateFood(this.controller);
               int foodScore = (Integer)calculation.getLeft();
               this.cachedPerfectFood = (Optional<Item>)calculation.getRight();
               hasFood = foodScore > 0;
               if (this.requestFillup && this.controller.getBaritone().getPlayerContext().entity().getHungerManager().getFoodLevel() >= 20) {
                  this.requestFillup = false;
               }

               if (!hasFood) {
                  this.requestFillup = false;
               }

               if (hasFood && (this.needsToEat() || this.requestFillup) && this.cachedPerfectFood.isPresent()) {
                  this.startEat(this.controller, this.cachedPerfectFood.get());
               } else {
                  this.stopEat(this.controller);
               }

               Settings settings = this.controller.getModSettings();
               if (this.needsToCollectFood || foodScore < settings.getMinimumFoodAllowed()) {
                  this.needsToCollectFood = foodScore < settings.getFoodUnitsToCollect();
                  if (this.needsToCollectFood) {
                     this.setTask(new CollectFoodTask(settings.getFoodUnitsToCollect()));
                     return 55.0F;
                  }
               }

               this.setTask(null);
               return Float.NEGATIVE_INFINITY;
            } else {
               this.stopEat(this.controller);
               return Float.NEGATIVE_INFINITY;
            }
         } else {
            this.stopEat(this.controller);
            return Float.NEGATIVE_INFINITY;
         }
      }
   }

   @Override
   public String getName() {
      return "Food chain";
   }

   @Override
   protected void onStop() {
      super.onStop();
      if (this.controller != null) {
         this.stopEat(this.controller);
      }
   }

   public boolean needsToEat() {
      if (hasFood && !this.shouldStop) {
         LivingEntity player = this.controller.getEntity();
         int foodLevel = this.controller.getBaritone().getPlayerContext().entity().getHungerManager().getFoodLevel();
         float health = player.getHealth();
         if (foodLevel >= 20) {
            return false;
         } else if (health <= 10.0F) {
            return true;
         } else if (player.isOnFire() || player.hasStatusEffect(StatusEffects.WITHER) || health < config.alwaysEatWhenWitherOrFireAndHealthBelow) {
            return true;
         } else if (foodLevel <= config.alwaysEatWhenBelowHunger) {
            return true;
         } else if (health < config.alwaysEatWhenBelowHealth) {
            return true;
         } else if (foodLevel < config.alwaysEatWhenBelowHungerAndPerfectFit && this.cachedPerfectFood.isPresent()) {
            int need = 20 - foodLevel;
            Item best = this.cachedPerfectFood.get();
            int fills = Optional.ofNullable(ItemVer.getFoodComponent(best)).map(FoodComponentWrapper::getHunger).orElse(-1);
            return fills > 0 && fills <= need;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private Pair<Integer, Optional<Item>> calculateFood(AltoClefController controller) {
      Item bestFood = null;
      double bestFoodScore = Double.NEGATIVE_INFINITY;
      int foodTotal = 0;
      LivingEntity player = controller.getEntity();
      float health = player.getHealth();
      float hunger = controller.getBaritone().getPlayerContext().entity().getHungerManager().getFoodLevel();
      float saturation = controller.getBaritone().getPlayerContext().entity().getHungerManager().getSaturationLevel();

      for (ItemStack stack : controller.getItemStorage().getItemStacksPlayerInventory(true)) {
         if (ItemVer.isFood(stack) && !stack.isOf(Items.SPIDER_EYE)) {
            FoodComponentWrapper food = ItemVer.getFoodComponent(stack.getItem());
            if (food != null) {
               float hungerIfEaten = Math.min(hunger + food.getHunger(), 20.0F);
               float saturationIfEaten = Math.min(hungerIfEaten, saturation + food.getSaturationModifier());
               float gainedSaturation = saturationIfEaten - saturation;
               float gainedHunger = hungerIfEaten - hunger;
               float hungerWasted = food.getHunger() - gainedHunger;
               float score = gainedSaturation * 2.0F - hungerWasted;
               if (stack.isOf(Items.ROTTEN_FLESH)) {
                  score -= 100.0F;
               }

               if (score > bestFoodScore) {
                  bestFoodScore = score;
                  bestFood = stack.getItem();
               }

               foodTotal += food.getHunger() * stack.getCount();
            }
         }
      }

      return new Pair(foodTotal, Optional.ofNullable(bestFood));
   }

   public boolean hasFood() {
      return hasFood;
   }

   public void shouldStop(boolean shouldStopInput) {
      this.shouldStop = shouldStopInput;
   }

   public boolean isShouldStop() {
      return this.shouldStop;
   }

   static {
      ConfigHelper.loadConfig(
         "configs/food_chain_settings.json", FoodChainConfig::new, FoodChainConfig.class, newConfig -> config = newConfig
      );
   }

   static class FoodChainConfig {
      public int alwaysEatWhenWitherOrFireAndHealthBelow = 6;
      public int alwaysEatWhenBelowHunger = 10;
      public int alwaysEatWhenBelowHealth = 14;
      public int alwaysEatWhenBelowHungerAndPerfectFit = 15;
   }
}
