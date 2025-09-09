package adris.altoclef.util.baritone;

import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalBlock;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;

public class GoalFollowEntity implements Goal {
   private final Entity entity;
   private final double closeEnoughDistance;

   public GoalFollowEntity(Entity entity, double closeEnoughDistance) {
      this.entity = entity;
      this.closeEnoughDistance = closeEnoughDistance;
   }

   @Override
   public boolean isInGoal(int x, int y, int z) {
      BlockPos p = new BlockPos(x, y, z);
      return this.entity.getBlockPos().equals(p) || p.isWithinDistance(this.entity.getPos(), this.closeEnoughDistance);
   }

   @Override
   public double heuristic(int x, int y, int z) {
      double xDiff = x - this.entity.getPos().getX();
      int yDiff = y - this.entity.getBlockPos().getY();
      double zDiff = z - this.entity.getPos().getZ();
      return GoalBlock.calculate(xDiff, yDiff, zDiff);
   }
}
