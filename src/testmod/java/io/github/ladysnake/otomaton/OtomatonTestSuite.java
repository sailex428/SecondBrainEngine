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

import com.mojang.authlib.GameProfile;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import java.util.UUID;

public class OtomatonTestSuite {

    @GameTest
    public void connectToServer(TestContext ctx) {
//        NPCServerPlayerEntity playerEntity = new NPCServerPlayerEntity(ctx.getWorld(), ConnectedClientData.createDefault(new GameProfile(UUID.randomUUID(), "NPC"), false));
//        playerEntity.connectToServer();
//        ctx.complete();
    }

//    @GameTest
//    public void beforeSleepingTests(TestContext ctx) {
//        ServerWorld world = ctx.getWorld();
//        world.setTimeOfDay(20000);  // set time to night
//        world.calculateAmbientDarkness(); // refreshes light info for sleeping
//        ctx.complete();
//    }

//
//    @GameTest
//    public void shellsDoNotPreventSleeping(TestContext ctx) {
//        ServerPlayerEntity player = ctx.spawnServerPlayer(1, 0, 1);
//        ServerPlayerEntity fakePlayer = new NPCServerPlayerEntity(Otomaton.FAKE_PLAYER, ctx.getWorld());
//        ctx.getWorld().spawnEntity(fakePlayer);
//
//        ItemStack bed = new ItemStack(Items.RED_BED);
//        fakePlayer.setStackInHand(Hand.MAIN_HAND, bed);
//        BlockPos bedPos = new BlockPos(1, 0, 2);
//        fakePlayer.interactionManager.interactBlock(fakePlayer, ctx.getWorld(), bed, Hand.MAIN_HAND,
//                new BlockHitResult(new Vec3d(0.5, 0.5, 0.5), Direction.UP, bedPos, false)
//        );
//        ctx.expectBlock(Blocks.RED_BED, bedPos);
//
//        player.interactionManager.interactBlock(player, ctx.getWorld(), ItemStack.EMPTY, Hand.OFF_HAND,
//                new BlockHitResult(new Vec3d(0.5, 0.5, 0.5), Direction.UP, ctx.getAbsolutePos(bedPos), false)
//        );
//        List<ServerPlayerEntity> players = List.of(fakePlayer, player);
//        SleepManager sleepManager = ((ServerWorldAccessor) player.getWorld()).requiem$getSleepManager();
//        sleepManager.update(players);
//        GameTestUtil.assertTrue("player should be sleeping", player.isSleeping());
//        GameTestUtil.assertTrue("all players should be sleeping", sleepManager.canResetTime(100, players));
//        ctx.complete();
//    }
//
//    @GameTest
//    public void shellsKeepUuidOnReload(TestContext ctx) {
//        ServerPlayerEntity fakePlayer = new NPCServerPlayerEntity(Otomaton.FAKE_PLAYER, ctx.getWorld());
//        ServerPlayerEntity fakePlayer2 = new NPCServerPlayerEntity(Otomaton.FAKE_PLAYER, ctx.getWorld());
//        NbtCompound nbtCompound = fakePlayer.writeNbt(new NbtCompound());
//        fakePlayer2.readNbt(nbtCompound);
//        GameTestUtil.assertTrue("Fake players should keep UUID after reload", fakePlayer2.getUuid().equals(fakePlayer.getUuid()));
//        ctx.complete();
//    }
//
//    @GameTes
//    public void realPlayersDoBroadcastAdvancements(TestContext ctx) {
//        ServerPlayerEntity player = ctx.spawnServerPlayer(1, 0, 1);
//        ctx.getWorld().getServer().getPlayerManager().getPlayerList().add(player);
//        Criteria.INVENTORY_CHANGED.trigger(player, player.getInventory(), new ItemStack(Items.COBBLESTONE));
//        ctx.verifyConnection(player, conn -> conn.sent(GameMessageS2CPacket.class, packet -> packet.content().getString().contains("chat.type.advancement.task")).exactly(1));
//        ctx.getWorld().getServer().getPlayerManager().getPlayerList().remove(player);
//        player.remove(Entity.RemovalReason.DISCARDED);
//        ctx.complete();
//    }
//
//    @GameTest
//    public void fakePlayersDoNotBroadcastAdvancements(TestContext ctx) {
//        ServerPlayerEntity fakePlayer = new NPCServerPlayerEntity(Otomaton.FAKE_PLAYER, ctx.getWorld());
//        fakePlayer.networkHandler = new ServerPlayNetworkHandler(ctx.getWorld().getServer(), new MockClientConnection(NetworkSide.SERVERBOUND), fakePlayer, ConnectedClientData.createDefault(fakePlayer.getGameProfile(), false));
//        fakePlayer.setPosition(ctx.getAbsolute(new Vec3d(1, 0, 1)));
//        ctx.getWorld().spawnEntity(fakePlayer);
//        // Needed for getting broadcasted messages
//        ctx.getWorld().getServer().getPlayerManager().getPlayerList().add(fakePlayer);
//        Criteria.INVENTORY_CHANGED.trigger(fakePlayer, fakePlayer.getInventory(), new ItemStack(Items.COBBLESTONE));
//        ctx.verifyConnection(fakePlayer, conn -> conn.allowNoPacketMatch(true).sent(GameMessageS2CPacket.class, packet -> packet.content().getString().contains("chat.type.advancement.task")).exactly(0));
//        ctx.getWorld().getServer().getPlayerManager().getPlayerList().remove(fakePlayer);
//        fakePlayer.remove(Entity.RemovalReason.DISCARDED);
//        ctx.complete();
//    }
}
