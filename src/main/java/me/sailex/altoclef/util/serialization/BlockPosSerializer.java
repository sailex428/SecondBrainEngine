package me.sailex.altoclef.util.serialization;

import net.minecraft.util.math.BlockPos;

import java.util.Arrays;
import java.util.Collection;

public class BlockPosSerializer extends AbstractVectorSerializer<BlockPos> {
   protected Collection<String> getParts(BlockPos value) {
      return Arrays.asList(value.getX() + "", value.getY() + "", value.getZ() + "");
   }
}
