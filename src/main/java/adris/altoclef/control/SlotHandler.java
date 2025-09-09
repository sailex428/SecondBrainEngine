package adris.altoclef.control;

import adris.altoclef.AltoClefController;
import adris.altoclef.Debug;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.slots.PlayerSlot;
import adris.altoclef.util.slots.Slot;
import baritone.api.entity.IInventoryProvider;
import baritone.api.entity.LivingEntityInventory;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;

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

   public void clickSlot(Slot slot, int mouseButton, ClickType type) {
      if (slot != null && !slot.equals(Slot.UNDEFINED)) {
         NonNullList<ItemStack> inventory = slot.getInventory();
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
            this.controller.getEntity().spawnAtLocation(this.cursorStack.copy());
            this.setCursorStack(ItemStack.EMPTY);
            this.registerSlotAction();
         }
      }
   }

   public void forceEquipItemToOffhand(Item toEquip) {
      LivingEntityInventory inventory = ((IInventoryProvider)this.controller.getEntity()).getLivingInventory();
      ItemStack offhandStack = inventory.getItem(0);
      if (!offhandStack.is(toEquip)) {
         for (int i = 0; i < inventory.main.size(); i++) {
            ItemStack potential = (ItemStack)inventory.main.get(i);
            if (potential.is(toEquip)) {
               inventory.setItem(0, potential);
               inventory.main.set(i, offhandStack);
               this.registerSlotAction();
               return;
            }
         }
      }
   }

   public boolean forceEquipItem(Item[] toEquip) {
      LivingEntityInventory inventory = ((IInventoryProvider)this.controller.getEntity()).getLivingInventory();
      if (Arrays.stream(toEquip).allMatch(ix -> ix == inventory.getMainHandStack().getItem())) {
         return true;
      } else {
         for (int i = 0; i < 9; i++) {
            int finalI = i;
            if (Arrays.stream(toEquip).allMatch(it -> it == inventory.getItem(finalI).getItem())) {
               inventory.selectedSlot = i;
               this.registerSlotAction();
               return true;
            }
         }

         for (int ix = 9; ix < inventory.main.size(); ix++) {
            int finalI = ix;
            if (Arrays.stream(toEquip).allMatch(it -> it == inventory.getItem(finalI).getItem())) {
               ItemStack handStack = inventory.getMainHandStack();
               inventory.setItem(inventory.selectedSlot, inventory.getItem(ix));
               inventory.setItem(ix, handStack);
               this.registerSlotAction();
               return true;
            }
         }

         return false;
      }
   }

   public boolean forceEquipItem(Item toEquip) {
      LivingEntityInventory inventory = ((IInventoryProvider)this.controller.getEntity()).getLivingInventory();
      if (inventory.getMainHandStack().is(toEquip)) {
         return true;
      } else {
         for (int i = 0; i < 9; i++) {
            if (inventory.getItem(i).is(toEquip)) {
               inventory.selectedSlot = i;
               this.registerSlotAction();
               return true;
            }
         }

         for (int ix = 9; ix < inventory.main.size(); ix++) {
            if (inventory.getItem(ix).is(toEquip)) {
               ItemStack handStack = inventory.getMainHandStack();
               inventory.setItem(inventory.selectedSlot, inventory.getItem(ix));
               inventory.setItem(ix, handStack);
               this.registerSlotAction();
               return true;
            }
         }

         return false;
      }
   }

   public boolean forceDeequip(Predicate<ItemStack> isBad) {
      LivingEntityInventory inventory = ((IInventoryProvider)this.controller.getEntity()).getLivingInventory();
      ItemStack equip = inventory.getMainHandStack();
      if (isBad.test(equip)) {
         int emptySlot = inventory.getEmptySlot();
         if (emptySlot != -1) {
            if (LivingEntityInventory.isValidHotbarIndex(emptySlot)) {
               inventory.selectedSlot = emptySlot;
            } else {
               inventory.setItem(emptySlot, equip);
               inventory.setItem(inventory.selectedSlot, ItemStack.EMPTY);
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
      return this.forceDeequip(stack -> stack.getItem() instanceof TieredItem);
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
               || item instanceof FoodOnAStickItem
               || item == Items.COMPASS
               || item instanceof EmptyMapItem
               || item instanceof ArmorItem
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
      LivingEntityInventory inventory = ((IInventoryProvider)controller.getEntity()).getLivingInventory();

      for (Item item : target.getMatches()) {
         if (item instanceof ArmorItem armorItem) {
            EquipmentSlot slotType = armorItem.getType().getSlot();
            if (!controller.getEntity().getItemBySlot(slotType).is(item)) {
               for (int i = 0; i < inventory.getContainerSize(); i++) {
                  ItemStack stackInSlot = inventory.getItem(i);
                  if (stackInSlot.is(item)) {
                     ItemStack currentlyEquipped = controller.getEntity().getItemBySlot(slotType).copy();
                     controller.getEntity().setItemSlot(slotType, stackInSlot.copy());
                     inventory.setItem(i, currentlyEquipped);
                     this.registerSlotAction();
                     break;
                  }
               }
            }
         }
      }
   }
}
