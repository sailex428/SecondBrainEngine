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

package baritone.api;

import baritone.api.cache.IWorldScanner;
import baritone.api.command.ICommand;
import baritone.api.command.ICommandSystem;
import baritone.api.schematic.ISchematicSystem;
import net.minecraft.server.network.ServerPlayerEntity;
import org.ladysnake.cca.api.v3.component.ComponentFactory;

/**
 * Provides the present {@link IBaritone} instances, as well as non-baritone instance related APIs.
 *
 * @author leijurv
 */
public interface IBaritoneProvider {

    /**
     * Provides the {@link IBaritone} instance for a given {@link ServerPlayerEntity}.
     *
     * @param player The player
     * @return The {@link IBaritone} instance.
     */
    IBaritone getBaritone(ServerPlayerEntity player);

    /**
     * Returns the {@link IWorldScanner} instance. This is not a type returned by
     * {@link IBaritone} implementation, because it is not linked with {@link IBaritone}.
     *
     * @return The {@link IWorldScanner} instance.
     */
    IWorldScanner getWorldScanner();

    /**
     * Returns the {@link ICommandSystem} instance. This is not bound to a specific {@link IBaritone}
     * instance because {@link ICommandSystem} itself controls global behavior for {@link ICommand}s.
     *
     * @return The {@link ICommandSystem} instance.
     */
    ICommandSystem getCommandSystem();

    /**
     * @return The {@link ISchematicSystem} instance.
     */
    ISchematicSystem getSchematicSystem();

    Settings getGlobalSettings();

    <E extends ServerPlayerEntity> ComponentFactory<E, IBaritone> componentFactory();
}
