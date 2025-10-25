package me.sailex.altoclef.multiversion.item;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;

//? >=1.21.8 {
/*import net.minecraft.component.DataComponentTypes;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.registry.tag.ItemTags;
*///?} else {

import net.minecraft.item.ToolItem;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.SwordItem;
//?}

public class ToolItemVer {

    public static boolean isToolItem(final Item item) {
        //? >=1.21.8 {
        /*return item.getDefaultStack().isIn(ConventionalItemTags.TOOLS);
        *///?} else {
        return item instanceof ToolItem;
         //?}
    }

    public static float getAttackDamage(Item toolItem) {
        //? >=1.21.8 {
        /*return toolItem.getComponents().get(DataComponentTypes.DAMAGE);
        *///?} else {
        
        return ((ToolItem) toolItem).getMaterial().getAttackDamage();
        //?}
    }

    public static boolean isArmorItem(Item item) {
        //? >=1.21.8 {
        /*return item.getDefaultStack().hasChangedComponent(DataComponentTypes.EQUIPPABLE);
        *///?} else {
        return item instanceof ArmorItem;
        //?}
    }

    public static EquipmentSlot getArmorEquipmentSlot(Item armorItem) {
        //? >=1.21.8 {
        /*return armorItem.getComponents().get(DataComponentTypes.EQUIPPABLE).slot();
        *///?} else {
        return ((ArmorItem) armorItem).getType().getEquipmentSlot();
        //?}
    }

    public static boolean isSwordItem(Item item) {
        //? >=1.21.8 {
        /*return item.getDefaultStack().isIn(ItemTags.SWORDS);
        *///?} else {
        return item instanceof SwordItem;
         //?}
    }

}
