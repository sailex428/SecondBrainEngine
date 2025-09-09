package adris.altoclef.mixins.baritone;

import baritone.utils.accessor.ServerChunkManagerAccessor;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

//@Mixin({ServerChunkManager.class})
//public abstract class MixinServerChunkManager implements ServerChunkManagerAccessor {
//   @Shadow
//   @Nullable
//   protected abstract ChunkHolder getVisibleChunkIfPresent(long var1);
//
//   @Nullable
//   @Override
//   public WorldChunk automatone$getChunkNow(int chunkX, int chunkZ) {
//      ChunkHolder chunkHolder = this.getVisibleChunkIfPresent(ChunkPos.toLong(chunkX, chunkZ));
//      return chunkHolder == null ? null : chunkHolder.getWorldChunk();
//   }
//}
