package me.sailex.altoclef.multiversion;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;

public class PlayerInventoryVer {

    public static DefaultedList<ItemStack> getOffHandStack(PlayerInventory inv) {
        //? >=1.21.8 {
        /*DefaultedList<ItemStack> stacks = DefaultedList.ofSize(1);
        stacks.addFirst( inv.getStack(EquipmentSlot.OFFHAND.getEntitySlotId()));
        return stacks;
        *///?} else {
        return inv.offHand;
         //?}
    }

    public static DefaultedList<ItemStack> getMainInventory(PlayerInventory inv) {
        //? >=1.21.8 {
        /*return inv.getMainStacks();
        *///?} else {
        return inv.main;
         //?}
    }

    public static ItemStack getMainHandStack(PlayerInventory inv) {
        //? >=1.21.8 {
        /*return inv.getStack(EquipmentSlot.MAINHAND.getEntitySlotId());
        *///?} else {
        return inv.getMainHandStack();
         //?}
    }

    public static int getSelectedSlot(PlayerInventory inv) {
        //? >=1.21.8 {
        /*return inv.getSelectedSlot();
        *///?} else {
        return inv.selectedSlot;
         //?}
    }

    public static void setSelectedSlot(PlayerInventory inv, int slot) {
        //? >=1.21.8 {
        /*inv.setSelectedSlot(slot);
        *///?} else {
        inv.selectedSlot = slot;
         //?}
    }

    public static DefaultedList<ItemStack> getArmorSlots(PlayerInventory inv) {
        //? >=1.21.8 {
        /*DefaultedList<ItemStack> stacks = DefaultedList.ofSize(4);
        stacks.add(inv.getStack(EquipmentSlot.CHEST.getEntitySlotId()));
        stacks.add(inv.getStack(EquipmentSlot.LEGS.getEntitySlotId()));
        stacks.add(inv.getStack(EquipmentSlot.FEET.getEntitySlotId()));
        stacks.add(inv.getStack(EquipmentSlot.HEAD.getEntitySlotId()));
        return stacks;
        *///?} else {
        return inv.armor;
         //?}
    }

}
