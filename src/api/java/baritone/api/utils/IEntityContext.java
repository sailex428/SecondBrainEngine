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

package baritone.api.utils;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.cache.IWorldData;
import baritone.api.pathing.calc.Avoidance;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @author Brady
 * @since 11/12/2018
 */
public interface IEntityContext {

    ServerPlayerEntity entity();

    default IBaritone baritone() {
        return IBaritone.KEY.get(entity());
    }

    @Nullable PlayerInventory inventory();

    IPlayerController playerController();

    ServerWorld world();

    default Iterable<Entity> worldEntities() {
        return world().iterateEntities();
    }

    default Stream<Entity> worldEntitiesStream() {
        return StreamSupport.stream(worldEntities().spliterator(), false);
    }

    void setAvoidanceFinder(@Nullable Supplier<List<Avoidance>> avoidanceFinder);

    List<Avoidance> listAvoidedAreas();

    IWorldData worldData();

    HitResult objectMouseOver();

    BetterBlockPos feetPos();

    default Vec3d feetPosAsVec() {
        return new Vec3d(entity().getX(), entity().getY(), entity().getZ());
    }

    default Vec3d headPos() {
        return new Vec3d(entity().getX(), entity().getY() + entity().getStandingEyeHeight(), entity().getZ());
    }

    default Rotation entityRotations() {
        return new Rotation(entity().getYaw(), entity().getPitch());
    }

    /**
     * Returns the block that the crosshair is currently placed over. Updated once per tick.
     *
     * @return The position of the highlighted block
     */
    default Optional<BlockPos> getSelectedBlock() {
        HitResult result = objectMouseOver();
        if (result != null && result.getType() == HitResult.Type.BLOCK) {
            return Optional.of(((BlockHitResult) result).getBlockPos());
        }
        return Optional.empty();
    }

    default boolean isLookingAt(BlockPos pos) {
        return getSelectedBlock().equals(Optional.of(pos));
    }

    default void logDebug(String message) {
        if (!BaritoneAPI.getGlobalSettings().chatDebug.get()) {
            return;
        }
        LivingEntity entity = entity();
        if (entity instanceof PlayerEntity) ((PlayerEntity) entity).sendMessage(Text.literal(message).formatted(Formatting.GRAY), false);

        if (!BaritoneAPI.getGlobalSettings().syncWithOps.get()) return;

        MinecraftServer server = world().getServer();
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            if (server.getPlayerManager().isOperator(p.getGameProfile())) {
                IBaritone.KEY.get(p).logDirect(message);
            }
        }
    }
}
