package adris.altoclef.tasks;

import adris.altoclef.AltoClefController;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.helpers.WorldHelper;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class DoToClosestBlockTask extends AbstractDoToClosestObjectTask<BlockPos> {
   private final Block[] targetBlocks;
   private final Supplier<Vec3d> getOriginPos;
   private final Function<Vec3d, Optional<BlockPos>> getClosest;
   private final Function<BlockPos, Task> getTargetTask;
   private final Predicate<BlockPos> isValid;

   public DoToClosestBlockTask(
      Supplier<Vec3d> getOriginSupplier,
      Function<BlockPos, Task> getTargetTask,
      Function<Vec3d, Optional<BlockPos>> getClosestBlock,
      Predicate<BlockPos> isValid,
      Block... blocks
   ) {
      this.getOriginPos = getOriginSupplier;
      this.getTargetTask = getTargetTask;
      this.getClosest = getClosestBlock;
      this.isValid = isValid;
      this.targetBlocks = blocks;
   }

   public DoToClosestBlockTask(
      Function<BlockPos, Task> getTargetTask, Function<Vec3d, Optional<BlockPos>> getClosestBlock, Predicate<BlockPos> isValid, Block... blocks
   ) {
      this((Supplier<Vec3d>)null, getTargetTask, getClosestBlock, isValid, blocks);
   }

   public DoToClosestBlockTask(Function<BlockPos, Task> getTargetTask, Predicate<BlockPos> isValid, Block... blocks) {
      this((Supplier<Vec3d>)null, getTargetTask, (Function<Vec3d, Optional<BlockPos>>)null, isValid, blocks);
   }

   public DoToClosestBlockTask(Function<BlockPos, Task> getTargetTask, Block... blocks) {
      this(getTargetTask, (Function<Vec3d, Optional<BlockPos>>)null, blockPos -> true, blocks);
   }

   protected Vec3d getPos(AltoClefController mod, BlockPos obj) {
      return WorldHelper.toVec3d(obj);
   }

   @Override
   protected Optional<BlockPos> getClosestTo(AltoClefController mod, Vec3d pos) {
      return this.getClosest != null ? this.getClosest.apply(pos) : mod.getBlockScanner().getNearestBlock(pos, this.isValid, this.targetBlocks);
   }

   @Override
   protected Vec3d getOriginPos(AltoClefController mod) {
      return this.getOriginPos != null ? this.getOriginPos.get() : mod.getPlayer().getPos();
   }

   protected Task getGoalTask(BlockPos obj) {
      return this.getTargetTask.apply(obj);
   }

   protected boolean isValid(AltoClefController mod, BlockPos obj) {
      if (!mod.getChunkTracker().isChunkLoaded(obj)) {
         return true;
      } else {
         return this.isValid != null && !this.isValid.test(obj) ? false : mod.getBlockScanner().isBlockAtPosition(obj, this.targetBlocks);
      }
   }

   @Override
   protected void onStart() {
   }

   @Override
   protected void onStop(Task interruptTask) {
   }

   @Override
   protected boolean isEqual(Task other) {
      return other instanceof DoToClosestBlockTask task ? Arrays.equals((Object[])task.targetBlocks, (Object[])this.targetBlocks) : false;
   }

   @Override
   protected String toDebugString() {
      return "Doing something to closest block...";
   }
}
