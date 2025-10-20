package me.sailex.altoclef.multiversion;

//? >=1.21 {
/*import net.minecraft.component.type.FoodComponent;
*///?} elif >= 1.20 {
import net.minecraft.item.FoodComponent;
//?}


public class FoodComponentWrapper {
   private final FoodComponent component;

   public static FoodComponentWrapper of(FoodComponent component) {
      return component == null ? null : new FoodComponentWrapper(component);
   }

   private FoodComponentWrapper(FoodComponent component) {
      this.component = component;
   }

   public int getHunger() {
       //? >=1.21 {
       /*return this.component.nutrition();
        *///?} elif >= 1.20 {
       return this.component.getHunger();
        //?}
   }

   public float getSaturationModifier() {
       //? >=1.21 {
       /*return component.nutrition() == 0 ? 0.0f : component.saturation() / (component.nutrition() * 2.0f);
       *///?} elif >= 1.20 {
       return this.component.getSaturationModifier();
       //?}
   }
}
