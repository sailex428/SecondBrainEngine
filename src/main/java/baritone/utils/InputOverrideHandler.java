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
import baritone.api.utils.ICommandHelper;
import baritone.api.utils.IInputOverrideHandler;
import baritone.api.utils.MoveDirection;
import baritone.api.utils.input.Input;
import baritone.behavior.Behavior;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.EnumSet;
import java.util.Set;

/**
 * An interface with the game's control system allowing the ability to
 * force down certain controls, having the same effect as if we were actually
 * physically forcing down the assigned key.
 *
 * @author Brady
 * @since 7/31/2018
 */
public final class InputOverrideHandler extends Behavior implements IInputOverrideHandler {

    /**
     * Maps inputs to whether or not we are forcing their state down.
     */
    private final Set<Input> inputForceStateMap = EnumSet.noneOf(Input.class);

    private final BlockBreakHelper blockBreakHelper;
    private final BlockPlaceHelper blockPlaceHelper;
    private final ICommandHelper commandHelper;
    private boolean needsUpdate;

    public InputOverrideHandler(Baritone baritone) {
        super(baritone);
        this.blockBreakHelper = new BlockBreakHelper(baritone.getPlayerContext());
        this.blockPlaceHelper = new BlockPlaceHelper(baritone.getPlayerContext());
        this.commandHelper = baritone.getCommandHelper();
    }

    /**
     * Returns whether or not we are forcing down the specified {@link Input}.
     *
     * @param input The input
     * @return Whether or not it is being forced down
     */
    @Override
    public final synchronized boolean isInputForcedDown(Input input) {
        return input != null && this.inputForceStateMap.contains(input);
    }

    /**
     * Sets whether or not the specified {@link Input} is being forced down.
     *
     * @param input  The {@link Input}
     * @param forced Whether or not the state is being forced
     */
    @Override
    public final synchronized void setInputForceState(Input input, boolean forced) {
        if (forced) {
            this.inputForceStateMap.add(input);
        } else {
            this.inputForceStateMap.remove(input);
        }
        this.needsUpdate = true;
    }

    /**
     * Clears the override state for all keys
     */
    @Override
    public final synchronized void clearAllKeys() {
        // Note that calling setSprinting before entity attributes are initialized will crash the game
        // PERF: entity flags use a lock, see if we can put this elsewhere to reduce the number of calls
        if (this.ctx.entity().isSprinting()) this.ctx.entity().setSprinting(false);
        this.inputForceStateMap.clear();
        this.needsUpdate = true;
    }

    @Override
    public final void onTickServer() {
        if (!this.needsUpdate) return;

        if (isInputForcedDown(Input.CLICK_LEFT)) {
            setInputForceState(Input.CLICK_RIGHT, false);
        }

        ServerPlayerEntity entity = this.ctx.entity();
        entity.sidewaysSpeed = 0.0F;
        entity.forwardSpeed = 0.0F;
        entity.setSneaking(false);
        commandHelper.executeMoveStop();
        commandHelper.executeUnSneak();

        if (this.isInputForcedDown(Input.JUMP)) {
//            entity.setJumping(this.isInputForcedDown(Input.JUMP)); // oppa gangnam style
            commandHelper.executeJump();
        }

        if (this.isInputForcedDown(Input.MOVE_FORWARD)) {
            entity.forwardSpeed++;
            commandHelper.executeMove(MoveDirection.FORWARD);
        }

        if (this.isInputForcedDown(Input.MOVE_BACK)) {
            //entity.forwardSpeed--;
            commandHelper.executeMove(MoveDirection.BACKWARD);
        }

        if (this.isInputForcedDown(Input.MOVE_LEFT)) {
            //entity.sidewaysSpeed++;
            commandHelper.executeMove(MoveDirection.LEFT);
        }

        if (this.isInputForcedDown(Input.MOVE_RIGHT)) {
            //entity.sidewaysSpeed--;
            commandHelper.executeMove(MoveDirection.RIGHT);
        }

        if (this.isInputForcedDown(Input.SNEAK)) {
//            entity.setSneaking(true);
//            entity.sidewaysSpeed *= 0.3D;
//            entity.forwardSpeed *= 0.3D;
            commandHelper.executeSneak();
        }

        if (entity.isTouchingWater()) {
            Vec3d vec3d = entity.getVelocity();
            entity.setVelocity(vec3d.x, 0.25F, vec3d.z);
        }

        blockBreakHelper.tick(isInputForcedDown(Input.CLICK_LEFT));
        blockPlaceHelper.tick(isInputForcedDown(Input.CLICK_RIGHT));

        this.needsUpdate = false;
    }

    public BlockBreakHelper getBlockBreakHelper() {
        return blockBreakHelper;
    }
}
