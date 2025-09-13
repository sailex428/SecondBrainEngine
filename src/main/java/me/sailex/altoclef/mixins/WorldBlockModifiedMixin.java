package me.sailex.altoclef.mixins;

import me.sailex.altoclef.eventbus.EventBus;
import me.sailex.altoclef.eventbus.events.BlockPlaceEvent;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({World.class})
public class WorldBlockModifiedMixin {
   @Unique
   private boolean hasBlock(BlockState state, BlockPos pos) {
      return !state.isAir() && state.isSolidBlock((World)(Object)this, pos);
   }

   @Inject(
      method = {"onBlockChanged"},
      at = {@At("HEAD")}
   )
   public void onBlockWasChanged(BlockPos pos, BlockState oldBlock, BlockState newBlock, CallbackInfo ci) {
      if (!((World)(Object)this).isClient && !this.hasBlock(oldBlock, pos) && this.hasBlock(newBlock, pos)) {
         BlockPlaceEvent evt = new BlockPlaceEvent(pos, newBlock);
         EventBus.publish(evt);
      }
   }
}
