package me.sailex.altoclef.multiversion;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;

import java.util.Optional;

public class EnchantmentVer {

    //? >=1.21 {
    /*public static int getEnchantmentLevel(RegistryKey<Enchantment> enchantment, ItemStack stack, MinecraftServer server) {
        Registry<Enchantment> enchantmentRegistry = server.getRegistryManager().get(RegistryKeys.ENCHANTMENT);
        Optional<RegistryEntry.Reference<Enchantment>> entry = enchantmentRegistry.getEntry(enchantment.getValue());
        return EnchantmentHelper.getLevel(entry.get(), stack);
    }
    *///?}

}
