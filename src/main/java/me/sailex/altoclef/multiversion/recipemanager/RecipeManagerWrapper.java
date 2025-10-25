package me.sailex.altoclef.multiversion.recipemanager;


//? >=1.21.8 {
/*import net.minecraft.recipe.ServerRecipeManager;
import net.minecraft.recipe.RecipeEntry;
*///?} else {

import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.util.Identifier;
//?}

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class RecipeManagerWrapper {

    //? >=1.21.8 {
    /*private final ServerRecipeManager recipeManager;

    public static RecipeManagerWrapper of(ServerRecipeManager recipeManager) {
        return recipeManager == null ? null : new RecipeManagerWrapper(recipeManager);
    }

    private RecipeManagerWrapper(ServerRecipeManager recipeManager) {
        this.recipeManager = recipeManager;
    }

    *///?} else {
    
    private final RecipeManager recipeManager;

    public static RecipeManagerWrapper of(RecipeManager recipeManager) {
      return recipeManager == null ? null : new RecipeManagerWrapper(recipeManager);
    }

    private RecipeManagerWrapper(RecipeManager recipeManager) {
      this.recipeManager = recipeManager;
    }
    //?}

   public Collection<WrappedRecipeEntry> values() {
      List<WrappedRecipeEntry> result = new ArrayList<>();

       //? >=1.21.8 {
       /*for (RecipeEntry<?> entry : this.recipeManager.values()) {
           result.add(new WrappedRecipeEntry(entry.id().getValue(), this.recipeManager.get(entry.id()).get().value()));
       }
       *///?} elif >=1.21 {
       /*
       for (Identifier id : this.recipeManager.keys().toList()) {
         result.add(new WrappedRecipeEntry(id, this.recipeManager.get(id).get().value()));
       }
       *///?} else {
       
       for (Identifier id : this.recipeManager.keys().toList()) {
         result.add(new WrappedRecipeEntry(id, (Recipe<?>) this.recipeManager.get(id).get()));
       }
       //?}

      return result;
   }
}
