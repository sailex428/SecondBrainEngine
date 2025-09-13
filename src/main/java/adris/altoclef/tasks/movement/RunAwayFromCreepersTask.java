package adris.altoclef.tasks.movement;

import adris.altoclef.AltoClefController;
import adris.altoclef.chains.MobDefenseChain;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.baritone.GoalRunAwayFromEntities;
import baritone.api.pathing.goals.Goal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class RunAwayFromCreepersTask extends CustomBaritoneGoalTask {
   private final double distanceToRun;

   public RunAwayFromCreepersTask(double distance) {
      this.distanceToRun = distance;
   }

   @Override
   protected boolean isEqual(Task other) {
      return other instanceof RunAwayFromCreepersTask task ? !(Math.abs(task.distanceToRun - this.distanceToRun) > 1.0) : false;
   }

   @Override
   protected String toDebugString() {
      return "Run " + this.distanceToRun + " blocks away from creepers";
   }

   @Override
   protected Goal newGoal(AltoClefController mod) {
      mod.getBaritone().getPathingBehavior().forceCancel();
      return new GoalRunAwayFromCreepers(mod, this.distanceToRun);
   }

   private static class GoalRunAwayFromCreepers extends GoalRunAwayFromEntities {
      public GoalRunAwayFromCreepers(AltoClefController mod, double distance) {
         super(mod, distance, false, 10.0);
      }

      @Override
      protected List<Entity> getEntities(AltoClefController mod) {
         return new ArrayList<>(mod.getEntityTracker().getTrackedEntities(CreeperEntity.class));
      }

      @Override
      protected double getCostOfEntity(Entity entity, int x, int y, int z) {
         return entity instanceof CreeperEntity
            ? MobDefenseChain.getCreeperSafety(new Vec3d(x + 0.5, y + 0.5, z + 0.5), (CreeperEntity)entity)
            : super.getCostOfEntity(entity, x, y, z);
      }
   }
}
