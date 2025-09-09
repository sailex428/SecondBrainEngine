package adris.altoclef.tasks.movement;

import adris.altoclef.AltoClefController;
import adris.altoclef.multiversion.FoodComponentWrapper;
import adris.altoclef.multiversion.item.ItemVer;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.helpers.LookHelper;
import adris.altoclef.util.progresscheck.MovementProgressChecker;
import baritone.api.pathing.goals.Goal;
import baritone.api.utils.Rotation;
import baritone.api.utils.input.Input;
import baritone.pathing.movement.MovementHelper;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public class EscapeFromLavaTask extends CustomBaritoneGoalTask {
   private final float strength;
   private int ticks = 0;
   private final Predicate<BlockPos> avoidPlacingRiskyBlock;

   public EscapeFromLavaTask(AltoClefController mod, float strength) {
      this.strength = strength;
      this.avoidPlacingRiskyBlock = blockPos -> mod.getPlayer().getBoundingBox().intersects(new Box(blockPos))
         && (mod.getWorld().getBlockState(mod.getPlayer().getBlockPos().down()).getBlock() == Blocks.LAVA || mod.getPlayer().isInLava());
   }

   public EscapeFromLavaTask(AltoClefController mod) {
      this(mod, 100.0F);
   }

   @Override
   protected void onStart() {
      AltoClefController mod = this.controller;
      mod.getBehaviour().push();
      mod.getBaritone().getExploreProcess().onLostControl();
      mod.getBaritone().getCustomGoalProcess().onLostControl();
      mod.getBehaviour().allowSwimThroughLava(true);
      mod.getBehaviour().setBlockPlacePenalty(0.0);
      mod.getBehaviour().setBlockBreakAdditionalPenalty(0.0);
      this.checker = new MovementProgressChecker(Integer.MAX_VALUE);
      mod.getExtraBaritoneSettings().avoidBlockPlace(this.avoidPlacingRiskyBlock);
   }

   @Override
   protected Task onTick() {
      AltoClefController mod = this.controller;
      mod.getInputControls().hold(Input.JUMP);
      mod.getInputControls().hold(Input.SPRINT);
      Optional<Item> food = this.calculateFood(mod);
      if (food.isPresent() && mod.getBaritone().getEntityContext().hungerManager().getFoodLevel() < 20) {
         if (mod.getPlayer().isBlocking()) {
            mod.log("want to eat, trying to stop shielding...");
            mod.getInputControls().release(Input.CLICK_RIGHT);
         } else {
            mod.getSlotHandler().forceEquipItem(new ItemTarget(food.get()), true);
            mod.getInputControls().hold(Input.CLICK_RIGHT);
         }
      }

      if (mod.getPlayer().isInLava() || mod.getWorld().getBlockState(mod.getPlayer().getBlockPos().down()).getBlock() == Blocks.LAVA) {
         this.setDebugState("run away from lava");
         BlockPos steppingPos = mod.getPlayer().getSteppingPos();
         if (!mod.getWorld().getBlockState(steppingPos.east()).getBlock().equals(Blocks.LAVA)
            || !mod.getWorld().getBlockState(steppingPos.west()).getBlock().equals(Blocks.LAVA)
            || !mod.getWorld().getBlockState(steppingPos.south()).getBlock().equals(Blocks.LAVA)
            || !mod.getWorld().getBlockState(steppingPos.north()).getBlock().equals(Blocks.LAVA)
            || !mod.getWorld().getBlockState(steppingPos.east().north()).getBlock().equals(Blocks.LAVA)
            || !mod.getWorld().getBlockState(steppingPos.east().south()).getBlock().equals(Blocks.LAVA)
            || !mod.getWorld().getBlockState(steppingPos.west().north()).getBlock().equals(Blocks.LAVA)
            || !mod.getWorld().getBlockState(steppingPos.west().south()).getBlock().equals(Blocks.LAVA)) {
            return super.onTick();
         }

         if (mod.getPlayer().isBlocking()) {
            mod.log("want to place block, trying to stop shielding...");
            mod.getInputControls().release(Input.CLICK_RIGHT);
         }

         for (float pitch = 25.0F; pitch < 90.0F; pitch++) {
            for (float yaw = -180.0F; yaw < 180.0F; yaw++) {
               HitResult result = this.raycast(mod, 4.0, pitch, yaw);
               if (result.getType() == Type.BLOCK) {
                  BlockHitResult blockHitResult = (BlockHitResult)result;
                  BlockPos pos = blockHitResult.getBlockPos();
                  if (pos.getY() <= mod.getPlayer().getSteppingPos().getY()) {
                     Direction facing = blockHitResult.getSide();
                     if (facing != Direction.UP) {
                        LookHelper.lookAt(this.controller, new Rotation(yaw, pitch));
                        if (mod.getItemStorage().hasItem(Items.NETHERRACK)) {
                           mod.getSlotHandler().forceEquipItem(Items.NETHERRACK);
                        } else {
                           mod.getSlotHandler().forceEquipItem(mod.getBaritoneSettings().acceptableThrowawayItems.get().toArray(new Item[0]));
                        }

                        mod.log(String.valueOf(pos));
                        mod.log(String.valueOf(facing));
                        mod.getInputControls().tryPress(Input.CLICK_RIGHT);
                        return null;
                     }
                  }
               }
            }
         }
      }

      return super.onTick();
   }

   private Optional<Item> calculateFood(AltoClefController mod) {
      Item bestFood = null;
      double bestFoodScore = Double.NEGATIVE_INFINITY;
      LivingEntity player = mod.getPlayer();
      float hunger = player != null ? mod.getBaritone().getEntityContext().hungerManager().getFoodLevel() : 20.0F;
      float saturation = player != null ? mod.getBaritone().getEntityContext().hungerManager().getSaturationLevel() : 20.0F;

      for (ItemStack stack : mod.getItemStorage().getItemStacksPlayerInventory(true)) {
         if (ItemVer.isFood(stack) && stack.getItem() != Items.SPIDER_EYE) {
            float score = getScore(stack, hunger, saturation);
            if (score > bestFoodScore) {
               bestFoodScore = score;
               bestFood = stack.getItem();
            }
         }
      }

      return Optional.ofNullable(bestFood);
   }

   private static float getScore(ItemStack stack, float hunger, float saturation) {
      FoodComponentWrapper food = ItemVer.getFoodComponent(stack.getItem());

      assert food != null;

      float hungerIfEaten = Math.min(hunger + food.getHunger(), 20.0F);
      float saturationIfEaten = Math.min(hungerIfEaten, saturation + food.getSaturationModifier());
      float gainedSaturation = saturationIfEaten - saturation;
      float hungerNotFilled = 20.0F - hungerIfEaten;
      float saturationGoodScore = gainedSaturation * 10.0F;
      float hungerNotFilledPenalty = hungerNotFilled * 2.0F;
      float score = saturationGoodScore - hungerNotFilledPenalty;
      if (stack.getItem() == Items.ROTTEN_FLESH) {
         score = 0.0F;
      }

      return score;
   }

   public HitResult raycast(AltoClefController mod, double maxDistance, float pitch, float yaw) {
      Vec3d cameraPos = mod.getPlayer().getCameraPosVec(0.0F);
      Vec3d rotationVector = this.getRotationVector(pitch, yaw);
      Vec3d vec3d3 = cameraPos.add(rotationVector.x * maxDistance, rotationVector.y * maxDistance, rotationVector.z * maxDistance);
      return mod.getPlayer().method_48926().raycast(new RaycastContext(cameraPos, vec3d3, ShapeType.OUTLINE, FluidHandling.NONE, mod.getPlayer()));
   }

   protected final Vec3d getRotationVector(float pitch, float yaw) {
      float f = pitch * (float) (Math.PI / 180.0);
      float g = -yaw * (float) (Math.PI / 180.0);
      float h = MathHelper.cos(g);
      float i = MathHelper.sin(g);
      float j = MathHelper.cos(f);
      float k = MathHelper.sin(f);
      return new Vec3d(i * j, -k, h * j);
   }

   @Override
   protected void onStop(Task interruptTask) {
      AltoClefController mod = this.controller;
      mod.getBehaviour().pop();
      mod.getInputControls().release(Input.JUMP);
      mod.getInputControls().release(Input.SPRINT);
      mod.getInputControls().release(Input.CLICK_RIGHT);
      synchronized (mod.getExtraBaritoneSettings().getPlaceMutex()) {
         mod.getExtraBaritoneSettings().getPlaceAvoiders().remove(this.avoidPlacingRiskyBlock);
      }
   }

   @Override
   protected Goal newGoal(AltoClefController mod) {
      return new EscapeFromLavaGoal();
   }

   @Override
   protected boolean isEqual(Task other) {
      return other instanceof EscapeFromLavaTask;
   }

   @Override
   public boolean isFinished() {
      LivingEntity player = this.controller.getPlayer();
      return !player.isInLava() && !player.isOnFire();
   }

   @Override
   protected String toDebugString() {
      return "Escaping lava";
   }

   private class EscapeFromLavaGoal implements Goal {
      private boolean isLava(int x, int y, int z) {
         return EscapeFromLavaTask.this.controller.getWorld() == null
            ? false
            : MovementHelper.isLava(EscapeFromLavaTask.this.controller.getWorld().getBlockState(new BlockPos(x, y, z)));
      }

      private boolean isLavaAdjacent(int x, int y, int z) {
         return this.isLava(x + 1, y, z)
            || this.isLava(x - 1, y, z)
            || this.isLava(x, y, z + 1)
            || this.isLava(x, y, z - 1)
            || this.isLava(x + 1, y, z - 1)
            || this.isLava(x + 1, y, z + 1)
            || this.isLava(x - 1, y, z - 1)
            || this.isLava(x - 1, y, z + 1);
      }

      private boolean isWater(int x, int y, int z) {
         return EscapeFromLavaTask.this.controller.getWorld() == null
            ? false
            : MovementHelper.isWater(EscapeFromLavaTask.this.controller.getWorld().getBlockState(new BlockPos(x, y, z)));
      }

      @Override
      public boolean isInGoal(int x, int y, int z) {
         return !this.isLava(x, y, z) && !this.isLavaAdjacent(x, y, z);
      }

      @Override
      public double heuristic(int x, int y, int z) {
         if (this.isLava(x, y, z)) {
            return EscapeFromLavaTask.this.strength;
         } else if (this.isLavaAdjacent(x, y, z)) {
            return EscapeFromLavaTask.this.strength * 0.5F;
         } else {
            return this.isWater(x, y, z) ? -100.0 : 0.0;
         }
      }
   }
}
