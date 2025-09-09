package adris.altoclef.util.baritone;

import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalXZ;
import net.minecraft.util.math.ChunkPos;

public class GoalChunk implements Goal {
   private final ChunkPos pos;

   public GoalChunk(ChunkPos pos) {
      this.pos = pos;
   }

   @Override
   public boolean isInGoal(int x, int y, int z) {
      return this.pos.getStartX() <= x && x <= this.pos.getEndX() && this.pos.getStartZ() <= z && z <= this.pos.getEndZ();
   }

   @Override
   public double heuristic(int x, int y, int z) {
      double cx = (this.pos.getStartX() + this.pos.getEndX()) / 2.0;
      double cz = (this.pos.getStartZ() + this.pos.getEndZ()) / 2.0;
      return GoalXZ.calculate(cx - x, cz - z);
   }
}
