package adris.altoclef.multiversion;

import net.minecraft.item.FoodComponent;

public class FoodComponentWrapper {
   private final FoodComponent component;

   public static FoodComponentWrapper of(FoodComponent component) {
      return component == null ? null : new FoodComponentWrapper(component);
   }

   private FoodComponentWrapper(FoodComponent component) {
      this.component = component;
   }

   public int getHunger() {
      return this.component.getHunger();
   }

   public float getSaturationModifier() {
      return this.component.getSaturationModifier();
   }
}
