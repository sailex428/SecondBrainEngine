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

package baritone.command.defaults;

import baritone.api.IBaritone;
import baritone.api.command.Command;
import baritone.api.command.argument.IArgConsumer;
import baritone.api.command.exception.CommandException;
import baritone.api.command.exception.CommandNotFoundException;
import baritone.command.CommandUnhandledException;
import carpet.patches.EntityPlayerMPFake;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameMode;

import java.util.List;
import java.util.stream.Stream;

public class SpawnCommand extends Command {

    public SpawnCommand() {
        super("spawn");
    }

    @Override
    public void execute(ServerCommandSource source, String label, IArgConsumer args, IBaritone baritone) throws CommandException {
        args.requireMax(1);
        String playerName = args.getAsOrNull(String.class);

        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            throw new CommandNotFoundException("Spawn command must be executed with a Player");
        }
        boolean couldSpawn = EntityPlayerMPFake.createFake(playerName, source.getServer(),
                player.getPos(), player.getYaw(), player.getPitch(),
                player.getWorld().getRegistryKey(),
                GameMode.SURVIVAL, false);
        if (!couldSpawn) {
            throw new CommandUnhandledException(playerName + "doesn't exist and cannot spawn in online mode.");
        }
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Spawn a player";
    }

    @Override
    public List<String> getLongDesc() {
        return List.of( "Spawn a fake player",
                "",
                "Usage:",
                "> spawn <playername>"
        );
    }
}
