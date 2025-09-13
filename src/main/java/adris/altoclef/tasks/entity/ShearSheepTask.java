package adris.altoclef.tasks.entity;

import adris.altoclef.AltoClefController;
import adris.altoclef.Debug;
import adris.altoclef.tasksystem.Task;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;

import java.util.Optional;

public class ShearSheepTask extends AbstractDoToEntityTask {
   public ShearSheepTask() {
      super(0.0, -1.0, -1.0);
   }

   @Override
   protected boolean isSubEqual(AbstractDoToEntityTask other) {
      return other instanceof ShearSheepTask;
   }

   @Override
   protected Task onEntityInteract(AltoClefController mod, Entity entity) {
      if (!mod.getItemStorage().hasItem(Items.SHEARS)) {
         Debug.logWarning("Failed to shear sheep because you have no shears.");
         return null;
      } else {
         if (mod.getSlotHandler().forceEquipItem(Items.SHEARS)) {
            ((SheepEntity)entity).sheared(SoundCategory.PLAYERS);
            mod.getPlayer().getMainHandStack().damage(1, mod.getPlayer(), e -> {});
         }

         return null;
      }
   }

   @Override
   protected Optional<Entity> getEntityTarget(AltoClefController mod) {
      return mod.getEntityTracker()
         .getClosestEntity(
            mod.getPlayer().getPos(), entity -> !(entity instanceof SheepEntity sheep) ? false : sheep.isShearable() && !sheep.isSheared(), SheepEntity.class
         );
   }

   @Override
   protected String toDebugString() {
      return "Shearing Sheep";
   }
}
