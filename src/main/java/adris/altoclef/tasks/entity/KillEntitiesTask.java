package adris.altoclef.tasks.entity;

import net.minecraft.entity.Entity;

import java.util.function.Predicate;

public class KillEntitiesTask extends DoToClosestEntityTask {
   public KillEntitiesTask(Predicate<Entity> shouldKill) {
      super(KillEntityTask::new, e -> e.isAlive() && shouldKill.test(e), (Class[])null);
   }

   public KillEntitiesTask(Predicate<Entity> shouldKill, Class<?>... entities) {
      super(KillEntityTask::new, e -> e.isAlive() && shouldKill.test(e), entities);

      assert entities != null;
   }

   public KillEntitiesTask(Class<?>... entities) {
      super(KillEntityTask::new, entities);

      assert entities != null;
   }
}
