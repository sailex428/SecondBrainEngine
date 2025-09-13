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
import me.sailex.automatone.api.pathing.goals.GoalStrictDirection;
import me.sailex.automatone.api.utils.IEntityContext;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class TunnelCommand extends Command {

    public TunnelCommand() {
        super("tunnel");
    }

    @Override
    public void execute(ServerCommandSource source, String label, IArgConsumer args, IBaritone baritone) throws CommandException {
        args.requireMax(3);
        IEntityContext ctx = baritone.getPlayerContext();

        if (args.hasExactly(3)) {
            boolean cont = true;
            int height = Integer.parseInt(args.getArgs().get(0).getValue());
            int width = Integer.parseInt(args.getArgs().get(1).getValue());
            int depth = Integer.parseInt(args.getArgs().get(2).getValue());

            if (width < 1 || height < 2 || depth < 1 || height > 255) {
                logDirect(source, "Width and depth must at least be 1 block; Height must at least be 2 blocks, and cannot be greater than the build limit.");
                cont = false;
            }

            if (cont) {
                height--;
                width--;
                BlockPos corner1;
                BlockPos corner2;
                Direction enumFacing = ctx.entity().getHorizontalFacing();
                int addition = ((width % 2 == 0) ? 0 : 1);
                switch (enumFacing) {
                    case EAST:
                        corner1 = new BlockPos(ctx.feetPos().x, ctx.feetPos().y, ctx.feetPos().z - width / 2);
                        corner2 = new BlockPos(ctx.feetPos().x + depth, ctx.feetPos().y + height, ctx.feetPos().z + width / 2 + addition);
                        break;
                    case WEST:
                        corner1 = new BlockPos(ctx.feetPos().x, ctx.feetPos().y, ctx.feetPos().z + width / 2 + addition);
                        corner2 = new BlockPos(ctx.feetPos().x - depth, ctx.feetPos().y + height, ctx.feetPos().z - width / 2);
                        break;
                    case NORTH:
                        corner1 = new BlockPos(ctx.feetPos().x - width / 2, ctx.feetPos().y, ctx.feetPos().z);
                        corner2 = new BlockPos(ctx.feetPos().x + width / 2 + addition, ctx.feetPos().y + height, ctx.feetPos().z - depth);
                        break;
                    case SOUTH:
                        corner1 = new BlockPos(ctx.feetPos().x + width / 2 + addition, ctx.feetPos().y, ctx.feetPos().z);
                        corner2 = new BlockPos(ctx.feetPos().x - width / 2, ctx.feetPos().y + height, ctx.feetPos().z + depth);
                        break;
                    default:
                        throw new IllegalStateException("Unexpected value: " + enumFacing);
                }
                logDirect(source, String.format("Creating a tunnel %s block(s) high, %s block(s) wide, and %s block(s) deep", height + 1, width + 1, depth));
                baritone.getBuilderProcess().clearArea(corner1, corner2);
            }
        } else {
            Goal goal = new GoalStrictDirection(
                    ctx.feetPos(),
                    ctx.entity().getHorizontalFacing()
            );
            baritone.getCustomGoalProcess().setGoalAndPath(goal);
            logDirect(source, String.format("Goal: %s", goal.toString()));
        }
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) {
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Set a goal to tunnel in your current direction";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "The tunnel command sets a goal that tells Automatone to mine completely straight in the direction that you're facing.",
                "",
                "Usage:",
                "> tunnel - No arguments, mines in a 1x2 radius.",
                "> tunnel <height> <width> <depth> - Tunnels in a user defined height, width and depth."
        );
    }
}
