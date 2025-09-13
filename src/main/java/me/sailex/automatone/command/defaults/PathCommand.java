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

package me.sailex.automatone.command.defaults;

import me.sailex.automatone.api.IBaritone;
import me.sailex.automatone.api.command.Command;
import me.sailex.automatone.api.command.argument.IArgConsumer;
import me.sailex.automatone.api.command.exception.CommandException;
import me.sailex.automatone.api.process.ICustomGoalProcess;
import net.minecraft.server.command.ServerCommandSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class PathCommand extends Command {

    public PathCommand() {
        super("path");
    }

    @Override
    public void execute(ServerCommandSource source, String label, IArgConsumer args, IBaritone baritone) throws CommandException {
        ICustomGoalProcess customGoalProcess = baritone.getCustomGoalProcess();
        args.requireMax(0);
        customGoalProcess.path();
        logDirect(source, "Now pathing");
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Start heading towards the goal";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "The path command makes the targeted entity head towards the current goal.",
                "",
                "Usage:",
                "> path - Start the pathing."
        );
    }
}
