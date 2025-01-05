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

/*
 * Copyright (C) 2025 sailex428
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses>.
 *
 * Linking this mod statically or dynamically with other
 * modules is making a combined work based on this mod.
 * Thus, the terms and conditions of the GNU General Public License cover the whole combination.
 *
 * In addition, as a special exception, the copyright holders of
 * this mod give you permission to combine this mod
 * with free software programs or libraries that are released under the GNU LGPL
 * and with code included in the standard release of Minecraft under All Rights Reserved (or
 * modified versions of such code, with unchanged license).
 * You may copy and distribute such a system following the terms of the GNU GPL for this mod
 * and the licenses of the other code concerned.
 *
 * Note that people who make modified versions of this mod are not obligated to grant
 * this special exception for their modified versions; it is their choice whether to do so.
 * The GNU General Public License gives permission to release a modified version without this exception;
 * this exception also makes it possible to release a modified version which carries forward this exception.
 */
package baritone.api.npc;

import com.google.common.base.Preconditions;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

public class NPCServerPlayerEntity extends ServerPlayerEntity implements AutomatoneNPC {

    private boolean release;

    public NPCServerPlayerEntity(ServerWorld world, ConnectedClientData connectedClientData) {
        super(world.getServer(), world, connectedClientData.gameProfile(), connectedClientData.syncedOptions());
        // Side effects go brr
        server.getPlayerManager().onPlayerConnect(new ClientConnection(NetworkSide.SERVERBOUND), this, connectedClientData);
        //new ServerPlayNetworkHandler(world.getServer(), new ClientConnection(NetworkSide.S2C), this, connectedClientData);
    }

    public void selectHotbarSlot(int hotbarSlot) {
        Preconditions.checkArgument(PlayerInventory.isValidHotbarIndex(hotbarSlot));
        if (this.getInventory().selectedSlot != hotbarSlot && this.getActiveHand() == Hand.MAIN_HAND) {
            this.clearActiveItem();
        }

        this.getInventory().selectedSlot = hotbarSlot;
        this.updateLastActionTime();
    }

    public void swapHands() {
        ItemStack offhandStack = this.getStackInHand(Hand.OFF_HAND);
        this.setStackInHand(Hand.OFF_HAND, this.getStackInHand(Hand.MAIN_HAND));
        this.setStackInHand(Hand.MAIN_HAND, offhandStack);
        this.clearActiveItem();
    }

    /**
     * Calls {@link #clearActiveItem()} at the end of the tick if nothing re-activated it
     */
    public void releaseActiveItem() {
        this.release = true;
    }

    public void useItem(Hand hand) {
        if (this.release && hand != this.getActiveHand()) {
            this.clearActiveItem();
        }

        if (this.isUsingItem()) return;

        ItemStack stack = this.getStackInHand(hand);

        if (!stack.isEmpty()) {
            ActionResult actionResult = this.interactionManager.interactItem(
                this,
                this.getWorld(),
                stack,
                hand
            );

            if (actionResult.shouldSwingHand()) {
                this.swingHand(hand, true);
            }
        }
    }

    @Override
    public void tick() {
        this.closeHandledScreen();
        super.tick();
        this.playerTick();
    }

    @Override
    public void tickMovement() {
        if (this.isTouchingWater() && this.isSneaking() && this.shouldSwimInFluids()) {
            // Mirrors ClientPlayerEntity's sinking behaviour
            this.knockDownwards();
        }
        super.tickMovement();
    }

    @Override
    protected void tickNewAi() {
        super.tickNewAi();
        if (this.release) {
            this.clearActiveItem();
            this.release = false;
        }
    }

    @Override
    public boolean tryAttack(Entity target) {
        this.attack(target);
        return false;
    }

    @Override
    public void takeKnockback(double strength, double x, double z) {
        if (this.velocityModified) {
            super.takeKnockback(strength, x, z);
        }
    }

    @Override
    protected void fall(double heightDifference, boolean onGround, BlockState landedState, BlockPos landedPosition) {
        this.handleFall(0, heightDifference, 0, onGround);
    }

    @Override
    public boolean canResetTimeBySleeping() {
        // NPCs do not delay the sleep of other players
        return true;
    }

    /**
     * Controls whether this should be considered a player for ticking and tracking purposes
     *
     * <p>We want NPCs to behave like regular entities, so for once we pretend they are not players.
     */
    @Override
    public boolean isPlayer() {
        return false;
    }
}
