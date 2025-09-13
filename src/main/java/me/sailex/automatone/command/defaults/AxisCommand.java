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
import me.sailex.automatone.api.pathing.goals.Goal;
import me.sailex.automatone.api.pathing.goals.GoalAxis;
import net.minecraft.server.command.ServerCommandSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class AxisCommand extends Command {

    public AxisCommand() {
        super("axis", "highway");
    }

    @Override
    public void execute(ServerCommandSource source, String label, IArgConsumer args, IBaritone baritone) throws CommandException {
        args.requireMax(0);
        Goal goal = new GoalAxis(baritone.settings().axisHeight.get());
        baritone.getCustomGoalProcess().setGoal(goal);
        logDirect(source, String.format("Goal: %s", goal.toString()));
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) {
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Set a goal to the axes";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "The axis command sets a goal that makes an entity head towards the nearest axis. That is, X=0 or Z=0.",
                "",
                "Usage:",
                "> axis"
        );
    }
}
