package me.sailex.altoclef.multiversion;

//? >=1.21.8 {
/*import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ToolComponent;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.item.ItemStack;
*///?} else {
import net.minecraft.item.ToolItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.ToolMaterials;
 //?}

import net.minecraft.item.Item;

public class ToolMaterialVer {
    //? >=1.21.8 {
    /*private static int getMineableBlocks(Item item){
        for(ToolComponent.Rule rule : item.getComponents().get(DataComponentTypes.TOOL).rules()){
            if(rule.correctForDrops().isPresent() && rule.correctForDrops().get()){
                return rule.blocks().size();
            }
        }
        return RegistryEntryList.empty().size();
    }

    public static int getMiningLevel(ItemStack item) {
        return getMineableBlocks(item.getItem());
    }

    public static int getMiningLevel(Item material) {
        return getMineableBlocks(material);
    }
    *///?} else {
    
   public static int getMiningLevel(Item item) {
      return getMiningLevel(((ToolItem) item).getMaterial());
   }

   public static int getMiningLevel(ToolMaterial material) {
      if (material.equals(ToolMaterials.WOOD) || material.equals(ToolMaterials.GOLD)) {
         return 0;
      } else if (material.equals(ToolMaterials.STONE)) {
         return 1;
      } else if (material.equals(ToolMaterials.IRON)) {
         return 2;
      } else if (material.equals(ToolMaterials.DIAMOND)) {
         return 3;
      } else if (material.equals(ToolMaterials.NETHERITE)) {
         return 4;
      } else {
         throw new IllegalStateException("Unexpected value: " + material);
      }
   }
    //?}
}
