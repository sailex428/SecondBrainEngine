package me.sailex.altoclef.trackers.blacklisting;

import me.sailex.altoclef.util.helpers.WorldHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class WorldLocateBlacklist extends AbstractObjectBlacklist<BlockPos> {
   protected Vec3d getPos(BlockPos item) {
      return WorldHelper.toVec3d(item);
   }
}
