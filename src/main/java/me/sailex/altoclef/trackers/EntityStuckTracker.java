package me.sailex.altoclef.trackers;

import me.sailex.altoclef.util.helpers.BaritoneHelper;
import me.sailex.altoclef.util.helpers.WorldHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.HashSet;
import java.util.Set;

public class EntityStuckTracker extends Tracker {
   final float MOB_RANGE = 25.0F;
   private final Set<BlockPos> blockedSpots = new HashSet<>();

   public EntityStuckTracker(TrackerManager manager) {
      super(manager);
   }

   public boolean isBlockedByEntity(BlockPos pos) {
      this.ensureUpdated();
      synchronized (BaritoneHelper.MINECRAFT_LOCK) {
         return this.blockedSpots.contains(pos);
      }
   }

   @Override
   protected synchronized void updateState() {
      synchronized (BaritoneHelper.MINECRAFT_LOCK) {
         this.blockedSpots.clear();
         LivingEntity clientPlayerEntity = this.mod.getEntity();

         for (Entity entity : this.mod.getWorld().iterateEntities()) {
            if (entity != null && entity.isAlive() && !entity.equals(clientPlayerEntity) && clientPlayerEntity.isInRange(entity, 25.0)) {
               Box b = entity.getBoundingBox();

               for (BlockPos p : WorldHelper.getBlocksTouchingBox(b)) {
                  this.blockedSpots.add(p);
               }
            }
         }
      }
   }

   @Override
   protected void reset() {
      this.blockedSpots.clear();
   }
}
