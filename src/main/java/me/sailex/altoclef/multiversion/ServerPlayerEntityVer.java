package me.sailex.altoclef.multiversion;

import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

public class ServerPlayerEntityVer {

    public static ItemEntity dropStack(ServerPlayerEntity player, ItemStack stack, int amount) {
        //? >=1.21.8 {
        /*return player.dropStack(getWorld(player), stack, amount);
        *///?} else {
        return player.dropStack(stack, amount);
         //?}
    }

    public static ItemEntity dropStack(ServerPlayerEntity player, ItemStack stack) {
        //? >=1.21.8 {
        /*return player.dropStack(getWorld(player), stack, 1);
        *///?} else {
        return player.dropStack(stack);
         //?}
    }

    public static ServerWorld getWorld(ServerPlayerEntity player) {
        //? >=1.21.10 {
        /*return player.getEntityWorld();
        *///?} else {
        return player.getServerWorld();
         //?}
    }

}
