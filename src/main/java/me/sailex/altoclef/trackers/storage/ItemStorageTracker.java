package me.sailex.altoclef.trackers.storage;

import me.sailex.altoclef.AltoClefController;
import me.sailex.altoclef.trackers.Tracker;
import me.sailex.altoclef.trackers.TrackerManager;
import me.sailex.altoclef.util.ItemTarget;
import me.sailex.altoclef.util.helpers.StorageHelper;
import me.sailex.altoclef.util.slots.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class ItemStorageTracker extends Tracker {
   private final InventorySubTracker inventory;
   public final ContainerSubTracker containers;

   public ItemStorageTracker(AltoClefController mod, TrackerManager manager, Consumer<ContainerSubTracker> containerTrackerConsumer) {
      super(manager);
      this.inventory = new InventorySubTracker(manager);
      this.containers = new ContainerSubTracker(manager);
      containerTrackerConsumer.accept(this.containers);
   }

   public int getItemCount(Item... items) {
      return this.inventory.getItemCount(items);
   }

   public int getItemCount(ItemTarget... targets) {
      return Arrays.stream(targets).mapToInt(target -> this.getItemCount(target.getMatches())).sum();
   }

   @Deprecated
   public int getItemCountScreen(Item... items) {
      return this.getItemCount(items);
   }

   public int getItemCountInventoryOnly(Item... items) {
      return this.getItemCount(items);
   }

   public boolean hasItemInventoryOnly(Item... items) {
      return this.inventory.hasItem(items);
   }

   public boolean hasItem(Item... items) {
      return this.inventory.hasItem(items);
   }

   public boolean hasItemAll(Item... items) {
      return Arrays.stream(items).allMatch(xva$0 -> this.hasItem(xva$0));
   }

   public boolean hasItem(ItemTarget... targets) {
      return Arrays.stream(targets).anyMatch(target -> this.hasItem(target.getMatches()));
   }

   public boolean hasItemInOffhand(AltoClefController controller, Item item) {
      ItemStack offhand = StorageHelper.getItemStackInSlot(new Slot(controller.getInventory().offHand, 0));
      return offhand.getItem() == item;
   }

   public List<Slot> getSlotsWithItemPlayerInventory(boolean includeArmor, Item... items) {
      return this.inventory.getSlotsWithItemsPlayerInventory(includeArmor, items);
   }

   public List<ItemStack> getItemStacksPlayerInventory(boolean includeCursorSlot) {
      List<ItemStack> stacks = this.inventory.getInventoryStacks();
      if (includeCursorSlot) {
         stacks.add(0, this.mod.getSlotHandler().getCursorStack());
      }

      return stacks;
   }

   public List<Slot> getSlotsThatCanFitInPlayerInventory(ItemStack stack, boolean acceptPartial) {
      return this.inventory.getSlotsThatCanFit(stack, acceptPartial);
   }

   public Optional<Slot> getSlotThatCanFitInPlayerInventory(ItemStack stack, boolean acceptPartial) {
      return this.getSlotsThatCanFitInPlayerInventory(stack, acceptPartial).stream().findFirst();
   }

   public boolean hasEmptyInventorySlot() {
      return this.inventory.hasEmptySlot();
   }

   public boolean hasItemContainer(Predicate<ContainerCache> accept, Item... items) {
      return this.containers.getCachedContainers(accept).stream().anyMatch(cache -> cache.hasItem(items));
   }

   public Optional<ContainerCache> getContainerAtPosition(BlockPos pos) {
      return this.containers.getContainerAtPosition(pos);
   }

   public List<ContainerCache> getContainersWithItem(Item... items) {
      return this.containers.getContainersWithItem(items);
   }

   public Optional<ContainerCache> getClosestContainerWithItem(Vec3d pos, Item... items) {
      return this.containers
         .getCachedContainers(c -> c.hasItem(items))
         .stream()
         .min(Comparator.comparingDouble(c -> c.getBlockPos().getSquaredDistance(new Vec3i((int)pos.getX(), (int)pos.getY(), (int)pos.getZ()))));
   }

   public Optional<BlockPos> getLastBlockPosInteraction() {
      return this.containers.getLastInteractedContainer();
   }

   public void registerSlotAction() {
      this.inventory.setDirty();
   }

   @Override
   protected void updateState() {
      this.inventory.ensureUpdated();
      this.containers.ensureUpdated();
   }

   @Override
   protected void reset() {
      this.inventory.reset();
      this.containers.reset();
   }
}
