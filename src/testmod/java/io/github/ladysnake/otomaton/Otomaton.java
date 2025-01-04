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

import net.fabricmc.api.ModInitializer;

public class Otomaton implements ModInitializer {
//    public static final String MOD_ID = "otomaton";
//
//    public static Identifier id(String path) {
//        return Identifier.of(MOD_ID, path);
//    }
//
//    public static final EntityType<PlayerEntity> FAKE_PLAYER = FabricEntityTypeBuilder.<PlayerEntity>createLiving()
//            .spawnGroup(SpawnGroup.MISC)
//            .entityFactory(NPCs.entityFactory(NPCServerPlayerEntity::new))
//            .defaultAttributes(NPCServerPlayerEntity::createPlayerAttributes)
//            .dimensions(EntityDimensions.changing(EntityType.PLAYER.getWidth(), EntityType.PLAYER.getHeight()))
//            .trackRangeBlocks(64)
//            .trackedUpdateRate(1)
//            .forceTrackedVelocityUpdates(true)
//            .build();

    @Override
    public void onInitialize() {
        //Registry.register(Registries.ENTITY_TYPE, id("fake_player"), FAKE_PLAYER);
    }
}
