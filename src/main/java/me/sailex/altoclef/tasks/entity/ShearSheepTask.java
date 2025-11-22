package me.sailex.altoclef.tasks.entity;

import me.sailex.altoclef.AltoClefController;
import me.sailex.altoclef.Debug;
import me.sailex.altoclef.multiversion.EntityVer;
import me.sailex.altoclef.tasksystem.Task;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Hand;

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
            ((SheepEntity)entity).sheared(/*? >=1.21.8 {*/ /*mod.getWorld(), SoundCategory.PLAYERS, mod.getPlayer().getMainHandStack() *//*?} else {*/ SoundCategory.PLAYERS /*?}*/);
            mod.getPlayer().getMainHandStack().damage(1, mod.getPlayer(), /*? >=1.21 {*/ /*SheepEntity.getSlotForHand(Hand.MAIN_HAND) *//*?} else {*/ e -> {} /*?}*/ );
         }

         return null;
      }
   }

   @Override
   protected Optional<Entity> getEntityTarget(AltoClefController mod) {
      return mod.getEntityTracker()
         .getClosestEntity(
                 EntityVer.getPos(mod.getPlayer()), entity -> !(entity instanceof SheepEntity sheep) ? false : sheep.isShearable() && !sheep.isSheared(), SheepEntity.class
         );
   }

   @Override
   protected String toDebugString() {
      return "Shearing Sheep";
   }
}
