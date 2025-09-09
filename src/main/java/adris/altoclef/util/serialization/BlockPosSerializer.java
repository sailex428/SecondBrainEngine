package adris.altoclef.util.serialization;

import java.util.Arrays;
import java.util.Collection;
import net.minecraft.util.math.BlockPos;

public class BlockPosSerializer extends AbstractVectorSerializer<BlockPos> {
   protected Collection<String> getParts(BlockPos value) {
      return Arrays.asList(value.getX() + "", value.getY() + "", value.getZ() + "");
   }
}
