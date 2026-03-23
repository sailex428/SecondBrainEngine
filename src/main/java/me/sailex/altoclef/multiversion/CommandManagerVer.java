package me.sailex.altoclef.multiversion;

import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public class CommandManagerVer {

    public static void executeWithPrefix(CommandManager commandManager, ServerCommandSource source, String command) {
        //? >=1.21.11 {
        /*commandManager.parseAndExecute(source, command);
        *///?} else {
        commandManager.executeWithPrefix(source, command);
        //?}
    }
}
