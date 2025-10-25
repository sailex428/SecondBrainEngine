package me.sailex.altoclef.multiversion.recipemanager;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.entry.RegistryEntry;

import java.util.Arrays;
import java.util.List;

public class CraftingRecipeVer {

    public static ItemStack getResult(CraftingRecipe recipe) {
        //? if >=1.21 {
        /*return recipe.craft(null, null);
        *///?} elif >=1.21 {
        /*
        return recipe.getResult(null);
        *///?} elif >= 1.20 {
        
        return recipe.getOutput(null);
        //?}
    }

    public static List<Ingredient> getIngredients(CraftingRecipe recipe) {
        //? >=1.21.8 {
        /*return recipe.getIngredientPlacement().getIngredients();
        *///?} else {
        
        return recipe.getIngredients();
         //?}
    }

    public static Item[] getMatchingStacks(Ingredient ingredient) {
        //? >=1.21.8 {
        /*return ingredient.getMatchingItems().map(RegistryEntry::value).distinct().toArray(Item[]::new);
        *///?} else {
        
        return Arrays.stream(ingredient.getMatchingStacks()).map(ItemStack::getItem).toArray(Item[]::new);
         //?}

    }

}
