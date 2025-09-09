package adris.altoclef.tasks.movement;

import adris.altoclef.AltoClefController;
import adris.altoclef.Debug;
import adris.altoclef.control.InputControls;
import adris.altoclef.multiversion.DamageSourceVer;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.helpers.*;
import adris.altoclef.util.serialization.ItemDeserializer;
import adris.altoclef.util.serialization.ItemSerializer;
import baritone.api.IBaritone;
import baritone.api.utils.IEntityContext;
import baritone.api.utils.Rotation;
import baritone.api.utils.RotationUtils;
import baritone.api.utils.input.Input;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.World;

public class MLGBucketTask extends Task {
   private static MLGClutchConfig config;
   private BlockPos placedPos;
   private BlockPos movingTorwards;

   private static boolean isLava(AltoClefController controller, BlockPos pos) {
      assert controller.getWorld() != null;

      return controller.getWorld().getBlockState(pos).getBlock() == Blocks.LAVA;
   }

   private static boolean lavaWillProtect(AltoClefController controller, BlockPos pos) {
      assert controller.getWorld() != null;

      BlockState state = controller.getWorld().getBlockState(pos);
      if (state.getBlock() != Blocks.LAVA) {
         return false;
      } else {
         int level = state.getFluidState().getLevel();
         return level == 0 || level >= config.lavaLevelOrGreaterWillCancelFallDamage;
      }
   }

   private static boolean isWater(AltoClefController controller, BlockPos pos) {
      assert controller.getWorld() != null;

      return controller.getWorld().getBlockState(pos).getBlock() == Blocks.WATER;
   }

   private static boolean canTravelToInAir(AltoClefController controller, BlockPos pos) {
      LivingEntity clientPlayerEntity = controller.getPlayer();

      assert clientPlayerEntity != null;

      double verticalDist = clientPlayerEntity.getPos().getY() - pos.getY() - 1.0;
      double verticalVelocity = -1.0 * clientPlayerEntity.getVelocity().y;
      double grav = 0.08;
      double movementSpeedPerTick = config.averageHorizontalMovementSpeedPerTick;
      double ticksToTravelSq = (-verticalVelocity + Math.sqrt(verticalVelocity * verticalVelocity + 2.0 * grav * verticalDist)) / grav;
      double maxMoveDistanceSq = movementSpeedPerTick * movementSpeedPerTick * ticksToTravelSq * ticksToTravelSq;
      double horizontalDistance = WorldHelper.distanceXZ(clientPlayerEntity.getPos(), WorldHelper.toVec3d(pos)) - 0.8;
      if (horizontalDistance < 0.0) {
         horizontalDistance = 0.0;
      }

      return maxMoveDistanceSq > horizontalDistance * horizontalDistance;
   }

   private static boolean isFallDeadly(AltoClefController controller, BlockPos pos) {
      LivingEntity clientPlayerEntity = controller.getPlayer();
      double damage = calculateFallDamageToLandOn(controller, pos);

      assert controller.getWorld() != null;

      Block b = controller.getWorld().getBlockState(pos).getBlock();
      if (b == Blocks.HAY_BLOCK) {
         damage *= 0.2F;
      }

      assert clientPlayerEntity != null;

      double resultingHealth = clientPlayerEntity.getHealth() - (float)damage;
      return resultingHealth < config.preferLavaWhenFallDropsHealthBelowThreshold;
   }

   private static double calculateFallDamageToLandOn(AltoClefController controller, BlockPos pos) {
      World world = controller.getWorld();
      LivingEntity clientPlayerEntity = controller.getPlayer();

      assert clientPlayerEntity != null;

      double totalFallDistance = clientPlayerEntity.fallDistance + clientPlayerEntity.getY() - pos.getY() - 1.0;
      double baseFallDamage = MathHelper.ceil(totalFallDistance - 3.0);

      assert world != null;

      return EntityHelper.calculateResultingPlayerDamage(clientPlayerEntity, DamageSourceVer.getFallDamageSource(world), baseFallDamage);
   }

   private static void moveLeftRight(AltoClefController controller, int delta) {
      InputControls controls = controller.getInputControls();
      if (delta == 0) {
         controls.release(Input.MOVE_LEFT);
         controls.release(Input.MOVE_RIGHT);
      } else if (delta > 0) {
         controls.release(Input.MOVE_LEFT);
         controls.hold(Input.MOVE_RIGHT);
      } else {
         controls.hold(Input.MOVE_LEFT);
         controls.release(Input.MOVE_RIGHT);
      }
   }

   private static void moveForwardBack(AltoClefController controller, int delta) {
      InputControls controls = controller.getInputControls();
      if (delta == 0) {
         controls.release(Input.MOVE_FORWARD);
         controls.release(Input.MOVE_BACK);
      } else if (delta > 0) {
         controls.hold(Input.MOVE_FORWARD);
         controls.release(Input.MOVE_BACK);
      } else {
         controls.release(Input.MOVE_FORWARD);
         controls.hold(Input.MOVE_BACK);
      }
   }

   private Task onTickInternal(AltoClefController mod, BlockPos oldMovingTorwards) {
      Optional<BlockPos> willLandOn = this.getBlockWeWillLandOn(mod);
      Optional<BlockPos> bestClutchPos = this.getBestConeClutchBlock(mod, oldMovingTorwards);
      if (bestClutchPos.isPresent()) {
         this.movingTorwards = bestClutchPos.get().mutableCopy();
         if (!this.movingTorwards.equals(oldMovingTorwards)) {
            if (oldMovingTorwards == null) {
               Debug.logMessage("(NEW clutch target: " + this.movingTorwards + ")");
            } else {
               Debug.logMessage("(changed clutch target: " + this.movingTorwards + ")");
            }
         }
      } else if (oldMovingTorwards != null) {
         Debug.logMessage("(LOST clutch position!)");
      }

      if (willLandOn.isPresent()) {
         this.handleJumpForLand(mod, willLandOn.get());
         return this.placeMLGBucketTask(mod, willLandOn.get());
      } else {
         this.setDebugState("Wait for it...");
         mod.getInputControls().release(Input.JUMP);
         return null;
      }
   }

   private Task placeMLGBucketTask(AltoClefController mod, BlockPos toPlaceOn) {
      if (!this.hasClutchItem(mod)) {
         this.setDebugState("No clutch item");
         return null;
      } else {
         if (!WorldHelper.isSolidBlock(this.controller, toPlaceOn)) {
            toPlaceOn = toPlaceOn.down();
         }

         BlockPos willLandIn = toPlaceOn.up();
         BlockState willLandInState = mod.getWorld().getBlockState(willLandIn);
         if (willLandInState.getBlock() == Blocks.WATER) {
            this.setDebugState("Waiting to fall into water");
            mod.getBaritone().getInputOverrideHandler().setInputForceState(Input.CLICK_RIGHT, false);
            return null;
         } else {
            IEntityContext ctx = mod.getBaritone().getEntityContext();
            Optional<Rotation> reachable = RotationUtils.reachableCenter(ctx.entity(), toPlaceOn, ctx.playerController().getBlockReachDistance(), false);
            if (reachable.isPresent()) {
               this.setDebugState("Performing MLG");
               LookHelper.lookAt(this.controller, reachable.get());
               boolean hasClutch = !mod.getWorld().getDimension().ultrawarm() && mod.getSlotHandler().forceEquipItem(Items.WATER_BUCKET);
               if (!hasClutch && !config.clutchItems.isEmpty()) {
                  for (Item tryEquip : config.clutchItems) {
                     if (mod.getSlotHandler().forceEquipItem(tryEquip)) {
                        hasClutch = true;
                        break;
                     }
                  }
               }

               BlockPos[] toCheckLook = new BlockPos[]{toPlaceOn, toPlaceOn.up(), toPlaceOn.up(2)};
               if (hasClutch && Arrays.stream(toCheckLook).anyMatch(check -> mod.getBaritone().getEntityContext().isLookingAt(check))) {
                  Debug.logMessage("HIT: " + willLandIn);
                  this.placedPos = willLandIn;
                  mod.getInputControls().tryPress(Input.CLICK_RIGHT);
               } else {
                  this.setDebugState("NOT LOOKING CORRECTLY!");
               }
            } else {
               this.setDebugState("Waiting to reach target block...");
            }

            return null;
         }
      }
   }

   @Override
   protected Task onTick() {
      AltoClefController mod = this.controller;
      mod.getInputControls().hold(Input.SPRINT);
      Mutable mutable = this.movingTorwards != null ? this.movingTorwards.mutableCopy() : null;
      this.movingTorwards = null;
      Task result = this.onTickInternal(mod, mutable);
      this.handleForwardVelocity(mod, !Objects.equals(mutable, this.movingTorwards));
      this.handleCancellingSidewaysVelocity(mod);
      return result;
   }

   private void handleForwardVelocity(AltoClefController mod, boolean newForwardTarget) {
      if (!mod.getPlayer().isOnGround() && this.movingTorwards != null && !WorldHelper.inRangeXZ(mod.getPlayer(), this.movingTorwards, 0.05F)) {
         Rotation look = LookHelper.getLookRotation(this.controller);
         look = new Rotation(look.getYaw(), 0.0F);
         Vec3d forwardFacing = LookHelper.toVec3d(look).multiply(1.0, 0.0, 1.0).normalize();
         Vec3d delta = WorldHelper.toVec3d(this.movingTorwards).subtract(mod.getPlayer().getPos()).multiply(1.0, 0.0, 1.0);
         Vec3d velocity = mod.getPlayer().getVelocity().multiply(1.0, 0.0, 1.0);
         Vec3d pd = delta.subtract(velocity.multiply(3.0));
         double forwardStrength = pd.dotProduct(forwardFacing);
         if (newForwardTarget) {
            LookHelper.lookAt(mod, this.movingTorwards);
         }

         Debug.logInternal("F:" + forwardStrength);
         moveForwardBack(mod, (int)Math.signum(forwardStrength));
      } else {
         moveForwardBack(mod, 0);
      }
   }

   @Override
   protected void onStart() {
      this.controller.getBaritone().getPathingBehavior().forceCancel();
      this.placedPos = null;
      this.controller.getPlayer().setPitch(90.0F);
   }

   private void handleJumpForLand(AltoClefController mod, BlockPos willLandOn) {
      BlockPos willLandIn = WorldHelper.isSolidBlock(this.controller, willLandOn) ? willLandOn.up() : willLandOn;
      BlockState s = mod.getWorld().getBlockState(willLandIn);
      if (s.getBlock() == Blocks.LAVA) {
         mod.getInputControls().hold(Input.JUMP);
      } else {
         Box blockBounds;
         try {
            blockBounds = s.getCollisionShape(mod.getWorld(), willLandIn).getBoundingBox();
         } catch (UnsupportedOperationException var7) {
            blockBounds = Box.of(WorldHelper.toVec3d(willLandIn), 1.0, 1.0, 1.0);
         }

         boolean inside = mod.getPlayer().getBoundingBox().intersects(blockBounds);
         if (inside) {
            mod.getInputControls().hold(Input.JUMP);
         } else {
            mod.getInputControls().release(Input.JUMP);
         }
      }
   }

   private Optional<BlockPos> getBlockWeWillLandOn(AltoClefController mod) {
      Vec3d velCheck = mod.getPlayer().getVelocity();
      velCheck.multiply(10.0, 0.0, 10.0);
      Box b = mod.getPlayer().getBoundingBox().offset(velCheck);
      Vec3d c = b.getCenter();
      Vec3d[] coords = new Vec3d[]{c, new Vec3d(b.minX, c.y, b.minZ), new Vec3d(b.maxX, c.y, b.minZ), new Vec3d(b.minX, c.y, b.maxZ), new Vec3d(b.maxX, c.y, b.maxZ)};
      BlockHitResult result = null;
      double bestSqDist = Double.POSITIVE_INFINITY;

      for (Vec3d rayOrigin : coords) {
         RaycastContext rctx = this.castDown(rayOrigin);
         BlockHitResult hit = mod.getWorld().raycast(rctx);
         if (hit.getType() == Type.BLOCK) {
            double curDis = hit.getPos().squaredDistanceTo(rayOrigin);
            if (curDis < bestSqDist) {
               result = hit;
               bestSqDist = curDis;
            }
         }
      }

      return result != null && result.getType() == Type.BLOCK ? Optional.ofNullable(result.getBlockPos()) : Optional.empty();
   }

   private void handleCancellingSidewaysVelocity(AltoClefController mod) {
      if (this.movingTorwards == null) {
         moveLeftRight(mod, 0);
      } else {
         Vec3d velocity = mod.getPlayer().getVelocity();
         Vec3d deltaTarget = WorldHelper.toVec3d(this.movingTorwards).subtract(mod.getPlayer().getPos());
         Rotation look = LookHelper.getLookRotation(this.controller);
         Vec3d forwardFacing = LookHelper.toVec3d(look).multiply(1.0, 0.0, 1.0).normalize();
         Vec3d rightVelocity = MathsHelper.projectOntoPlane(velocity, forwardFacing).multiply(1.0, 0.0, 1.0);
         Vec3d rightDelta = MathsHelper.projectOntoPlane(deltaTarget, forwardFacing).multiply(1.0, 0.0, 1.0);
         Vec3d pd = rightDelta.subtract(rightVelocity.multiply(2.0));
         Vec3d faceRight = forwardFacing.crossProduct(new Vec3d(0.0, 1.0, 0.0));
         boolean moveRight = pd.dotProduct(faceRight) > 0.0;
         if (moveRight) {
            moveLeftRight(mod, 1);
         } else {
            moveLeftRight(mod, -1);
         }
      }
   }

   private Optional<BlockPos> getBestConeClutchBlock(AltoClefController mod, BlockPos oldClutchTarget) {
      double pitchHalfWidth = config.epicClutchConePitchAngle;
      double dpitchStart = pitchHalfWidth / config.epicClutchConePitchResolution;
      ConeClutchContext cctx = new ConeClutchContext(mod);
      if (oldClutchTarget != null) {
         cctx.checkBlock(mod, oldClutchTarget);
      }

      for (double pitch = dpitchStart; pitch <= pitchHalfWidth; pitch += pitchHalfWidth / config.epicClutchConePitchResolution) {
         double pitchProgress = (pitch - dpitchStart) / (pitchHalfWidth - dpitchStart);
         double yawResolution = config.epicClutchConeYawDivisionStart
            + pitchProgress * (config.epicClutchConeYawDivisionEnd - config.epicClutchConeYawDivisionStart);

         for (double yaw = 0.0; yaw < 360.0; yaw += 360.0 / yawResolution) {
            RaycastContext rctx = this.castCone(yaw, pitch);
            cctx.checkRay(mod, rctx);
         }
      }

      Vec3d center = mod.getPlayer().getPos();

      for (int dx = -2; dx <= 2; dx++) {
         for (int dz = -2; dz <= 2; dz++) {
            RaycastContext ctx = this.castDown(center.add(dx, 0.0, dz));
            cctx.checkRay(mod, ctx);
         }
      }

      return Optional.ofNullable(cctx.bestBlock);
   }

   private RaycastContext castDown(Vec3d origin) {
      LivingEntity clientPlayerEntity = this.controller.getPlayer();

      assert clientPlayerEntity != null;

      return new RaycastContext(
         origin, origin.add(0.0, -1.0 * config.castDownDistance, 0.0), net.minecraft.world.RaycastContext.ShapeType.COLLIDER, FluidHandling.ANY, clientPlayerEntity
      );
   }

   private RaycastContext castCone(double yaw, double pitch) {
      LivingEntity clientPlayerEntity = this.controller.getPlayer();

      assert clientPlayerEntity != null;

      Vec3d origin = clientPlayerEntity.getPos();
      double dy = config.epicClutchConeCastHeight;
      double dH = dy * Math.sin(Math.toRadians(pitch));
      double yawRad = Math.toRadians(yaw);
      double dx = dH * Math.cos(yawRad);
      double dz = dH * Math.sin(yawRad);
      Vec3d end = origin.add(dx, -1.0 * dy, dz);
      return new RaycastContext(origin, end, net.minecraft.world.RaycastContext.ShapeType.COLLIDER, FluidHandling.ANY, clientPlayerEntity);
   }

   @Override
   protected void onStop(Task interruptTask) {
      IBaritone baritone = this.controller.getBaritone();
      InputControls controls = this.controller.getInputControls();
      baritone.getPathingBehavior().forceCancel();
      this.movingTorwards = null;
      baritone.getInputOverrideHandler().setInputForceState(Input.CLICK_RIGHT, false);
      moveLeftRight(this.controller, 0);
      moveForwardBack(this.controller, 0);
      controls.release(Input.SPRINT);
      controls.release(Input.JUMP);
   }

   private boolean hasClutchItem(AltoClefController mod) {
      return !mod.getWorld().getDimension().ultrawarm() && mod.getItemStorage().hasItem(Items.WATER_BUCKET)
         ? true
         : config.clutchItems.stream().anyMatch(item -> mod.getItemStorage().hasItem(item));
   }

   @Override
   public boolean isFinished() {
      LivingEntity player = this.controller.getPlayer();
      return player.isSwimming() || player.isTouchingWater() || player.isOnGround() || player.isClimbing();
   }

   @Override
   protected boolean isEqual(Task other) {
      return other instanceof MLGBucketTask;
   }

   @Override
   protected String toDebugString() {
      String result = "Epic gaemer moment";
      if (this.movingTorwards != null) {
         result = result + " (CLUTCH AT: " + result + ")";
      }

      return result;
   }

   public BlockPos getWaterPlacedPos() {
      return this.placedPos;
   }

   static {
      ConfigHelper.loadConfig(
         "configs/mlg_clutch_settings.json", MLGClutchConfig::new, MLGClutchConfig.class, newConfig -> config = newConfig
      );
   }

   class ConeClutchContext {
      private final boolean hasClutchItem;
      public BlockPos bestBlock = null;
      private double highestY = Double.NEGATIVE_INFINITY;
      private double closestXZ = Double.POSITIVE_INFINITY;
      private boolean bestBlockIsSafe = false;
      private boolean bestBlockIsDeadlyFall = false;
      private boolean bestBlockIsLava = false;

      public ConeClutchContext(AltoClefController mod) {
         this.hasClutchItem = MLGBucketTask.this.hasClutchItem(mod);
      }

      public void checkBlock(AltoClefController mod, BlockPos check) {
         if (!Objects.equals(this.bestBlock, check)) {
            if (WorldHelper.isAir(mod.getWorld().getBlockState(check).getBlock())) {
               Debug.logMessage("(MLG Air block checked for landing, the block broke. We'll try another): " + check);
            } else {
               boolean lava = MLGBucketTask.isLava(MLGBucketTask.this.controller, check);
               boolean lavaWillProtect = lava && MLGBucketTask.lavaWillProtect(MLGBucketTask.this.controller, check);
               boolean water = MLGBucketTask.isWater(MLGBucketTask.this.controller, check);
               boolean isDeadlyFall = !this.hasClutchItem && MLGBucketTask.isFallDeadly(MLGBucketTask.this.controller, check);
               if (!this.bestBlockIsSafe || water) {
                  double height = check.getY();
                  double distSqXZ = WorldHelper.distanceXZSquared(WorldHelper.toVec3d(check), mod.getPlayer().getPos());
                  boolean highestSoFar = height > this.highestY;
                  boolean closestSoFar = distSqXZ < this.closestXZ;
                  if ((
                        this.bestBlock == null
                           || water && !this.bestBlockIsSafe
                           || lava && lavaWillProtect && this.bestBlockIsDeadlyFall && !this.hasClutchItem
                           || !lava && !isDeadlyFall && (closestSoFar && this.hasClutchItem && highestSoFar || this.bestBlockIsLava)
                     )
                     && MLGBucketTask.canTravelToInAir(MLGBucketTask.this.controller, !lava && !water ? check : check.down())) {
                     if (highestSoFar) {
                        this.highestY = height;
                     }

                     if (closestSoFar) {
                        this.closestXZ = distSqXZ;
                     }

                     this.bestBlockIsSafe = water;
                     this.bestBlockIsDeadlyFall = isDeadlyFall;
                     this.bestBlockIsLava = lava;
                     this.bestBlock = check;
                  }
               }
            }
         }
      }

      public void checkRay(AltoClefController mod, RaycastContext rctx) {
         BlockHitResult hit = mod.getWorld().raycast(rctx);
         if (hit.getType() == Type.BLOCK) {
            BlockPos check = hit.getBlockPos();
            if (hit.getSide().getOffsetY() <= 0) {
               return;
            }

            this.checkBlock(mod, check);
         }
      }
   }

   private static class MLGClutchConfig {
      public double castDownDistance = 40.0;
      public double averageHorizontalMovementSpeedPerTick = 0.25;
      public double epicClutchConeCastHeight = 40.0;
      public double epicClutchConePitchAngle = 25.0;
      public int epicClutchConePitchResolution = 8;
      public int epicClutchConeYawDivisionStart = 6;
      public int epicClutchConeYawDivisionEnd = 20;
      public int preferLavaWhenFallDropsHealthBelowThreshold = 3;
      public int lavaLevelOrGreaterWillCancelFallDamage = 5;
      @JsonSerialize(
         using = ItemSerializer.class
      )
      @JsonDeserialize(
         using = ItemDeserializer.class
      )
      public List<Item> clutchItems = List.of(Items.HAY_BLOCK, Items.TWISTING_VINES);
   }
}
