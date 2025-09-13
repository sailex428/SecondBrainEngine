package me.sailex.altoclef.control;

import me.sailex.altoclef.AltoClefController;
import me.sailex.altoclef.eventbus.EventBus;
import me.sailex.altoclef.eventbus.events.BlockBreakingCancelEvent;
import me.sailex.altoclef.eventbus.events.BlockBreakingEvent;
import net.minecraft.entity.Entity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

public class PlayerExtraController {
   private final AltoClefController mod;
   private BlockPos blockBreakPos;

   public PlayerExtraController(AltoClefController mod) {
      this.mod = mod;
      EventBus.subscribe(BlockBreakingEvent.class, evt -> this.onBlockBreak(evt.blockPos));
      EventBus.subscribe(BlockBreakingCancelEvent.class, evt -> this.onBlockStopBreaking());
   }

   private void onBlockBreak(BlockPos pos) {
      this.blockBreakPos = pos;
   }

   private void onBlockStopBreaking() {
      this.blockBreakPos = null;
   }

   public BlockPos getBreakingBlockPos() {
      return this.blockBreakPos;
   }

   public boolean isBreakingBlock() {
      return this.blockBreakPos != null;
   }

   public boolean inRange(Entity entity) {
      return this.mod.getPlayer().isInRange(entity, this.mod.getModSettings().getEntityReachRange());
   }

   public void attack(Entity entity) {
      if (this.inRange(entity)) {
         this.mod.getPlayer().tryAttack(entity);
         this.mod.getPlayer().swingHand(Hand.MAIN_HAND);
      }
   }
}
