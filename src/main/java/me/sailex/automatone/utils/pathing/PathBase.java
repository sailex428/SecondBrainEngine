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

package me.sailex.automatone.utils.pathing;

import me.sailex.automatone.api.Settings;
import me.sailex.automatone.api.pathing.calc.IPath;
import me.sailex.automatone.api.pathing.goals.Goal;
import me.sailex.automatone.pathing.path.CutoffPath;
import me.sailex.automatone.utils.BlockStateInterface;
import net.minecraft.util.math.BlockPos;

public abstract class PathBase implements IPath {

    public PathBase cutoffAtLoadedChunks(BlockStateInterface bsi, Settings settings) {
        if (!settings.cutoffAtLoadBoundary.get()) {
            return this;
        }
        for (int i = 0; i < positions().size(); i++) {
            BlockPos pos = positions().get(i);
            if (!bsi.worldContainsLoadedChunk(pos.getX(), pos.getZ())) {
                return new CutoffPath(this, i);
            }
        }
        return this;
    }

    public PathBase staticCutoff(Goal destination, Settings settings) {
        int minLength = settings.pathCutoffMinimumLength.get();
        double cutoffFactor = settings.pathCutoffFactor.get();

        if (length() < minLength) {
            return this;
        }

        if (destination == null || destination.isInGoal(getDest())) {
            return this;
        }

        int newLength = (int) ((length() - minLength) * cutoffFactor) + minLength - 1;
        return new CutoffPath(this, newLength);
    }
}
