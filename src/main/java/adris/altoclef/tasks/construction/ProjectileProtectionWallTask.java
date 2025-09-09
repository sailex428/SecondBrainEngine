package adris.altoclef.tasks.construction;

import adris.altoclef.AltoClefController;
import adris.altoclef.tasksystem.ITaskRequiresGrounded;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.helpers.EntityHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.helpers.WorldHelper;
import adris.altoclef.util.slots.Slot;
import adris.altoclef.util.time.TimerGame;
import net.minecraft.block.AbstractPressurePlateBlock;
import net.minecraft.block.AnvilBlock;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.Blocks;
import net.minecraft.block.ButtonBlock;
import net.minecraft.block.CraftingTableBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.NoteBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Optional;
import java.util.function.Predicate;

public class ProjectileProtectionWallTask extends Task implements ITaskRequiresGrounded {
   private final AltoClefController mod;
   private final TimerGame waitForBlockPlacement = new TimerGame(2.0);
   private BlockPos targetPlacePos;

   public ProjectileProtectionWallTask(AltoClefController mod) {
      this.mod = mod;
   }

   @Override
   protected void onStart() {
      this.waitForBlockPlacement.forceElapse();
   }

   @Override
   protected Task onTick() {
      if (this.targetPlacePos != null && !WorldHelper.isSolidBlock(this.controller, this.targetPlacePos)) {
         Optional<Slot> slot = StorageHelper.getSlotWithThrowawayBlock(this.mod, true);
         if (slot.isPresent()) {
            this.place(this.targetPlacePos, Hand.MAIN_HAND, slot.get().getInventorySlot());
            this.targetPlacePos = null;
            this.setDebugState(null);
         }

         return null;
      } else {
         Optional<Entity> sentity = this.mod
            .getEntityTracker()
            .getClosestEntity(
               (Predicate<Entity>)(e -> e instanceof SkeletonEntity && EntityHelper.isAngryAtPlayer(this.mod, e) && ((SkeletonEntity)e).getItemUseTime() > 8),
               SkeletonEntity.class
            );
         if (sentity.isPresent()) {
            Vec3d playerPos = this.mod.getPlayer().getPos();
            Vec3d targetPos = sentity.get().getPos();
            Vec3d direction = playerPos.subtract(targetPos).normalize();
            double x = playerPos.x - 2.0 * direction.x;
            double y = playerPos.y + direction.y;
            double z = playerPos.z - 2.0 * direction.z;
            this.targetPlacePos = new BlockPos((int)x, (int)y + 1, (int)z);
            this.setDebugState("Placing at " + this.targetPlacePos.toString());
            this.waitForBlockPlacement.reset();
         }

         return null;
      }
   }

   @Override
   protected void onStop(Task interruptTask) {
   }

   @Override
   public boolean isFinished() {
      assert this.controller.getWorld() != null;

      Optional<Entity> entity = this.mod
         .getEntityTracker()
         .getClosestEntity(
            (Predicate<Entity>)(e -> e instanceof SkeletonEntity && EntityHelper.isAngryAtPlayer(this.mod, e) && ((SkeletonEntity)e).getItemUseTime() > 3),
            SkeletonEntity.class
         );
      return this.targetPlacePos != null && WorldHelper.isSolidBlock(this.mod, this.targetPlacePos) || entity.isEmpty();
   }

   @Override
   protected boolean isEqual(Task other) {
      return true;
   }

   @Override
   protected String toDebugString() {
      return "Placing blocks to block projectiles";
   }

   public Direction getPlaceSide(BlockPos blockPos) {
      for (Direction side : Direction.values()) {
         BlockPos neighbor = blockPos.offset(side);
         BlockState state = this.mod.getWorld().getBlockState(neighbor);
         if (!state.isAir() && !isClickable(state.getBlock()) && state.getFluidState().isEmpty()) {
            return side;
         }
      }

      return null;
   }

   public boolean place(BlockPos blockPos, Hand hand, int slot) {
      if (slot < 0 || slot > 8) {
         return false;
      } else if (!this.canPlace(blockPos)) {
         return false;
      } else {
         Vec3d hitPos = Vec3d.ofCenter(blockPos);
         Direction side = this.getPlaceSide(blockPos);
         if (side == null) {
            this.place(blockPos.down(), hand, slot);
            return false;
         } else {
            BlockPos neighbour = blockPos.offset(side);
            hitPos = hitPos.add(side.getOffsetX() * 0.5, side.getOffsetY() * 0.5, side.getOffsetZ() * 0.5);
            BlockHitResult bhr = new BlockHitResult(hitPos, side.getOpposite(), neighbour, false);
            this.mod.getPlayer().setYaw((float)this.getYaw(hitPos));
            this.mod.getPlayer().setPitch((float)this.getPitch(hitPos));
            this.swap(slot);
            this.interact(bhr, hand);
            return true;
         }
      }
   }

   public static boolean isClickable(Block block) {
      return block instanceof CraftingTableBlock
         || block instanceof AnvilBlock
         || block instanceof ButtonBlock
         || block instanceof AbstractPressurePlateBlock
         || block instanceof BlockWithEntity
         || block instanceof BedBlock
         || block instanceof FenceGateBlock
         || block instanceof DoorBlock
         || block instanceof NoteBlock
         || block instanceof TrapdoorBlock;
   }

   public void interact(BlockHitResult blockHitResult, Hand hand) {
      boolean wasSneaking = this.mod.getPlayer().isSneaking();
      this.mod.getPlayer().setSneaking(false);
      ActionResult result = this.mod
         .getBaritone()
         .getPlayerContext()
         .interactionController()
         .processRightClickBlock(this.mod.getPlayer(), this.mod.getWorld(), hand, blockHitResult);
      if (result.shouldSwingHand()) {
         this.mod.getPlayer().swingHand(hand);
      }

      this.mod.getPlayer().setSneaking(wasSneaking);
   }

   public boolean canPlace(BlockPos blockPos, boolean checkEntities) {
      if (blockPos == null) {
         return false;
      } else if (!World.isValid(blockPos) || !this.controller.getWorld().isInBuildLimit(blockPos)) {
         return false;
      } else {
         return !this.mod.getWorld().getBlockState(blockPos).isReplaceable()
            ? false
            : !checkEntities || this.mod.getWorld().canPlace(Blocks.OBSIDIAN.getDefaultState(), blockPos, ShapeContext.absent());
      }
   }

   public boolean canPlace(BlockPos blockPos) {
      return this.canPlace(blockPos, true);
   }

   public boolean swap(int slot) {
      if (slot == this.mod.getBaritone().getPlayerContext().inventory().selectedSlot) {
         return true;
      } else if (slot >= 0 && slot <= 8) {
         this.mod.getBaritone().getPlayerContext().inventory().selectedSlot = slot;
         return true;
      } else {
         return false;
      }
   }

   public double getYaw(Vec3d pos) {
      return this.mod.getPlayer().getYaw()
         + MathHelper.wrapDegrees(
            (float)Math.toDegrees(Math.atan2(pos.getZ() - this.mod.getPlayer().getZ(), pos.getX() - this.mod.getPlayer().getX()))
               - 90.0F
               - this.mod.getPlayer().getYaw()
         );
   }

   public double getPitch(Vec3d pos) {
      double diffX = pos.getX() - this.mod.getPlayer().getX();
      double diffY = pos.getY() - this.mod.getPlayer().getY() + this.mod.getPlayer().getEyeHeight(this.mod.getPlayer().getPose());
      double diffZ = pos.getZ() - this.mod.getPlayer().getZ();
      double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);
      return this.mod.getPlayer().getPitch() + MathHelper.wrapDegrees((float)(-Math.toDegrees(Math.atan2(diffY, diffXZ))) - this.mod.getPlayer().getPitch());
   }
}
