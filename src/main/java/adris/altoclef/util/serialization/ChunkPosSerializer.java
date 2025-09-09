package adris.altoclef.util.serialization;

import net.minecraft.world.level.ChunkPos;

import java.util.Arrays;
import java.util.Collection;

public class ChunkPosSerializer extends AbstractVectorSerializer<ChunkPos> {
   protected Collection<String> getParts(ChunkPos value) {
      return Arrays.asList(value.x + "", value.z + "");
   }
}
