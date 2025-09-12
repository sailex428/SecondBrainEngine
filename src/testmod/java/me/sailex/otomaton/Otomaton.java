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

package me.sailex.otomaton;

import adris.altoclef.AltoClefController;
import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import com.mojang.authlib.GameProfile;
import common.NPCSpawner;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import java.util.UUID;

public class Otomaton implements ModInitializer {

    private AltoClefController controller;
    private boolean init = false;

    @Override
    public void onInitialize() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {

            if (!init) {
                init = true;
                NPCSpawner.spawn(new GameProfile(UUID.randomUUID(), "minusaura"), server, handler.player.getBlockPos(), npc -> {
                    IBaritone automatone = BaritoneAPI.getProvider().getBaritone(npc);
                    this.controller = new AltoClefController(automatone);
                    this.controller.setOwner(handler.player);
                });
            }
        });
    }
}
