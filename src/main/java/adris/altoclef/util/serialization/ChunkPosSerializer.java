package adris.altoclef.util.serialization;

import java.util.Arrays;
import java.util.Collection;
import net.minecraft.util.math.ChunkPos;

public class ChunkPosSerializer extends AbstractVectorSerializer<ChunkPos> {
   protected Collection<String> getParts(ChunkPos value) {
      return Arrays.asList(value.x + "", value.z + "");
   }
}
