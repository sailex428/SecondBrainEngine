package adris.altoclef.trackers.storage;

import adris.altoclef.trackers.Tracker;
import adris.altoclef.trackers.TrackerManager;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.slots.Slot;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.collection.DefaultedList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InventorySubTracker extends Tracker {
   private final Map<Item, List<Integer>> itemToSlotPlayer = new HashMap<>();
   private final Map<Item, Integer> itemCountsPlayer = new HashMap<>();

   public InventorySubTracker(TrackerManager manager) {
      super(manager);
   }

   public int getItemCount(Item... items) {
      this.ensureUpdated();
      int result = 0;
      ItemStack cursorStack = this.mod.getSlotHandler().getCursorStack();

      for (Item item : items) {
         if (cursorStack.isOf(item)) {
            result += cursorStack.getCount();
         }

         result += this.itemCountsPlayer.getOrDefault(item, 0);
      }

      return result;
   }

   public boolean hasItem(Item... items) {
      this.ensureUpdated();
      ItemStack cursorStack = this.mod.getSlotHandler().getCursorStack();

      for (Item item : items) {
         if (cursorStack.isOf(item)) {
            return true;
         }

         if (this.itemCountsPlayer.containsKey(item)) {
            return true;
         }
      }

      return false;
   }

   public List<Slot> getSlotsWithItemsPlayerInventory(boolean includeArmor, Item... items) {
      this.ensureUpdated();
      List<Slot> result = new ArrayList<>();
      PlayerInventory inventory = this.mod.getInventory();

      for (Item item : items) {
         if (this.itemToSlotPlayer.containsKey(item)) {
            for (Integer index : this.itemToSlotPlayer.get(item)) {
               result.add(new Slot(inventory.main, index));
            }
         }
      }

      if (includeArmor) {
         for (int i = 0; i < inventory.armor.size(); i++) {
            ItemStack stack = (ItemStack)inventory.armor.get(i);
            if (Arrays.stream(items).anyMatch(stack::isOf)) {
               result.add(new Slot(inventory.armor, i));
            }
         }
      }

      ItemStack offhandStack = (ItemStack)inventory.offHand.get(0);
      if (Arrays.stream(items).anyMatch(offhandStack::isOf)) {
         result.add(new Slot(inventory.offHand, 0));
      }

      return result;
   }

   public List<ItemStack> getInventoryStacks() {
      this.ensureUpdated();
      PlayerInventory inventory = this.mod.getInventory();
      List<ItemStack> stacks = new ArrayList<>();
      stacks.addAll(inventory.main);
      stacks.addAll(inventory.armor);
      stacks.addAll(inventory.offHand);
      return stacks;
   }

   public List<Slot> getSlotsThatCanFit(ItemStack item, boolean acceptPartial) {
      this.ensureUpdated();
      List<Slot> result = new ArrayList<>();
      PlayerInventory inventory = this.mod.getInventory();
      if (item.isStackable()) {
         for (int i = 0; i < inventory.main.size(); i++) {
            ItemStack stackInSlot = (ItemStack)inventory.main.get(i);
            if (ItemHelper.canStackTogether(item, stackInSlot)) {
               int roomLeft = stackInSlot.getMaxCount() - stackInSlot.getCount();
               if (acceptPartial || roomLeft >= item.getCount()) {
                  result.add(new Slot(inventory.main, i));
               }
            }
         }
      }

      for (int ix = 0; ix < inventory.main.size(); ix++) {
         if (((ItemStack)inventory.main.get(ix)).isEmpty()) {
            result.add(new Slot(inventory.main, ix));
         }
      }

      return result;
   }

   public boolean hasEmptySlot() {
      this.ensureUpdated();
      return this.itemCountsPlayer.getOrDefault(Items.AIR, 0) > 0;
   }

   @Override
   protected void updateState() {
      this.reset();
      PlayerInventory inventory = this.mod.getInventory();
      if (inventory != null) {
         for (int i = 0; i < inventory.main.size(); i++) {
            ItemStack stack = (ItemStack)inventory.main.get(i);
            this.registerItem(stack, i, inventory.main);
         }

         for (int i = 0; i < inventory.armor.size(); i++) {
            ItemStack stack = (ItemStack)inventory.armor.get(i);
            this.registerItem(stack, i, inventory.armor);
         }

         for (int i = 0; i < inventory.offHand.size(); i++) {
            ItemStack stack = (ItemStack)inventory.offHand.get(i);
            this.registerItem(stack, i, inventory.offHand);
         }
      }
   }

   private void registerItem(ItemStack stack, int index, DefaultedList<ItemStack> inventory) {
      Item item = stack.isEmpty() ? Items.AIR : stack.getItem();
      int count = stack.getCount();
      this.itemCountsPlayer.put(item, this.itemCountsPlayer.getOrDefault(item, 0) + count);
      if (inventory instanceof DefaultedList) {
         this.itemToSlotPlayer.computeIfAbsent(item, k -> new ArrayList<>()).add(index);
      }
   }

   @Override
   protected void reset() {
      this.itemToSlotPlayer.clear();
      this.itemCountsPlayer.clear();
   }
}
