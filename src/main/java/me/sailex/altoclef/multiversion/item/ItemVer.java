package me.sailex.altoclef.multiversion.item;

import me.sailex.altoclef.multiversion.FoodComponentWrapper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

//? >=1.21 {
/*import net.minecraft.component.DataComponentTypes;
*///?}

public class ItemVer {
   public static FoodComponentWrapper getFoodComponent(Item item) {
       //? >=1.21 {
       /*return FoodComponentWrapper.of(item.getComponents().get(DataComponentTypes.FOOD));
       *///?} elif >= 1.20 {
       return FoodComponentWrapper.of(item.getFoodComponent());
       //?}
   }

   public static boolean isFood(ItemStack stack) {
      return isFood(stack.getItem());
   }

   public static boolean hasCustomName(ItemStack stack) {
       //? >=1.21 {
       /*return stack.get(DataComponentTypes.CUSTOM_NAME) != null;
       *///?} elif >= 1.20 {
       return stack.hasCustomName();
       //?}
   }

   public static boolean isFood(Item item) {
       //? >=1.21 {
       /*return FoodComponentWrapper.of(item.getComponents().get(DataComponentTypes.FOOD)) != null;
       *///?} elif >= 1.20 {
       return item.isFood();
       //?}
   }

//   private static boolean isSuitableFor(Item item, BlockState state) {
//      return item.isSuitableFor(state);
//   }

   private static Item RAW_GOLD() {
      return Items.RAW_GOLD;
   }

   private static Item RAW_IRON() {
      return Items.RAW_IRON;
   }
}
