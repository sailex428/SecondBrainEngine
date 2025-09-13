package me.sailex.altoclef.mixins;

// this is only client side i guess??

//@Mixin({ClientPlayerInteractionManager.class})
//public final class ClientBlockBreakMixin {
//   @Unique
//   private static int breakCancelFrames;
//
//   @Inject(
//      method = {"continueDestroyBlock"},
//      at = {@At("HEAD")}
//   )
//   private void onBreakUpdate(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> ci) {
//      EventBus.publish(new BlockBreakingEvent(pos));
//   }
//
//   @Inject(
//      method = {"stopDestroyBlock"},
//      at = {@At("HEAD")}
//   )
//   private void cancelBlockBreaking(CallbackInfo ci) {
//      if (breakCancelFrames-- == 0) {
//         EventBus.publish(new BlockBreakingCancelEvent());
//      }
//   }
//}
