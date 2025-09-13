package me.sailex.altoclef.tasks.movement;

import me.sailex.altoclef.AltoClefController;
import me.sailex.altoclef.multiversion.world.WorldVer;
import me.sailex.altoclef.tasksystem.Task;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.biome.Biome;

public class SearchWithinBiomeTask extends SearchChunksExploreTask {
   private final RegistryKey<Biome> toSearch;

   public SearchWithinBiomeTask(RegistryKey<Biome> toSearch) {
      this.toSearch = toSearch;
   }

   @Override
   protected boolean isChunkWithinSearchSpace(AltoClefController mod, ChunkPos pos) {
      return WorldVer.isBiomeAtPos(mod.getWorld(), this.toSearch, pos.getStartPos().add(1, 1, 1));
   }

   @Override
   protected boolean isEqual(Task other) {
      return other instanceof SearchWithinBiomeTask task ? task.toSearch == this.toSearch : false;
   }

   @Override
   protected String toDebugString() {
      return "Searching for+within biome: " + this.toSearch;
   }
}
