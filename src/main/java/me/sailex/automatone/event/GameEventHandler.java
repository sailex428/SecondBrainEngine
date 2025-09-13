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

package me.sailex.automatone.event;

import me.sailex.automatone.Automatone;
import me.sailex.automatone.Baritone;
import me.sailex.automatone.api.event.events.BlockInteractEvent;
import me.sailex.automatone.api.event.events.PathEvent;
import me.sailex.automatone.api.event.listener.IEventBus;
import me.sailex.automatone.api.event.listener.IGameEventListener;
import me.sailex.automatone.utils.BlockStateInterface;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Brady
 * @since 7/31/2018
 */
public final class GameEventHandler implements IEventBus {

    private final Baritone baritone;

    private final List<IGameEventListener> listeners = new CopyOnWriteArrayList<>();

    public GameEventHandler(Baritone baritone) {
        this.baritone = baritone;
    }

    @Override
    public void onTickServer() {
        try {
            baritone.bsi = new BlockStateInterface(baritone.getPlayerContext());
        } catch (Exception ex) {
            Automatone.LOGGER.error(ex);
            baritone.bsi = null;
        }
        listeners.forEach(IGameEventListener::onTickServer);
    }

    @Override
    public void onBlockInteract(BlockInteractEvent event) {
        listeners.forEach(l -> l.onBlockInteract(event));
    }

    @Override
    public void onPathEvent(PathEvent event) {
        listeners.forEach(l -> l.onPathEvent(event));
    }

    @Override
    public final void registerEventListener(IGameEventListener listener) {
        this.listeners.add(listener);
    }
}
