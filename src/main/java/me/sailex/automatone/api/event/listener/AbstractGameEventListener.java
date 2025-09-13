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

package me.sailex.automatone.api.event.listener;

import me.sailex.automatone.api.event.events.BlockInteractEvent;
import me.sailex.automatone.api.event.events.PathEvent;

/**
 * An implementation of {@link IGameEventListener} that has all methods
 * overridden with empty bodies, allowing inheritors of this class to choose
 * which events they would like to listen in on.
 *
 * @author Brady
 * @see IGameEventListener
 * @since 8/1/2018
 */
public interface AbstractGameEventListener extends IGameEventListener {

    @Override
    default void onTickServer() {}

    @Override
    default void onBlockInteract(BlockInteractEvent event) {}

    @Override
    default void onPathEvent(PathEvent event) {}
}
