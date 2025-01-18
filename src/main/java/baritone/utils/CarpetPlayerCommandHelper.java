/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.utils;

import baritone.Baritone;
import baritone.api.IBaritone;
import baritone.api.utils.ICommandHelper;
import baritone.api.utils.MoveDirection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Direction;

public class CarpetPlayerCommandHelper implements ICommandHelper {

    private final IBaritone baritone;
    private final CommandManager commandManager;
    private final ServerCommandSource commandSource;
    private String playerCommand;

    public CarpetPlayerCommandHelper(Baritone baritone, ServerPlayerEntity player) {
        this.baritone = baritone;
        MinecraftServer server = player.getServer();
        this.commandManager = server.getCommandManager();
        this.commandSource = server.getCommandSource();
    }

    private void buildPlayerCommand() {
        buildPlayerCommand(baritone.getPlayerContext().entity().getGameProfile().getName());
    }

    private void buildPlayerCommand(String playerName) {
        this.playerCommand = "player " + playerName;
    }

    @Override
    public void executeMove(MoveDirection moveDirection) {
        buildPlayerCommand();
        commandManager.executeWithPrefix(commandSource, playerCommand + " move " + moveDirection.toString().toLowerCase());
    }

    @Override
    public void executeMoveStop() {
        buildPlayerCommand();
        commandManager.executeWithPrefix(commandSource, playerCommand + " move");
    }

    @Override
    public void executeSpawn(String username) {
        buildPlayerCommand(username);
        commandManager.executeWithPrefix(commandSource, playerCommand + " spawn");
    }

    @Override
    public void executeLook(Direction lookDirection) {
        buildPlayerCommand();
        commandManager.executeWithPrefix(commandSource, playerCommand + " look " + lookDirection.name().toLowerCase());
    }

    @Override
    public void executeSneak() {
        buildPlayerCommand();
        commandManager.executeWithPrefix(commandSource, playerCommand + " sneak");
    }

    @Override
    public void executeUnSneak() {
        buildPlayerCommand();
        commandManager.executeWithPrefix(commandSource, playerCommand + " unsneak");
    }

    @Override
    public void executeJump() {
        buildPlayerCommand();
        commandManager.executeWithPrefix(commandSource, playerCommand + " jump");
    }

    @Override
    public void executeAttack() {
        buildPlayerCommand();
        commandManager.executeWithPrefix(commandSource, playerCommand + " attack");
    }

    @Override
    public void executeDrop(int slot) {
        buildPlayerCommand();
        commandManager.executeWithPrefix(commandSource, playerCommand + " drop slot " + slot);
    }

    @Override
    public void executeDropStack() {
        buildPlayerCommand();
        commandManager.executeWithPrefix(commandSource, playerCommand + " dropStack");
    }

    @Override
    public void executeUse() {
        buildPlayerCommand();
        commandManager.executeWithPrefix(commandSource, playerCommand + " use");
    }
}
