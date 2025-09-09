package adris.altoclef.player2api.status;

import adris.altoclef.AltoClefController;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.helpers.ItemHelper;
import baritone.api.entity.IAutomatone;
import java.util.*;
import java.util.Map.Entry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShieldItem;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction.Axis;

public class StatusUtils {
   public static String getInventoryString(AltoClefController mod) {
      Map<String, Integer> counts = new HashMap<>();

      for (int i = 0; i < mod.getBaritone().getEntityContext().inventory().getContainerSize(); i++) {
         ItemStack stack = mod.getBaritone().getEntityContext().inventory().getItem(i);
         if (!stack.isEmpty()) {
            String name = ItemHelper.stripItemName(stack.getItem());
            counts.put(name, counts.getOrDefault(name, 0) + stack.getCount());
         }
      }

      ObjectStatus status = new ObjectStatus();

      for (Entry<String, Integer> entry : counts.entrySet()) {
         status.add(entry.getKey(), entry.getValue().toString());
      }

      return status.toString();
   }

   public static String getDimensionString(AltoClefController mod) {
      return mod.getWorld().getRegistryKey().getValue().toString().replace("minecraft:", "");
   }

   public static String getWeatherString(AltoClefController mod) {
      boolean isRaining = mod.getWorld().isRaining();
      boolean isThundering = mod.getWorld().isThundering();
      ObjectStatus status = new ObjectStatus().add("isRaining", String.valueOf(isRaining)).add("isThundering",
            String.valueOf(isThundering));
      return status.toString();
   }

   public static String getSpawnPosString(AltoClefController mod) {
      BlockPos spawnPos = mod.getWorld().getSpawnPos();
      return String.format("(%d, %d, %d)", spawnPos.getX(), spawnPos.getY(), spawnPos.getZ());
   }

   public static String getTaskStatusString(AltoClefController mod) {
      String noTask = "No tasks currently running.";
      List<Task> tasks = mod.getUserTaskChain().getTasks();
      // ignore lookATOwner task
      return tasks.isEmpty() ? noTask
            : tasks.get(0).toString().contains("LookAtOwner") ? noTask : tasks.get(0).toString();
   }

   public static String getNearbyBlocksString(AltoClefController mod) {
      int radius = 12;
      BlockPos center = mod.getPlayer().getBlockPos();
      Map<String, Integer> blockCounts = new HashMap<>();

      for (int dx = -radius; dx <= radius; dx++) {
         for (int dy = -radius; dy <= radius; dy++) {
            for (int dz = -radius; dz <= radius; dz++) {
               BlockPos pos = center.add(dx, dy, dz);
               String blockName = mod.getWorld().getBlockState(pos).getBlock().getTranslationKey()
                     .replace("block.minecraft.", "");
               if (!blockName.equals("air")) {
                  blockCounts.put(blockName, blockCounts.getOrDefault(blockName, 0) + 1);
               }
            }
         }
      }

      ObjectStatus status = new ObjectStatus();

      for (Entry<String, Integer> entry : blockCounts.entrySet()) {
         status.add(entry.getKey(), entry.getValue().toString());
      }

      return status.toString();
   }

   public static String getOxygenString(AltoClefController mod) {
      return String.format("%s/300", mod.getPlayer().getAir());
   }

   public static String getNearbyHostileMobs(AltoClefController mod) {
      int radius = 32;
      List<String> descriptions = new ArrayList<>();

      for (Entity entity : mod.getWorld().iterateEntities()) {
         if (entity instanceof HostileEntity && entity.distanceTo(mod.getPlayer()) < radius) {
            String type = entity.getType().getTranslationKey();
            String niceName = type.replace("entity.minecraft.", "");
            String position = entity.getPos().floorAlongAxes(EnumSet.allOf(Axis.class)).toString();
            descriptions.add(niceName + " at " + position);
         }
      }

      return descriptions.isEmpty()
            ? String.format("no nearby hostile mobs within %d", radius)
            : "[" + String.join(",", descriptions.stream().map(s -> "\"" + s + "\"").toArray(String[]::new)) + "]";
   }

   public static String getEquippedArmorStatusString(AltoClefController mod) {
      LivingEntity player = mod.getPlayer();
      ObjectStatus status = new ObjectStatus();
      ItemStack head = player.getEquippedStack(EquipmentSlot.HEAD);
      ItemStack chest = player.getEquippedStack(EquipmentSlot.CHEST);
      ItemStack legs = player.getEquippedStack(EquipmentSlot.LEGS);
      ItemStack feet = player.getEquippedStack(EquipmentSlot.FEET);
      ItemStack offhand = player.getEquippedStack(EquipmentSlot.OFFHAND);
      status.add("helmet",
            !head.isEmpty() && head.getItem() instanceof ArmorItem
                  ? head.getItem().getTranslationKey().replace("item.minecraft.", "")
                  : "none");
      status.add(
            "chestplate",
            !chest.isEmpty() && chest.getItem() instanceof ArmorItem
                  ? chest.getItem().getTranslationKey().replace("item.minecraft.", "")
                  : "none");
      status.add("leggings",
            !legs.isEmpty() && legs.getItem() instanceof ArmorItem
                  ? legs.getItem().getTranslationKey().replace("item.minecraft.", "")
                  : "none");
      status.add("boots",
            !feet.isEmpty() && feet.getItem() instanceof ArmorItem
                  ? feet.getItem().getTranslationKey().replace("item.minecraft.", "")
                  : "none");
      status.add(
            "offhand_shield",
            !offhand.isEmpty() && offhand.getItem() instanceof ShieldItem
                  ? offhand.getItem().getTranslationKey().replace("item.minecraft.", "")
                  : "none");
      return status.toString();
   }

   public static String getNearbyPlayers(AltoClefController mod) {
      List<String> descriptions = new ArrayList<>();

      for (Entity entity : mod.getEntityTracker().getCloseEntities()) {
         if (entity instanceof PlayerEntity player && entity.distanceTo(mod.getPlayer()) < 32.0F) {
            String username = player.getName().getString();
            String position = entity.getPos().floorAlongAxes(EnumSet.allOf(Axis.class)).toString();
            descriptions.add(username + " at " + position);
         }
      }

      return descriptions.isEmpty()
            ? String.format("no nearby users within %d", 32)
            : "[" + String.join(",", descriptions.stream().map(s -> "\"" + s + "\"").toArray(String[]::new)) + "]";
   }

   public static String getNearbyNPCs(AltoClefController mod) {
      List<String> descriptions = new ArrayList<>();

      for (Entity entity : mod.getEntityTracker().getCloseEntities()) {
         if (entity instanceof IAutomatone && entity.distanceTo(mod.getPlayer()) < 32.0F) {
            String username = entity.getDisplayName().getString();
            if (!Objects.equals(username, mod.getPlayer().getDisplayName().getString())) {
               String position = entity.getPos().floorAlongAxes(EnumSet.allOf(Axis.class)).toString();
               descriptions.add(username + " at " + position);
            }
         }
      }

      return descriptions.isEmpty()
            ? String.format("no nearby npcs within %d", 32)
            : "[" + String.join(",", descriptions.stream().map(s -> "\"" + s + "\"").toArray(String[]::new)) + "]";
   }

   public static float getUserNameDistance(AltoClefController mod, String targetUsername) {
      for (PlayerEntity player : mod.getWorld().getPlayers()) {
         String username = player.getName().getString();
         if (username.equals(targetUsername)) {
            return player.distanceTo(mod.getPlayer());
         }
      }

      return Float.MAX_VALUE;
   }

   public static String getDifficulty(AltoClefController mod) {
      return mod.getWorld().getDifficulty().toString();
   }

   public static String getTimeString(AltoClefController mod) {
      ObjectStatus status = new ObjectStatus();
      status.add("isDay", Boolean.toString(mod.getWorld().isDay()));
      status.add("timeOfDay", String.format("%d/24,000", mod.getWorld().getTimeOfDay() % 24000L));
      return status.toString();
   }

   public static String getGamemodeString(AltoClefController mod) {
      return mod.getInteractionManager().getGameType().isCreative() ? "creative" : "survival";
   }

   public static String getTaskTree(AltoClefController mod) {
      Task task = mod.getUserTaskChain().getCurrentTask();
      return task == null ? "Task tree is empty" : task.getTaskTree();
   }

   public static float getDistanceToUUID(AltoClefController mod, UUID target) {
      // for (Player player : mod.getWorld().players()) {
      // if (player.getUUID().equals(target)) {
      // return player.distanceTo(mod.getPlayer());
      // }
      // }
      for (Entity entity : mod.getWorld().iterateEntities()) {
         if (entity.getUuid().equals(target)) {
            return entity.distanceTo(mod.getPlayer());
         }
      }

      return Float.MAX_VALUE;
   }

   public static float getDistanceToUsername(AltoClefController mod, String username) {
      return mod.getWorld().getPlayers().stream()
            .filter(p -> p.getName().getString().equals(username))
            .findFirst()
            .map(p -> p.distanceTo(mod.getPlayer()))
            .orElse(Float.MAX_VALUE);
   }
}
