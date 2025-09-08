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

package baritone.api.pathing.goals;

import baritone.api.BaritoneAPI;

public class GoalAxis implements Goal {

    private static final double SQRT_2_OVER_2 = Math.sqrt(2) / 2;
    private final int targetHeight;

    public GoalAxis(int targetHeight) {
        this.targetHeight = targetHeight;
    }

    @Override
    public boolean isInGoal(int x, int y, int z) {
        return y == targetHeight && (x == 0 || z == 0 || Math.abs(x) == Math.abs(z));
    }

    @Override
    public double heuristic(int x0, int y, int z0) {
        int x = Math.abs(x0);
        int z = Math.abs(z0);

        int shrt = Math.min(x, z);
        int lng = Math.max(x, z);
        int diff = lng - shrt;

        double flatAxisDistance = Math.min(x, Math.min(z, diff * SQRT_2_OVER_2));

        return flatAxisDistance * BaritoneAPI.getGlobalSettings().costHeuristic.get() + GoalYLevel.calculate(BaritoneAPI.getGlobalSettings().axisHeight.get(), y);
    }

    @Override
    public String toString() {
        return "GoalAxis";
    }
}
