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

import me.sailex.automatone.api.pathing.movement.ActionCosts;

import javax.annotation.Nonnegative;

/**
 * The result of a calculated movement, with destination x, y, z, and the cost of performing the movement
 *
 * @author leijurv
 */
public final class MutableMoveResult {

    public int x;
    public int y;
    public int z;
    @Nonnegative
    public double cost;
    /**
     * 0 means the player can breathe throughout the whole movement
     */
    @Nonnegative
    public double oxygenCost;

    public MutableMoveResult() {
        reset();
    }

    public final void reset() {
        x = 0;
        y = 0;
        z = 0;
        cost = ActionCosts.COST_INF;    // movements are assumed to be impossible until proven otherwise
        oxygenCost = 0; // movements are assumed to be done in air until proven otherwise
    }
}
