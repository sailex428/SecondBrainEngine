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

package me.sailex.automatone.api;

import me.sailex.automatone.api.behavior.ILookBehavior;
import me.sailex.automatone.api.behavior.IPathingBehavior;
import me.sailex.automatone.api.cache.IWorldProvider;
import me.sailex.automatone.api.command.manager.ICommandManager;
import me.sailex.automatone.api.event.listener.IEventBus;
import me.sailex.automatone.api.pathing.calc.IPathingControlManager;
import me.sailex.automatone.api.process.IBaritoneProcess;
import me.sailex.automatone.api.process.IBuilderProcess;
import me.sailex.automatone.api.process.ICustomGoalProcess;
import me.sailex.automatone.api.process.IExploreProcess;
import me.sailex.automatone.api.process.IFarmProcess;
import me.sailex.automatone.api.process.IFollowProcess;
import me.sailex.automatone.api.process.IGetToBlockProcess;
import me.sailex.automatone.api.process.IMineProcess;
import me.sailex.automatone.api.selection.ISelectionManager;
import me.sailex.automatone.api.utils.ICommandHelper;
import me.sailex.automatone.api.utils.IEntityContext;
import me.sailex.automatone.api.utils.IInputOverrideHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * @author Brady
 * @since 9/29/2018
 */
public interface IBaritone {

    /**
     * @return The {@link IPathingBehavior} instance
     * @see IPathingBehavior
     */
    IPathingBehavior getPathingBehavior();

    /**
     * @return The {@link ILookBehavior} instance
     * @see ILookBehavior
     */
    ILookBehavior getLookBehavior();

    /**
     * @return The {@link IFollowProcess} instance
     * @see IFollowProcess
     */
    IFollowProcess getFollowProcess();

    /**
     * @return The {@link IMineProcess} instance
     * @see IMineProcess
     */
    IMineProcess getMineProcess();

    /**
     * @return The {@link IBuilderProcess} instance
     * @see IBuilderProcess
     */
    IBuilderProcess getBuilderProcess();

    /**
     * @return The {@link IExploreProcess} instance
     * @see IExploreProcess
     */
    IExploreProcess getExploreProcess();

    /**
     * @return The {@link IFarmProcess} instance
     * @see IFarmProcess
     */
    IFarmProcess getFarmProcess();

    /**
     * @return The {@link ICustomGoalProcess} instance
     * @see ICustomGoalProcess
     */
    ICustomGoalProcess getCustomGoalProcess();

    /**
     * @return The {@link IGetToBlockProcess} instance
     * @see IGetToBlockProcess
     */
    IGetToBlockProcess getGetToBlockProcess();

    /**
     * @return The {@link IWorldProvider} instance
     * @see IWorldProvider
     */
    IWorldProvider getWorldProvider();

    ISelectionManager getSelectionManager();

    /**
     * Returns the {@link IPathingControlManager} for this {@link IBaritone} instance, which is responsible
     * for managing the {@link IBaritoneProcess}es which control the {@link IPathingBehavior} state.
     *
     * @return The {@link IPathingControlManager} instance
     * @see IPathingControlManager
     */
    IPathingControlManager getPathingControlManager();

    /**
     * @return The {@link IInputOverrideHandler} instance
     * @see IInputOverrideHandler
     */
    IInputOverrideHandler getInputOverrideHandler();

    /**
     * @return The {@link IEntityContext} instance
     * @see IEntityContext
     */
    IEntityContext getPlayerContext();

    /**
     * @return The {@link IEventBus} instance
     * @see IEventBus
     */
    IEventBus getGameEventHandler();

    /**
     * @return The {@link ICommandManager} instance
     * @see ICommandManager
     */
    ICommandManager getCommandManager();

    /**
     * Send a message to chat only if chatDebug is on
     *
     * @param message The message to display in chat
     */
    void logDebug(String message);

    /**
     * Send components to chat with the [Automatone] prefix
     *
     * @param components The components to send
     */
    default void logDirect(Text... components) {
        IEntityContext playerContext = this.getPlayerContext();
        LivingEntity entity = playerContext.entity();
        if (entity instanceof PlayerEntity) {
            MutableText component = Text.literal("");
            // If we are not logging as a Toast
            // Append the prefix to the base component line
            component.append(BaritoneAPI.getPrefix());
            component.append(Text.literal(" "));
            Arrays.asList(components).forEach(component::append);
            ((PlayerEntity) entity).sendMessage(component, false);
        }
    }

    /**
     * Send a message to chat regardless of chatDebug (should only be used for critically important messages, or as a
     * direct response to a chat command)
     *
     * @param message The message to display in chat
     * @param color   The color to print that message in
     */
    default void logDirect(String message, Formatting color) {
        Stream.of(message.split("\n")).forEach(line -> {
            MutableText component = Text.literal(line.replace("\t", "    "));
            component.setStyle(component.getStyle().withFormatting(color));
            logDirect(component);
        });
    }

    /**
     * Send a message to chat regardless of chatDebug (should only be used for critically important messages, or as a
     * direct response to a chat command)
     *
     * @param message The message to display in chat
     */
    default void logDirect(String message) {
        logDirect(message, Formatting.GRAY);
    }

    boolean isActive();

    Settings settings();

    ICommandHelper getCommandHelper();

    void serverTick();
}
