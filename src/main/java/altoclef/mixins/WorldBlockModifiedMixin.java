package altoclef.mixins;

import adris.altoclef.eventbus.EventBus;
import adris.altoclef.eventbus.events.BlockPlaceEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Level.class})
public class WorldBlockModifiedMixin {
   @Unique
   private boolean hasBlock(BlockState state, BlockPos pos) {
      return !state.isAir() && state.isRedstoneConductor((Level)(Object)this, pos);
   }

   @Inject(
      method = {"onBlockStateChange"},
      at = {@At("HEAD")}
   )
   public void onBlockWasChanged(BlockPos pos, BlockState oldBlock, BlockState newBlock, CallbackInfo ci) {
      if (!((Level)(Object)this).isClientSide && !this.hasBlock(oldBlock, pos) && this.hasBlock(newBlock, pos)) {
         BlockPlaceEvent evt = new BlockPlaceEvent(pos, newBlock);
         EventBus.publish(evt);
      }
   }
}
