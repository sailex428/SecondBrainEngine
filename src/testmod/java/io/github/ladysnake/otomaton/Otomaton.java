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

package io.github.ladysnake.otomaton;

import baritone.api.npc.NPCServerPlayerEntity;
import com.mojang.authlib.GameProfile;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.server.network.ConnectedClientData;

import java.util.UUID;

public class Otomaton implements ModInitializer {

    private boolean isInitialized = false;

    @Override
    public void onInitialize() {
        ServerWorldEvents.LOAD.register((server, world)-> {
            if (!isInitialized) {
                NPCServerPlayerEntity playerEntity = new NPCServerPlayerEntity(server.getOverworld(),
                        ConnectedClientData.createDefault(new GameProfile(UUID.randomUUID(), "lolli"), false));
                isInitialized = true;
                playerEntity.connectToServer();
            }
        });
    }
}
