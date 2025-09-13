package me.sailex.altoclef.util.helpers;

import me.sailex.altoclef.AltoClefController;
import me.sailex.altoclef.mixins.EntityAccessor;
import me.sailex.altoclef.multiversion.MethodWrapper;
import me.sailex.altoclef.multiversion.world.WorldVer;
import me.sailex.altoclef.util.Dimension;
import me.sailex.automatone.pathing.movement.CalculationContext;
import me.sailex.automatone.pathing.movement.MovementHelper;
import me.sailex.automatone.process.MineProcess;
import me.sailex.automatone.utils.BlockStateInterface;
import net.minecraft.block.AbstractFurnaceBlock;
import net.minecraft.block.BarrelBlock;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CartographyTableBlock;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.CraftingTableBlock;
import net.minecraft.block.EnchantingTableBlock;
import net.minecraft.block.EnderChestBlock;
import net.minecraft.block.FallingBlock;
import net.minecraft.block.FluidBlock;
import net.minecraft.block.LoomBlock;
import net.minecraft.block.RedstoneOreBlock;
import net.minecraft.block.SpawnerBlock;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.block.enums.BedPart;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public interface WorldHelper {
   static Vec3d toVec3d(BlockPos pos) {
      return pos == null ? null : new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
   }

   static Vec3d toVec3d(Vec3i pos) {
      return new Vec3d(pos.getX(), pos.getY(), pos.getZ());
   }

   static Vec3i toVec3i(Vec3d pos) {
      return new Vec3i((int)pos.getX(), (int)pos.getY(), (int)pos.getZ());
   }

   static BlockPos toBlockPos(Vec3d pos) {
      return new BlockPos((int)pos.getX(), (int)pos.getY(), (int)pos.getZ());
   }

   static boolean isSourceBlock(AltoClefController controller, BlockPos pos, boolean onlyAcceptStill) {
      World world = controller.getWorld();
      BlockState s = world.getBlockState(pos);
      if (s.getBlock() instanceof FluidBlock) {
         if (!s.getFluidState().isStill() && onlyAcceptStill) {
            return false;
         } else {
            int level = s.getFluidState().getLevel();
            BlockState above = world.getBlockState(pos.up());
            return above.getBlock() instanceof FluidBlock ? false : level == 8;
         }
      } else {
         return false;
      }
   }

   static double distanceXZSquared(Vec3d from, Vec3d to) {
      Vec3d delta = to.subtract(from);
      return delta.x * delta.x + delta.z * delta.z;
   }

   static double distanceXZ(Vec3d from, Vec3d to) {
      return Math.sqrt(distanceXZSquared(from, to));
   }

   static boolean inRangeXZ(Vec3d from, Vec3d to, double range) {
      return distanceXZSquared(from, to) < range * range;
   }

   static boolean inRangeXZ(BlockPos from, BlockPos to, double range) {
      return inRangeXZ(toVec3d(from), toVec3d(to), range);
   }

   static boolean inRangeXZ(Entity entity, Vec3d to, double range) {
      return inRangeXZ(entity.getPos(), to, range);
   }

   static boolean inRangeXZ(Entity entity, BlockPos to, double range) {
      return inRangeXZ(entity, toVec3d(to), range);
   }

   static boolean inRangeXZ(Entity entity, Entity to, double range) {
      return inRangeXZ(entity, to.getPos(), range);
   }

   static Dimension getCurrentDimension(AltoClefController controller) {
      World world = controller.getWorld();
      if (world == null) {
         return Dimension.OVERWORLD;
      } else if (world.getDimension().ultrawarm()) {
         return Dimension.NETHER;
      } else {
         return world.getDimension().natural() ? Dimension.OVERWORLD : Dimension.END;
      }
   }

   static boolean isSolidBlock(AltoClefController controller, BlockPos pos) {
      World world = controller.getWorld();
      return world.getBlockState(pos).isSolidBlock(world, pos);
   }

   static BlockPos getBedHead(AltoClefController controller, BlockPos posWithBed) {
      World world = controller.getWorld();
      BlockState state = world.getBlockState(posWithBed);
      if (state.getBlock() instanceof BedBlock) {
         Direction facing = (Direction)state.get(BedBlock.FACING);
         return ((BedPart)world.getBlockState(posWithBed).get(BedBlock.PART)).equals(BedPart.HEAD) ? posWithBed : posWithBed.offset(facing);
      } else {
         return null;
      }
   }

   static BlockPos getBedFoot(AltoClefController controller, BlockPos posWithBed) {
      World world = controller.getWorld();
      BlockState state = world.getBlockState(posWithBed);
      if (state.getBlock() instanceof BedBlock) {
         Direction facing = (Direction)state.get(BedBlock.FACING);
         return ((BedPart)world.getBlockState(posWithBed).get(BedBlock.PART)).equals(BedPart.FOOT)
            ? posWithBed
            : posWithBed.offset(facing.getOpposite());
      } else {
         return null;
      }
   }

   static int getGroundHeight(AltoClefController controller, int x, int z) {
      World world = controller.getWorld();

      for (int y = world.getTopY(); y >= world.getBottomY(); y--) {
         BlockPos check = new BlockPos(x, y, z);
         if (isSolidBlock(controller, check)) {
            return y;
         }
      }

      return -1;
   }

   static BlockPos getADesertTemple(AltoClefController controller) {
      World world = controller.getWorld();
      List<BlockPos> stonePressurePlates = controller.getBlockScanner().getKnownLocations(Blocks.STONE_PRESSURE_PLATE);
      if (!stonePressurePlates.isEmpty()) {
         for (BlockPos pos : stonePressurePlates) {
            if (world.getBlockState(pos).getBlock() == Blocks.STONE_PRESSURE_PLATE
               && world.getBlockState(pos.down()).getBlock() == Blocks.CUT_SANDSTONE
               && world.getBlockState(pos.down(2)).getBlock() == Blocks.TNT) {
               return pos;
            }
         }
      }

      return null;
   }

   static boolean isUnopenedChest(AltoClefController controller, BlockPos pos) {
      return controller.getItemStorage().getContainerAtPosition(pos).isEmpty();
   }

   static int getGroundHeight(AltoClefController controller, int x, int z, Block... groundBlocks) {
      World world = controller.getWorld();
      Set<Block> possibleBlocks = new HashSet<>(Arrays.asList(groundBlocks));

      for (int y = world.getTopY(); y >= world.getBottomY(); y--) {
         BlockPos check = new BlockPos(x, y, z);
         if (possibleBlocks.contains(world.getBlockState(check).getBlock())) {
            return y;
         }
      }

      return -1;
   }

   static boolean canBreak(AltoClefController controller, BlockPos pos) {
      boolean prevInteractionPaused = controller.getExtraBaritoneSettings().isInteractionPaused();
      controller.getExtraBaritoneSettings().setInteractionPaused(false);
      boolean canBreak = controller.getWorld().getBlockState(pos).getHardness(controller.getWorld(), pos) >= 0.0F
         && !controller.getExtraBaritoneSettings().shouldAvoidBreaking(pos)
         && MineProcess.plausibleToBreak(new CalculationContext(controller.getBaritone()), pos)
         && canReach(controller, pos);
      controller.getExtraBaritoneSettings().setInteractionPaused(prevInteractionPaused);
      return canBreak;
   }

   static boolean isInNetherPortal(AltoClefController controller) {
      LivingEntity player = controller.getPlayer();
      return player == null ? false : ((EntityAccessor)player).isInNetherPortal();
   }

   static boolean canPlace(AltoClefController controller, BlockPos pos) {
      return !controller.getExtraBaritoneSettings().shouldAvoidPlacingAt(pos) && canReach(controller, pos);
   }

   static boolean canReach(AltoClefController controller, BlockPos pos) {
      return controller.getModSettings().shouldAvoidOcean()
            && controller.getPlayer().getY() > 47.0
            && controller.getChunkTracker().isChunkLoaded(pos)
            && isOcean(controller.getWorld().getBiome(pos))
            && pos.getY() < 64
            && getGroundHeight(controller, pos.getX(), pos.getZ(), Blocks.WATER) > pos.getY()
         ? false
         : !controller.getBlockScanner().isUnreachable(pos);
   }

   static boolean isOcean(RegistryEntry<Biome> b) {
      return WorldVer.isBiome(b, BiomeKeys.OCEAN)
         || WorldVer.isBiome(b, BiomeKeys.COLD_OCEAN)
         || WorldVer.isBiome(b, BiomeKeys.DEEP_COLD_OCEAN)
         || WorldVer.isBiome(b, BiomeKeys.DEEP_OCEAN)
         || WorldVer.isBiome(b, BiomeKeys.DEEP_FROZEN_OCEAN)
         || WorldVer.isBiome(b, BiomeKeys.DEEP_LUKEWARM_OCEAN)
         || WorldVer.isBiome(b, BiomeKeys.LUKEWARM_OCEAN)
         || WorldVer.isBiome(b, BiomeKeys.WARM_OCEAN)
         || WorldVer.isBiome(b, BiomeKeys.FROZEN_OCEAN);
   }

   static boolean isAir(AltoClefController controller, BlockPos pos) {
      return controller.getBlockScanner().isBlockAtPosition(pos, Blocks.AIR, Blocks.CAVE_AIR, Blocks.VOID_AIR);
   }

   static boolean isAir(Block block) {
      return block == Blocks.AIR || block == Blocks.CAVE_AIR || block == Blocks.VOID_AIR;
   }

   static boolean isInteractableBlock(AltoClefController controller, BlockPos pos) {
      Block block = controller.getWorld().getBlockState(pos).getBlock();
      return block instanceof ChestBlock
         || block instanceof EnderChestBlock
         || block instanceof CraftingTableBlock
         || block instanceof AbstractFurnaceBlock
         || block instanceof LoomBlock
         || block instanceof CartographyTableBlock
         || block instanceof EnchantingTableBlock
         || block instanceof RedstoneOreBlock
         || block instanceof BarrelBlock;
   }

   static boolean isInsidePlayer(AltoClefController controller, BlockPos pos) {
      return pos.isWithinDistance(controller.getPlayer().getPos(), 2.0);
   }

   static Iterable<BlockPos> getBlocksTouchingPlayer(LivingEntity player) {
      return getBlocksTouchingBox(player.getBoundingBox());
   }

   static Iterable<BlockPos> getBlocksTouchingBox(Box box) {
      BlockPos min = new BlockPos((int)box.minX, (int)box.minY, (int)box.minZ);
      BlockPos max = new BlockPos((int)box.maxX, (int)box.maxY, (int)box.maxZ);
      return scanRegion(min, max);
   }

   static Iterable<BlockPos> scanRegion(BlockPos start, BlockPos end) {
      return () -> new Iterator<BlockPos>() {
         int x = start.getX();
         int y = start.getY();
         int z = start.getZ();

         @Override
         public boolean hasNext() {
            return this.y <= end.getY() && this.z <= end.getZ() && this.x <= end.getX();
         }

         public BlockPos next() {
            BlockPos result = new BlockPos(this.x, this.y, this.z);
            this.x++;
            if (this.x > end.getX()) {
               this.x = start.getX();
               this.z++;
               if (this.z > end.getZ()) {
                  this.z = start.getZ();
                  this.y++;
               }
            }

            return result;
         }
      };
   }

   static boolean fallingBlockSafeToBreak(AltoClefController controller, BlockPos pos) {
      BlockStateInterface bsi = new BlockStateInterface(controller.getBaritone().getPlayerContext());
      World clientWorld = controller.getWorld();
      if (clientWorld == null) {
         throw new AssertionError();
      } else {
         while (isFallingBlock(controller, pos)) {
            if (MovementHelper.avoidBreaking(bsi, pos.getX(), pos.getY(), pos.getZ(), clientWorld.getBlockState(pos), controller.getBaritoneSettings())) {
               return false;
            }

            pos = pos.up();
         }

         return true;
      }
   }

   static boolean isFallingBlock(AltoClefController controller, BlockPos pos) {
      World clientWorld = controller.getWorld();
      if (clientWorld == null) {
         throw new AssertionError();
      } else {
         return clientWorld.getBlockState(pos).getBlock() instanceof FallingBlock;
      }
   }

   static Entity getSpawnerEntity(AltoClefController controller, BlockPos pos) {
      World world = controller.getWorld();
      BlockState state = world.getBlockState(pos);
      return state.getBlock() instanceof SpawnerBlock && world.getBlockEntity(pos) instanceof MobSpawnerBlockEntity blockEntity
         ? MethodWrapper.getRenderedEntity(blockEntity.getLogic(), world, pos)
         : null;
   }

   static boolean isChest(AltoClefController controller, BlockPos block) {
      Block b = controller.getWorld().getBlockState(block).getBlock();
      return isChest(b);
   }

   static boolean isChest(Block b) {
      return b instanceof ChestBlock || b instanceof EnderChestBlock;
   }

   static boolean isBlock(AltoClefController controller, BlockPos pos, Block block) {
      return controller.getWorld().getBlockState(pos).getBlock() == block;
   }

   static boolean canSleep(AltoClefController controller) {
      World world = controller.getWorld();
      if (world != null) {
         if (world.isThundering() && world.isRaining()) {
            return true;
         } else {
            int time = getTimeOfDay(controller);
            return 12542 <= time && time <= 23992;
         }
      } else {
         return false;
      }
   }

   static int getTimeOfDay(AltoClefController controller) {
      World world = controller.getWorld();
      return world != null ? (int)(world.getTimeOfDay() % 24000L) : 0;
   }

   static boolean isVulnerable(LivingEntity player) {
      int armor = player.getArmor();
      float health = player.getHealth();
      if (armor <= 15 && health < 3.0F) {
         return true;
      } else {
         return armor < 10 && health < 10.0F ? true : armor < 5 && health < 18.0F;
      }
   }

   static boolean isSurroundedByHostiles(AltoClefController controller) {
      List<LivingEntity> hostiles = controller.getEntityTracker().getHostiles();
      return isSurrounded(controller, hostiles);
   }

   static boolean isSurrounded(AltoClefController controller, List<LivingEntity> entities) {
      LivingEntity player = controller.getPlayer();
      BlockPos playerPos = player.getBlockPos();
      int MIN_SIDES_TO_SURROUND = 2;
      List<Direction> uniqueSides = new ArrayList<>();

      for (Entity entity : entities) {
         if (entity.isInRange(player, 8.0)) {
            BlockPos entityPos = entity.getBlockPos();
            double angle = calculateAngle(playerPos, entityPos);
            boolean isUnique = !uniqueSides.contains(getHorizontalDirectionFromYaw(angle));
            if (isUnique) {
               uniqueSides.add(getHorizontalDirectionFromYaw(angle));
            }
         }
      }

      return uniqueSides.size() >= 2;
   }

   private static double calculateAngle(BlockPos origin, BlockPos target) {
      double translatedX = target.getX() - origin.getX();
      double translatedZ = target.getZ() - origin.getZ();
      double angleRad = Math.atan2(translatedZ, translatedX);
      double angleDeg = Math.toDegrees(angleRad);
      angleDeg -= 90.0;
      if (angleDeg < 0.0) {
         angleDeg += 360.0;
      }

      return angleDeg;
   }

   private static Direction getHorizontalDirectionFromYaw(double yaw) {
      yaw %= 360.0;
      if (yaw < 0.0) {
         yaw += 360.0;
      }

      if ((!(yaw >= 45.0) || !(yaw < 135.0)) && (!(yaw >= -315.0) || !(yaw < -225.0))) {
         if ((!(yaw >= 135.0) || !(yaw < 225.0)) && (!(yaw >= -225.0) || !(yaw < -135.0))) {
            return (!(yaw >= 225.0) || !(yaw < 315.0)) && (!(yaw >= -135.0) || !(yaw < -45.0)) ? Direction.SOUTH : Direction.EAST;
         } else {
            return Direction.NORTH;
         }
      } else {
         return Direction.WEST;
      }
   }
}
