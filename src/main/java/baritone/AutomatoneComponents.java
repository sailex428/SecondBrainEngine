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

package baritone;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.cache.IWorldProvider;
import baritone.api.selection.ISelectionManager;
import baritone.api.utils.InteractionController;
import baritone.cache.WorldProvider;
import baritone.selection.SelectionManager;
import baritone.utils.player.PlayerEntityInteractionController;
import net.minecraft.server.network.ServerPlayerEntity;

//@KeepName
//public final class AutomatoneComponents implements EntityComponentInitializer, WorldComponentInitializer {
//    @Override
//    public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
//        registry.registerFor(ServerPlayerEntity.class, IBaritone.KEY, BaritoneAPI.getProvider().componentFactory());
//        registry.registerFor(ServerPlayerEntity.class, ISelectionManager.KEY, SelectionManager::new);
//        registry.registerFor(ServerPlayerEntity.class, InteractionController.KEY, PlayerEntityInteractionController::new);
//    }
//
//    @Override
//    public void registerWorldComponentFactories(WorldComponentFactoryRegistry registry) {
//        registry.register(IWorldProvider.KEY, WorldProvider::new);
//    }
//}
