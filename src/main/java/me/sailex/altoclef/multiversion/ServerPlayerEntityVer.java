package me.sailex.altoclef.multiversion;

import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

public class ServerPlayerEntityVer {

    public static ItemEntity dropStack(ServerPlayerEntity player, ItemStack stack, int amount) {
        //? >=1.21.8 {
        /*return player.dropStack(player.getWorld(), stack, amount);
        *///?} else {
        return player.dropStack(stack, amount);
         //?}
    }

    public static ItemEntity dropStack(ServerPlayerEntity player, ItemStack stack) {
        //? >=1.21.8 {
        /*return player.dropStack(player.getWorld(), stack, 1);
        *///?} else {
        return player.dropStack(stack);
         //?}
    }

}
