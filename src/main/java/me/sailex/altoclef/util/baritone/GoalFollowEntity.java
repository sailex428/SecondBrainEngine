package me.sailex.altoclef.util.baritone;

import me.sailex.automatone.api.pathing.goals.Goal;
import me.sailex.automatone.api.pathing.goals.GoalBlock;
import me.sailex.altoclef.multiversion.EntityVer;
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
      return this.entity.getBlockPos().equals(p)
            || p.isWithinDistance(EntityVer.getPos(this.entity), this.closeEnoughDistance);
   }

   @Override
   public double heuristic(int x, int y, int z) {
      double xDiff = x - EntityVer.getPos(this.entity).getX();
      int yDiff = y - this.entity.getBlockPos().getY();
      double zDiff = z - EntityVer.getPos(this.entity).getZ();
      return GoalBlock.calculate(xDiff, yDiff, zDiff);
   }
}
