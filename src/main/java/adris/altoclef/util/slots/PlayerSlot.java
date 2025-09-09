package adris.altoclef.util.slots;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerInventory;

public final class PlayerSlot {
   public static final int ARMOR_BOOTS_SLOT_INDEX = 0;
   public static final int ARMOR_LEGGINGS_SLOT_INDEX = 1;
   public static final int ARMOR_CHESTPLATE_SLOT_INDEX = 2;
   public static final int ARMOR_HELMET_SLOT_INDEX = 3;
   public static final int[] ARMOR_SLOTS_INDICES = new int[]{0, 1, 2, 3};
   public static final int OFFHAND_SLOT_INDEX = 0;

   public static Slot getMainSlot(PlayerInventory inventory, int index) {
      return new Slot(inventory.main, index);
   }

   public static Slot getArmorSlot(PlayerInventory inventory, int armorIndex) {
      return new Slot(inventory.armor, armorIndex);
   }

   public static Slot getOffhandSlot(PlayerInventory inventory) {
      return new Slot(inventory.offHand, 0);
   }

   public static Slot getEquipSlot(PlayerInventory inventory, EquipmentSlot equipSlot) {
      switch (equipSlot.getType()) {
         case HAND:
            if (equipSlot == EquipmentSlot.MAINHAND) {
               return getMainSlot(inventory, inventory.selectedSlot);
            }

            return getOffhandSlot(inventory);
         case ARMOR:
            return getArmorSlot(inventory, equipSlot.getEntitySlotId());
         default:
            return Slot.UNDEFINED;
      }
   }

   public static Slot getEquipSlot(PlayerInventory inventory) {
      return getEquipSlot(inventory, EquipmentSlot.MAINHAND);
   }
}
