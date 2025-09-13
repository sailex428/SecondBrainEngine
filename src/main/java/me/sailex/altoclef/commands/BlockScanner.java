package me.sailex.altoclef.commands;

import me.sailex.altoclef.AltoClefController;
import me.sailex.altoclef.Debug;
import me.sailex.altoclef.eventbus.EventBus;
import me.sailex.altoclef.eventbus.events.BlockPlaceEvent;
import me.sailex.altoclef.multiversion.blockpos.BlockPosVer;
import me.sailex.altoclef.trackers.blacklisting.WorldLocateBlacklist;
import me.sailex.altoclef.util.Dimension;
import me.sailex.altoclef.util.helpers.BaritoneHelper;
import me.sailex.altoclef.util.helpers.WorldHelper;
import me.sailex.altoclef.util.time.TimerGame;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.function.Predicate;

public class BlockScanner {
   private static final boolean LOG = false;
   private static final int RESCAN_TICK_DELAY = 80;
   private static final int CACHED_POSITIONS_PER_BLOCK = 40;
   private final AltoClefController mod;
   private final TimerGame rescanTimer = new TimerGame(1.0);
   private final HashMap<Block, HashSet<BlockPos>> trackedBlocks = new HashMap<>();
   private final HashMap<Block, HashSet<BlockPos>> scannedBlocks = new HashMap<>();
   private final HashMap<ChunkPos, Long> scannedChunks = new HashMap<>();
   private final WorldLocateBlacklist blacklist = new WorldLocateBlacklist();
   private HashMap<Block, HashSet<BlockPos>> cachedScannedBlocks = new HashMap<>();
   private Dimension scanDimension = Dimension.OVERWORLD;
   private World scanWorld = null;
   private boolean scanning = false;
   private boolean forceStop = false;

   public BlockScanner(AltoClefController mod) {
      this.mod = mod;
      EventBus.subscribe(BlockPlaceEvent.class, evt -> this.addBlock(evt.blockState.getBlock(), evt.blockPos));
   }

   public void addBlock(Block block, BlockPos pos) {
      if (!this.isBlockAtPosition(pos, block)) {
         Debug.logInternal("INVALID SET: " + block + " " + pos);
      } else {
         if (this.trackedBlocks.containsKey(block)) {
            this.trackedBlocks.get(block).add(pos);
         } else {
            HashSet<BlockPos> set = new HashSet<>();
            set.add(pos);
            this.trackedBlocks.put(block, set);
         }
      }
   }

   public void requestBlockUnreachable(BlockPos pos, int allowedFailures) {
      this.blacklist.blackListItem(this.mod, pos, allowedFailures);
   }

   public void requestBlockUnreachable(BlockPos pos) {
      this.blacklist.blackListItem(this.mod, pos, 4);
   }

   public boolean isUnreachable(BlockPos pos) {
      return this.blacklist.unreachable(pos);
   }

   public List<BlockPos> getKnownLocationsIncludeUnreachable(Block... blocks) {
      List<BlockPos> locations = new LinkedList<>();

      for (Block block : blocks) {
         if (this.trackedBlocks.containsKey(block)) {
            locations.addAll(this.trackedBlocks.get(block));
         }
      }

      return locations;
   }

   public List<BlockPos> getKnownLocations(Block... blocks) {
      List<BlockPos> locations = this.getKnownLocationsIncludeUnreachable(blocks);
      locations.removeIf(this::isUnreachable);
      return locations;
   }

   public Optional<BlockPos> getNearestWithinRange(Vec3d pos, double range, Block... blocks) {
      Optional<BlockPos> nearest = this.getNearestBlock(pos, blocks);
      return !nearest.isEmpty() && !nearest.get().isWithinDistance(new Vec3i((int)pos.x, (int)pos.y, (int)pos.z), range) ? Optional.empty() : nearest;
   }

   public Optional<BlockPos> getNearestWithinRange(BlockPos pos, double range, Block... blocks) {
      return this.getNearestWithinRange(new Vec3d(pos.getX(), pos.getY(), pos.getZ()), range, blocks);
   }

   public boolean anyFound(Block... blocks) {
      return this.anyFound(block -> true, blocks);
   }

   public boolean anyFound(Predicate<BlockPos> isValidTest, Block... blocks) {
      for (Block block : blocks) {
         if (this.trackedBlocks.containsKey(block)) {
            for (BlockPos pos : this.trackedBlocks.get(block)) {
               if (isValidTest.test(pos) && this.mod.getWorld().getBlockState(pos).getBlock().equals(block) && !this.isUnreachable(pos)) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   public Optional<BlockPos> getNearestBlock(Block... blocks) {
      return this.getNearestBlock(this.mod.getPlayer().getPos().add(0.0, 0.6F, 0.0), blocks);
   }

   public Optional<BlockPos> getNearestBlock(Vec3d pos, Block... blocks) {
      return this.getNearestBlock(pos, p -> true, blocks);
   }

   public Optional<BlockPos> getNearestBlock(Predicate<BlockPos> isValidTest, Block... blocks) {
      return this.getNearestBlock(this.mod.getPlayer().getPos().add(0.0, 0.6F, 0.0), isValidTest, blocks);
   }

   public Optional<BlockPos> getNearestBlock(Vec3d pos, Predicate<BlockPos> isValidTest, Block... blocks) {
      Optional<BlockPos> closest = Optional.empty();

      for (Block block : blocks) {
         Optional<BlockPos> p = this.getNearestBlock(block, isValidTest, pos);
         if (p.isPresent()) {
            if (closest.isEmpty()) {
               closest = p;
            } else if (BaritoneHelper.calculateGenericHeuristic(pos, WorldHelper.toVec3d(closest.get()))
               > BaritoneHelper.calculateGenericHeuristic(pos, WorldHelper.toVec3d(p.get()))) {
               closest = p;
            }
         }
      }

      return closest;
   }

   public Optional<BlockPos> getNearestBlock(Block block, Vec3d fromPos) {
      return this.getNearestBlock(block, pos -> true, fromPos);
   }

   public Optional<BlockPos> getNearestBlock(Block block, Predicate<BlockPos> isValidTest, Vec3d fromPos) {
      BlockPos pos = null;
      double nearest = Double.POSITIVE_INFINITY;
      if (!this.trackedBlocks.containsKey(block)) {
         return Optional.empty();
      } else {
         for (BlockPos p : this.trackedBlocks.get(block)) {
            if (this.mod.getWorld().getBlockState(p).getBlock().equals(block) && isValidTest.test(p) && !this.isUnreachable(p)) {
               double dist = BaritoneHelper.calculateGenericHeuristic(fromPos, WorldHelper.toVec3d(p));
               if (dist < nearest) {
                  nearest = dist;
                  pos = p;
               }
            }
         }

         return pos != null ? Optional.of(pos) : Optional.empty();
      }
   }

   public boolean anyFoundWithinDistance(double distance, Block... blocks) {
      return this.anyFoundWithinDistance(this.mod.getPlayer().getPos().add(0.0, 0.6F, 0.0), distance, blocks);
   }

   public boolean anyFoundWithinDistance(Vec3d pos, double distance, Block... blocks) {
      Optional<BlockPos> blockPos = this.getNearestBlock(blocks);
      return blockPos.<Boolean>map(value -> value.isWithinDistance(new Vec3i((int)pos.x, (int)pos.y, (int)pos.z), distance)).orElse(false);
   }

   public double distanceToClosest(Block... blocks) {
      return this.distanceToClosest(this.mod.getPlayer().getPos().add(0.0, 0.6F, 0.0), blocks);
   }

   public double distanceToClosest(Vec3d pos, Block... blocks) {
      Optional<BlockPos> blockPos = this.getNearestBlock(blocks);
      return blockPos.<Double>map(value -> Math.sqrt(BlockPosVer.getSquaredDistance(value, pos))).orElse(Double.POSITIVE_INFINITY);
   }

   public boolean isBlockAtPosition(BlockPos pos, Block... blocks) {
      if (this.isUnreachable(pos)) {
         return false;
      } else if (!this.mod.getChunkTracker().isChunkLoaded(pos)) {
         return false;
      } else {
         World world = this.mod.getWorld();
         if (world == null) {
            return false;
         } else {
            try {
               for (Block block : blocks) {
                  if (world.isAir(pos) && WorldHelper.isAir(block)) {
                     return true;
                  }

                  BlockState state = world.getBlockState(pos);
                  if (state.getBlock() == block) {
                     return true;
                  }
               }

               return false;
            } catch (NullPointerException var9) {
               return false;
            }
         }
      }
   }

   public void reset() {
      this.trackedBlocks.clear();
      this.scannedBlocks.clear();
      this.scannedChunks.clear();
      this.rescanTimer.forceElapse();
      this.blacklist.clear();
      this.forceStop = true;
   }

   public void tick() {
      if (this.mod.getWorld() != null && this.mod.getPlayer() != null) {
         this.scanCloseBlocks();
         if (this.rescanTimer.elapsed() && !this.scanning) {
            if (this.scanDimension == WorldHelper.getCurrentDimension(this.mod) && this.mod.getWorld() == this.scanWorld) {
               this.cachedScannedBlocks = new HashMap<>(this.scannedBlocks.size());

               for (Entry<Block, HashSet<BlockPos>> entry : this.scannedBlocks.entrySet()) {
                  this.cachedScannedBlocks.put(entry.getKey(), (HashSet<BlockPos>)entry.getValue().clone());
               }

               this.scanning = true;
               this.forceStop = false;
               new Thread(() -> {
                  try {
                     this.rescan(Integer.MAX_VALUE, Integer.MAX_VALUE);
                  } catch (Exception var5) {
                     var5.printStackTrace();
                  } finally {
                     this.rescanTimer.reset();
                     this.scanning = false;
                  }
               }).start();
            } else {
               this.reset();
               this.scanWorld = this.mod.getWorld();
               this.scanDimension = WorldHelper.getCurrentDimension(this.mod);
            }
         }
      }
   }

   private void scanCloseBlocks() {
      for (Entry<Block, HashSet<BlockPos>> entry : this.cachedScannedBlocks.entrySet()) {
         if (!this.trackedBlocks.containsKey(entry.getKey())) {
            this.trackedBlocks.put(entry.getKey(), new HashSet<>());
         }

         this.trackedBlocks.get(entry.getKey()).clear();
         this.trackedBlocks.get(entry.getKey()).addAll(entry.getValue());
      }

      HashMap<Block, HashSet<BlockPos>> map = new HashMap<>();
      BlockPos pos = this.mod.getPlayer().getBlockPos();
      World world = this.mod.getPlayer().getWorld();

      for (int x = pos.getX() - 8; x <= pos.getX() + 8; x++) {
         for (int y = pos.getY() - 8; y < pos.getY() + 8; y++) {
            for (int z = pos.getZ() - 8; z <= pos.getZ() + 8; z++) {
               BlockPos p = new BlockPos(x, y, z);
               BlockState state = world.getBlockState(p);
               if (!world.getBlockState(p).isAir()) {
                  Block block = state.getBlock();
                  if (map.containsKey(block)) {
                     map.get(block).add(p);
                  } else {
                     HashSet<BlockPos> set = new HashSet<>();
                     set.add(p);
                     map.put(block, set);
                  }
               }
            }
         }
      }

      for (Entry<Block, HashSet<BlockPos>> entry : map.entrySet()) {
         this.getFirstFewPositions(entry.getValue(), this.mod.getPlayer().getPos());
         if (!this.trackedBlocks.containsKey(entry.getKey())) {
            this.trackedBlocks.put(entry.getKey(), new HashSet<>());
         }

         this.trackedBlocks.get(entry.getKey()).addAll(entry.getValue());
      }
   }

   private void rescan(int maxCount, int cutOffRadius) {
      long ms = System.currentTimeMillis();
      ChunkPos playerChunkPos = this.mod.getPlayer().getChunkPos();
      Vec3d playerPos = this.mod.getPlayer().getPos();
      HashSet<ChunkPos> visited = new HashSet<>();
      Queue<Node> queue = new ArrayDeque<>();
      queue.add(new Node(playerChunkPos, 0));

      while (!queue.isEmpty() && visited.size() < maxCount && !this.forceStop) {
         Node node = queue.poll();
         if (node.distance <= cutOffRadius && !visited.contains(node.pos) && this.mod.getWorld().getChunkManager().isChunkLoaded(node.pos.x, node.pos.z)) {
            boolean isPriorityChunk = this.getChunkDist(node.pos, playerChunkPos) <= 2;
            if (isPriorityChunk || !this.scannedChunks.containsKey(node.pos) || this.mod.getWorld().getTime() - this.scannedChunks.get(node.pos) >= 80L) {
               visited.add(node.pos);
               this.scanChunk(node.pos, playerChunkPos);
               queue.add(new Node(new ChunkPos(node.pos.x + 1, node.pos.z + 1), node.distance + 1));
               queue.add(new Node(new ChunkPos(node.pos.x - 1, node.pos.z + 1), node.distance + 1));
               queue.add(new Node(new ChunkPos(node.pos.x - 1, node.pos.z - 1), node.distance + 1));
               queue.add(new Node(new ChunkPos(node.pos.x + 1, node.pos.z - 1), node.distance + 1));
            }
         }
      }

      if (this.forceStop) {
         this.reset();
         this.forceStop = false;
      } else {
         Iterator<ChunkPos> iterator = this.scannedChunks.keySet().iterator();

         while (iterator.hasNext()) {
            ChunkPos pos = iterator.next();
            int distance = this.getChunkDist(pos, playerChunkPos);
            if (distance > cutOffRadius) {
               iterator.remove();
            }
         }

         for (HashSet<BlockPos> set : this.scannedBlocks.values()) {
            if (set.size() >= 40) {
               this.getFirstFewPositions(set, playerPos);
            }
         }
      }
   }

   private int getChunkDist(ChunkPos pos1, ChunkPos pos2) {
      return Math.abs(pos1.x - pos2.x) + Math.abs(pos1.z - pos2.z);
   }

   private void getFirstFewPositions(HashSet<BlockPos> set, Vec3d playerPos) {
      Queue<BlockPos> queue = new PriorityQueue<>(
         Comparator.comparingDouble(posx -> -BaritoneHelper.calculateGenericHeuristic(playerPos, WorldHelper.toVec3d(posx)))
      );

      for (BlockPos pos : set) {
         queue.add(pos);
         if (queue.size() > 40) {
            queue.poll();
         }
      }

      set.clear();

      for (int i = 0; i < 40 && !queue.isEmpty(); i++) {
         set.add(queue.poll());
      }
   }

   private void scanChunk(ChunkPos chunkPos, ChunkPos playerChunkPos) {
      World world = this.mod.getWorld();
      WorldChunk chunk = this.mod.getWorld().getChunk(chunkPos.x, chunkPos.z);
      this.scannedChunks.put(chunkPos, world.getTime());
      boolean isPriorityChunk = this.getChunkDist(chunkPos, playerChunkPos) <= 2;

      for (int x = chunkPos.getStartX(); x <= chunkPos.getEndX(); x++) {
         for (int y = world.getBottomY(); y < world.getTopY(); y++) {
            for (int z = chunkPos.getStartZ(); z <= chunkPos.getEndZ(); z++) {
               BlockPos p = new BlockPos(x, y, z);
               if (!this.isUnreachable(p) && !world.isOutOfHeightLimit(p)) {
                  BlockState state = chunk.getBlockState(p);
                  if (!state.isAir()) {
                     Block block = state.getBlock();
                     if (this.scannedBlocks.containsKey(block)) {
                        HashSet<BlockPos> set = this.scannedBlocks.get(block);
                        if (set.size() <= 30000 || isPriorityChunk) {
                           set.add(p);
                        }
                     } else {
                        HashSet<BlockPos> set = new HashSet<>();
                        set.add(p);
                        this.scannedBlocks.put(block, set);
                     }
                  }
               }
            }
         }
      }
   }

   private record Node(ChunkPos pos, int distance) {
   }
}
