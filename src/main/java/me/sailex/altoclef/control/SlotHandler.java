package me.sailex.altoclef.control;

import me.sailex.altoclef.AltoClefController;
import me.sailex.altoclef.Debug;
import me.sailex.altoclef.multiversion.PlayerInventoryVer;
import me.sailex.altoclef.multiversion.ServerPlayerEntityVer;
import me.sailex.altoclef.multiversion.item.ToolItemVer;
import me.sailex.altoclef.util.ItemTarget;
import me.sailex.altoclef.util.slots.PlayerSlot;
import me.sailex.altoclef.util.slots.Slot;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.BucketItem;
import net.minecraft.item.EmptyMapItem;
import net.minecraft.item.EnderEyeItem;
import net.minecraft.item.FireworkRocketItem;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.OnAStickItem;
import net.minecraft.item.PotionItem;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.collection.DefaultedList;

import java.util.Arrays;
import java.util.function.Predicate;

public class SlotHandler {
   private final AltoClefController controller;
   private ItemStack cursorStack = ItemStack.EMPTY;

   public SlotHandler(AltoClefController controller) {
      this.controller = controller;
   }

   public ItemStack getCursorStack() {
      return this.cursorStack;
   }

   public void setCursorStack(ItemStack stack) {
      this.cursorStack = stack != null && !stack.isEmpty() ? stack : ItemStack.EMPTY;
   }

   public boolean canDoSlotAction() {
      return true;
   }

   public void registerSlotAction() {
      this.controller.getItemStorage().registerSlotAction();
   }

   public void clickSlot(Slot slot, int mouseButton, SlotActionType type) {
      if (slot != null && !slot.equals(Slot.UNDEFINED)) {
         DefaultedList<ItemStack> inventory = slot.getInventory();
         int index = slot.getIndex();
         if (inventory == null) {
            Debug.logWarning("Attempt to click a slot without an inventory: " + slot);
         } else {
            ItemStack slotStack = (ItemStack)inventory.get(index);
            switch (type) {
               case PICKUP:
                  ItemStack temp = this.cursorStack;
                  this.setCursorStack(slotStack);
                  inventory.set(index, temp);
                  break;
               case QUICK_MOVE:
                  Debug.logError("QUICK_MOVE is NYI.");
                  break;
               default:
                  Debug.logWarning("Unsupported SlotActionType: " + type);
            }

            this.registerSlotAction();
         }
      } else {
         if (!this.cursorStack.isEmpty()) {
            ServerPlayerEntityVer.dropStack(this.controller.getEntity(), this.cursorStack.copy());
            this.setCursorStack(ItemStack.EMPTY);
            this.registerSlotAction();
         }
      }
   }

   public void forceEquipItemToOffhand(Item toEquip) {
      PlayerInventory inventory = this.controller.getEntity().getInventory();
      ItemStack offhandStack = inventory.getStack(0);
      if (!offhandStack.isOf(toEquip)) {
         for (int i = 0; i < PlayerInventoryVer.getMainInventory(inventory).size(); i++) {
            ItemStack potential = PlayerInventoryVer.getMainInventory(inventory).get(i);
            if (potential.isOf(toEquip)) {
               inventory.setStack(0, potential);
                PlayerInventoryVer.getMainInventory(inventory).set(i, offhandStack);
               this.registerSlotAction();
               return;
            }
         }
      }
   }

   public boolean forceEquipItem(Item[] toEquip) {
      PlayerInventory inventory = this.controller.getEntity().getInventory();
      if (Arrays.stream(toEquip).allMatch(ix -> ix == PlayerInventoryVer.getMainHandStack(inventory).getItem())) {
         return true;
      } else {
         for (int i = 0; i < 9; i++) {
            int finalI = i;
            if (Arrays.stream(toEquip).allMatch(it -> it == inventory.getStack(finalI).getItem())) {
                PlayerInventoryVer.setSelectedSlot(inventory, i);
               this.registerSlotAction();
               return true;
            }
         }

         for (int ix = 9; ix < PlayerInventoryVer.getMainInventory(inventory).size(); ix++) {
            int finalI = ix;
            if (Arrays.stream(toEquip).allMatch(it -> it == inventory.getStack(finalI).getItem())) {
               ItemStack handStack = PlayerInventoryVer.getMainHandStack(inventory);
               inventory.setStack(PlayerInventoryVer.getSelectedSlot(inventory), inventory.getStack(ix));
               inventory.setStack(ix, handStack);
               this.registerSlotAction();
               return true;
            }
         }

         return false;
      }
   }

   public boolean forceEquipItem(Item toEquip) {
      PlayerInventory inventory = this.controller.getEntity().getInventory();
      if (PlayerInventoryVer.getMainHandStack(inventory).isOf(toEquip)) {
         return true;
      } else {
         for (int i = 0; i < 9; i++) {
            if (inventory.getStack(i).isOf(toEquip)) {
               PlayerInventoryVer.setSelectedSlot(inventory, i);
               this.registerSlotAction();
               return true;
            }
         }

         for (int ix = 9; ix < PlayerInventoryVer.getMainInventory(inventory).size(); ix++) {
            if (inventory.getStack(ix).isOf(toEquip)) {
               ItemStack handStack = PlayerInventoryVer.getMainHandStack(inventory);
               inventory.setStack(PlayerInventoryVer.getSelectedSlot(inventory), inventory.getStack(ix));
               inventory.setStack(ix, handStack);
               this.registerSlotAction();
               return true;
            }
         }

         return false;
      }
   }

   public boolean forceDeequip(Predicate<ItemStack> isBad) {
      PlayerInventory inventory = this.controller.getEntity().getInventory();
      ItemStack equip = PlayerInventoryVer.getMainHandStack(inventory);
      if (isBad.test(equip)) {
         int emptySlot = inventory.getEmptySlot();
         if (emptySlot != -1) {
            if (PlayerInventory.isValidHotbarIndex(emptySlot)) {
                PlayerInventoryVer.setSelectedSlot(inventory, emptySlot);
            } else {
               inventory.setStack(emptySlot, equip);
               inventory.setStack(PlayerInventoryVer.getSelectedSlot(inventory), ItemStack.EMPTY);
            }

            this.registerSlotAction();
            return true;
         } else {
            return false;
         }
      } else {
         return true;
      }
   }

   public boolean forceDeequipHitTool() {
      return this.forceDeequip(stack -> ToolItemVer.isToolItem(stack.getItem()));
   }

   public boolean forceEquipItem(ItemTarget toEquip, boolean unInterruptable) {
      if (toEquip != null && !toEquip.isEmpty()) {
         if (this.controller.getFoodChain().needsToEat() && !unInterruptable) {
            return false;
         } else {
            for (Item item : toEquip.getMatches()) {
               if (this.forceEquipItem(item)) {
                  return true;
               }
            }

            return false;
         }
      } else {
         return this.forceDeequip(stack -> !stack.isEmpty());
      }
   }

   public void refreshInventory() {
   }

   public void forceDeequipRightClickableItem() {
      this.forceDeequip(
         stack -> {
            Item item = stack.getItem();
            return item instanceof BucketItem
               || item instanceof EnderEyeItem
               || item == Items.BOW
               || item == Items.CROSSBOW
               || item == Items.FLINT_AND_STEEL
               || item == Items.FIRE_CHARGE
               || item == Items.ENDER_PEARL
               || item instanceof FireworkRocketItem
               || item instanceof SpawnEggItem
               || item == Items.END_CRYSTAL
               || item == Items.EXPERIENCE_BOTTLE
               || item instanceof PotionItem
               || item == Items.TRIDENT
               || item == Items.WRITABLE_BOOK
               || item == Items.WRITTEN_BOOK
               || item instanceof FishingRodItem
               || item instanceof OnAStickItem
               || item == Items.COMPASS
               || item instanceof EmptyMapItem
               || ToolItemVer.isArmorItem(item)
               || item == Items.LEAD
               || item == Items.SHIELD;
         }
      );
   }

   private void swapSlots(Slot slot, Slot target) {
      ItemStack stack = slot.getStack();
      ItemStack targetStack = target.getStack();
      target.getInventory().set(target.getIndex(), stack);
      slot.getInventory().set(slot.getIndex(), targetStack);
   }

   public void forceEquipSlot(AltoClefController controller, Slot slot) {
      Slot target = PlayerSlot.getEquipSlot(controller.getInventory());
      this.swapSlots(slot, target);
   }

   public void forceEquipArmor(AltoClefController controller, ItemTarget target) {
      PlayerInventory inventory = this.controller.getEntity().getInventory();

      for (Item item : target.getMatches()) {
         if (ToolItemVer.isArmorItem(item)) {
            EquipmentSlot slotType = ToolItemVer.getArmorEquipmentSlot(item);
            if (!controller.getEntity().getEquippedStack(slotType).isOf(item)) {
               for (int i = 0; i < inventory.size(); i++) {
                  ItemStack stackInSlot = inventory.getStack(i);
                  if (stackInSlot.isOf(item)) {
                     ItemStack currentlyEquipped = controller.getEntity().getEquippedStack(slotType).copy();
                     controller.getEntity().equipStack(slotType, stackInSlot.copy());
                     inventory.setStack(i, currentlyEquipped);
                     this.registerSlotAction();
                     break;
                  }
               }
            }
         }
      }
   }
}
