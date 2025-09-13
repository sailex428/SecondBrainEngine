package me.sailex.altoclef.trackers;

import me.sailex.altoclef.Debug;
import me.sailex.altoclef.eventbus.EventBus;
import me.sailex.altoclef.eventbus.events.PlayerCollidedWithEntityEvent;
import me.sailex.altoclef.mixins.PersistentProjectileEntityAccessor;
import me.sailex.altoclef.trackers.blacklisting.EntityLocateBlacklist;
import me.sailex.altoclef.util.ItemTarget;
import me.sailex.altoclef.util.baritone.CachedProjectile;
import me.sailex.altoclef.util.helpers.BaritoneHelper;
import me.sailex.altoclef.util.helpers.EntityHelper;
import me.sailex.altoclef.util.helpers.ProjectileHelper;
import me.sailex.altoclef.util.helpers.WorldHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.item.Item;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Predicate;

public class EntityTracker extends Tracker {
   private final HashMap<Item, List<ItemEntity>> itemDropLocations = new HashMap<>();
   private final HashMap<Class, List<Entity>> entityMap = new HashMap<>();
   private final List<Entity> closeEntities = new ArrayList<>();
   private final List<LivingEntity> hostiles = new ArrayList<>();
   private final List<CachedProjectile> projectiles = new ArrayList<>();
   private final HashMap<String, PlayerEntity> playerMap = new HashMap<>();
   private final HashMap<String, Vec3d> playerLastCoordinates = new HashMap<>();
   private final EntityLocateBlacklist entityBlacklist = new EntityLocateBlacklist();
   private final HashMap<LivingEntity, List<Entity>> entitiesCollidingWithPlayerAccumulator = new HashMap<>();
   private final HashMap<LivingEntity, HashSet<Entity>> entitiesCollidingWithPlayer = new HashMap<>();

   public EntityTracker(TrackerManager manager) {
      super(manager);
      EventBus.subscribe(PlayerCollidedWithEntityEvent.class, evt -> this.registerPlayerCollision(evt.player, evt.other));
   }

   private static Class squashType(Class<?> type) {
      return PlayerEntity.class.isAssignableFrom(type) ? PlayerEntity.class : type;
   }

   private void registerPlayerCollision(LivingEntity player, Entity entity) {
      if (!this.entitiesCollidingWithPlayerAccumulator.containsKey(player)) {
         this.entitiesCollidingWithPlayerAccumulator.put(player, new ArrayList<>());
      }

      this.entitiesCollidingWithPlayerAccumulator.get(player).add(entity);
   }

   public boolean isCollidingWithPlayer(LivingEntity player, Entity entity) {
      return this.entitiesCollidingWithPlayer.containsKey(player) && this.entitiesCollidingWithPlayer.get(player).contains(entity);
   }

   public boolean isCollidingWithPlayer(Entity entity) {
      return this.isCollidingWithPlayer(this.mod.getPlayer(), entity);
   }

   public Optional<ItemEntity> getClosestItemDrop(Item... items) {
      return this.getClosestItemDrop(this.mod.getPlayer().getPos(), items);
   }

   public Optional<ItemEntity> getClosestItemDrop(Vec3d position, Item... items) {
      return this.getClosestItemDrop(position, entity -> true, items);
   }

   public Optional<ItemEntity> getClosestItemDrop(Vec3d position, ItemTarget... items) {
      return this.getClosestItemDrop(position, entity -> true, items);
   }

   public Optional<ItemEntity> getClosestItemDrop(Predicate<ItemEntity> acceptPredicate, Item... items) {
      return this.getClosestItemDrop(this.mod.getPlayer().getPos(), acceptPredicate, items);
   }

   public Optional<ItemEntity> getClosestItemDrop(Vec3d position, Predicate<ItemEntity> acceptPredicate, Item... items) {
      this.ensureUpdated();
      ItemTarget[] tempTargetList = new ItemTarget[items.length];

      for (int i = 0; i < items.length; i++) {
         tempTargetList[i] = new ItemTarget(items[i], 9999999);
      }

      return this.getClosestItemDrop(position, acceptPredicate, tempTargetList);
   }

   public Optional<ItemEntity> getClosestItemDrop(Vec3d position, Predicate<ItemEntity> acceptPredicate, ItemTarget... targets) {
      this.ensureUpdated();
      if (targets.length == 0) {
         Debug.logError("You asked for the drop position of zero items... Most likely a typo.");
         return Optional.empty();
      } else if (!this.itemDropped(targets)) {
         return Optional.empty();
      } else {
         ItemEntity closestEntity = null;
         float minCost = Float.POSITIVE_INFINITY;

         for (ItemTarget target : targets) {
            for (Item item : target.getMatches()) {
               if (this.itemDropped(item)) {
                  for (ItemEntity entity : this.itemDropLocations.get(item)) {
                     if (!this.entityBlacklist.unreachable(entity) && entity.getStack().getItem().equals(item) && acceptPredicate.test(entity)) {
                        float cost = (float)BaritoneHelper.calculateGenericHeuristic(position, entity.getPos());
                        if (cost < minCost) {
                           minCost = cost;
                           closestEntity = entity;
                        }
                     }
                  }
               }
            }
         }

         return Optional.ofNullable(closestEntity);
      }
   }

   private Class[] parsePossiblyNullEntityTypes(Class... entityTypes) {
      return entityTypes == null ? this.entityMap.keySet().toArray(Class[]::new) : entityTypes;
   }

   public Optional<Entity> getClosestEntity(Class... entityTypes) {
      return this.getClosestEntity(this.mod.getPlayer().getPos(), entityTypes);
   }

   public Optional<Entity> getClosestEntity(Vec3d position, Class... entityTypes) {
      return this.getClosestEntity(position, entity -> true, entityTypes);
   }

   public Optional<Entity> getClosestEntity(Predicate<Entity> acceptPredicate, Class... entityTypes) {
      return this.getClosestEntity(this.mod.getPlayer().getPos(), acceptPredicate, entityTypes);
   }

   public Optional<Entity> getClosestEntity(Vec3d position, Predicate<Entity> acceptPredicate, Class... entityTypes) {
      entityTypes = this.parsePossiblyNullEntityTypes(entityTypes);
      Entity closestEntity = null;
      double minCost = Double.POSITIVE_INFINITY;

      for (Class toFind : entityTypes) {
         synchronized (BaritoneHelper.MINECRAFT_LOCK) {
            if (this.entityMap.containsKey(toFind)) {
               for (Entity entity : this.entityMap.get(toFind)) {
                  if (!this.entityBlacklist.unreachable(entity) && entity.isAlive() && acceptPredicate.test(entity)) {
                     double cost = entity.squaredDistanceTo(position);
                     if (cost < minCost) {
                        minCost = cost;
                        closestEntity = entity;
                     }
                  }
               }
            }
         }
      }

      return Optional.ofNullable(closestEntity);
   }

   public boolean itemDropped(Item... items) {
      this.ensureUpdated();

      for (Item item : items) {
         if (this.itemDropLocations.containsKey(item)) {
            for (ItemEntity entity : this.itemDropLocations.get(item)) {
               if (!this.entityBlacklist.unreachable(entity)) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   public boolean itemDropped(ItemTarget... targets) {
      this.ensureUpdated();

      for (ItemTarget target : targets) {
         if (this.itemDropped(target.getMatches())) {
            return true;
         }
      }

      return false;
   }

   public List<ItemEntity> getDroppedItems() {
      this.ensureUpdated();
      return this.itemDropLocations.values().stream().reduce(new ArrayList<>(), (result, drops) -> {
         result.addAll(drops);
         return result;
      });
   }

   public boolean entityFound(Predicate<Entity> shouldAccept, Class... types) {
      this.ensureUpdated();
      types = this.parsePossiblyNullEntityTypes(types);

      for (Class type : types) {
         synchronized (BaritoneHelper.MINECRAFT_LOCK) {
            for (Entity entity : this.entityMap.getOrDefault(type, Collections.emptyList())) {
               if (shouldAccept.test(entity)) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   public boolean entityFound(Class... types) {
      return this.entityFound(check -> true, types);
   }

   public <T extends Entity> List<T> getTrackedEntities(Class<T> type) {
      this.ensureUpdated();
      if (!this.entityFound(type)) {
         return Collections.emptyList();
      } else {
         synchronized (BaritoneHelper.MINECRAFT_LOCK) {
            return (List<T>)this.entityMap.get(type);
         }
      }
   }

   public List<Entity> getCloseEntities() {
      this.ensureUpdated();
      synchronized (BaritoneHelper.MINECRAFT_LOCK) {
         return this.closeEntities;
      }
   }

   public List<CachedProjectile> getProjectiles() {
      this.ensureUpdated();
      synchronized (BaritoneHelper.MINECRAFT_LOCK) {
         return this.projectiles;
      }
   }

   public List<LivingEntity> getHostiles() {
      this.ensureUpdated();
      synchronized (BaritoneHelper.MINECRAFT_LOCK) {
         return this.hostiles;
      }
   }

   public boolean isPlayerLoaded(String name) {
      this.ensureUpdated();
      synchronized (BaritoneHelper.MINECRAFT_LOCK) {
         return this.playerMap.containsKey(name);
      }
   }

   public List<String> getAllLoadedPlayerUsernames() {
      synchronized (BaritoneHelper.MINECRAFT_LOCK) {
         return new ArrayList<>(this.playerMap.keySet());
      }
   }

   public Optional<Vec3d> getPlayerMostRecentPosition(String name) {
      this.ensureUpdated();
      synchronized (BaritoneHelper.MINECRAFT_LOCK) {
         return Optional.ofNullable(this.playerLastCoordinates.getOrDefault(name, null));
      }
   }

   public Optional<PlayerEntity> getPlayerEntity(String name) {
      if (this.isPlayerLoaded(name)) {
         synchronized (BaritoneHelper.MINECRAFT_LOCK) {
            return Optional.of(this.playerMap.get(name));
         }
      } else {
         return Optional.empty();
      }
   }

   public void requestEntityUnreachable(Entity entity) {
      this.entityBlacklist.blackListItem(this.mod, entity, 3);
   }

   public boolean isEntityReachable(Entity entity) {
      return !this.entityBlacklist.unreachable(entity);
   }

   @Override
   protected synchronized void updateState() {
      synchronized (BaritoneHelper.MINECRAFT_LOCK) {
         this.itemDropLocations.clear();
         this.entityMap.clear();
         this.closeEntities.clear();
         this.projectiles.clear();
         this.hostiles.clear();
         this.playerMap.clear();
         if (this.mod.getWorld() != null) {
            this.entitiesCollidingWithPlayer.clear();

            for (Entry<LivingEntity, List<Entity>> collisions : this.entitiesCollidingWithPlayerAccumulator.entrySet()) {
               this.entitiesCollidingWithPlayer.put(collisions.getKey(), new HashSet<>());
               this.entitiesCollidingWithPlayer.get(collisions.getKey()).addAll(collisions.getValue());
            }

            this.entitiesCollidingWithPlayerAccumulator.clear();

            for (Entity entity : this.mod.getWorld().iterateEntities()) {
               Class<?> type = entity.getClass();
               type = squashType(type);
               if (entity != null && entity.isAlive() && (type != LivingEntity.class || !entity.equals(this.mod.getPlayer()))) {
                  if (!this.entityMap.containsKey(type)) {
                     this.entityMap.put(type, new ArrayList<>());
                  }

                  this.entityMap.get(type).add(entity);
                  if (this.mod.getControllerExtras().inRange(entity)) {
                     this.closeEntities.add(entity);
                  }

                  if (entity instanceof ItemEntity ientity) {
                     Item droppedItem = ientity.getStack().getItem();
                     if (ientity.isOnGround()
                        || ientity.isTouchingWater()
                        || WorldHelper.isSolidBlock(this.mod, ientity.getBlockPos().down(2))
                        || WorldHelper.isSolidBlock(this.mod, ientity.getBlockPos().down(3))) {
                        if (!this.itemDropLocations.containsKey(droppedItem)) {
                           this.itemDropLocations.put(droppedItem, new ArrayList<>());
                        }

                        this.itemDropLocations.get(droppedItem).add(ientity);
                     }
                  }

                  if (entity instanceof MobEntity) {
                     if (EntityHelper.isAngryAtPlayer(this.mod, entity)) {
                        boolean closeEnough = entity.isInRange(this.mod.getPlayer(), 26.0);
                        if (closeEnough) {
                           this.hostiles.add((LivingEntity)entity);
                        }
                     }
                  } else if (entity instanceof ProjectileEntity projEntity) {
                     if (!this.mod.getBehaviour().shouldAvoidDodgingProjectile(entity)) {
                        CachedProjectile proj = new CachedProjectile();
                        boolean inGround = false;
                        if (entity instanceof PersistentProjectileEntity) {
                           inGround = ((PersistentProjectileEntityAccessor)entity).isInGround();
                        }

                        if (!(projEntity instanceof FishingBobberEntity)
                           && !(projEntity instanceof EnderPearlEntity)
                           && !(projEntity instanceof ExperienceBottleEntity)
                           && !inGround) {
                           proj.position = projEntity.getPos();
                           proj.velocity = projEntity.getVelocity();
                           proj.gravity = ProjectileHelper.hasGravity(projEntity) ? 0.05F : 0.0;
                           proj.projectileType = projEntity.getClass();
                           this.projectiles.add(proj);
                        }
                     }
                  } else if (entity instanceof PlayerEntity player) {
                     String name = player.getName().getString();
                     this.playerMap.put(name, player);
                     this.playerLastCoordinates.put(name, player.getPos());
                  }
               }
            }
         }
      }
   }

   @Override
   protected void reset() {
      this.entityBlacklist.clear();
   }
}
