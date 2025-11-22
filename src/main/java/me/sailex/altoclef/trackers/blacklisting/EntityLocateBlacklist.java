package me.sailex.altoclef.trackers.blacklisting;

import me.sailex.altoclef.multiversion.EntityVer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

public class EntityLocateBlacklist extends AbstractObjectBlacklist<Entity> {
   protected Vec3d getPos(Entity item) {
      return EntityVer.getPos(item);
   }
}
