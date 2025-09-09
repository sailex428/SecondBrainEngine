package adris.altoclef.trackers;

import adris.altoclef.AltoClefController;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.helpers.WorldHelper;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;

public class MiscBlockTracker {
   private final AltoClefController mod;
   private final Map<Dimension, BlockPos> lastNetherPortalsUsed = new HashMap<>();
   private Dimension lastDimension;
   private boolean newDimensionTriggered;

   public MiscBlockTracker(AltoClefController mod) {
      this.mod = mod;
   }

   public void tick() {
      if (WorldHelper.getCurrentDimension(this.mod) != this.lastDimension) {
         this.lastDimension = WorldHelper.getCurrentDimension(this.mod);
         this.newDimensionTriggered = true;
      }

      if (AltoClefController.inGame() && this.newDimensionTriggered) {
         for (BlockPos check : WorldHelper.scanRegion(
            this.mod.getPlayer().getBlockPos().add(-1, -1, -1), this.mod.getPlayer().getBlockPos().add(1, 1, 1)
         )) {
            Block currentBlock = this.mod.getWorld().getBlockState(check).getBlock();
            if (currentBlock == Blocks.NETHER_PORTAL) {
               while (check.getY() > 0 && this.mod.getWorld().getBlockState(check.down()).getBlock() == Blocks.NETHER_PORTAL) {
                  check = check.down();
               }

               BlockPos below = check.down();
               if (WorldHelper.isSolidBlock(this.mod, below)) {
                  this.lastNetherPortalsUsed.put(WorldHelper.getCurrentDimension(this.mod), check);
                  this.newDimensionTriggered = false;
               }
               break;
            }
         }
      }
   }

   public void reset() {
      this.lastNetherPortalsUsed.clear();
   }

   public Optional<BlockPos> getLastUsedNetherPortal(Dimension dimension) {
      if (this.lastNetherPortalsUsed.containsKey(dimension)) {
         BlockPos portalPos = this.lastNetherPortalsUsed.get(dimension);
         if (this.mod.getChunkTracker().isChunkLoaded(portalPos) && !this.mod.getBlockScanner().isBlockAtPosition(portalPos, Blocks.NETHER_PORTAL)) {
            this.lastNetherPortalsUsed.remove(dimension);
            return Optional.empty();
         } else {
            return Optional.ofNullable(portalPos);
         }
      } else {
         return Optional.empty();
      }
   }
}
