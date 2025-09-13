package adris.altoclef.mixins.baritone;

//@Mixin({LivingEntity.class})
//public abstract class MixinMobEntity {
//   @Shadow
//   public abstract void setSpeed(float speed) ;
//
//   @Inject(
//      method = {"serverAiStep"},
//      at = {@At("HEAD")},
//      cancellable = true
//   )
//   private void cancelAiTick(CallbackInfo ci) {
//      if (BaritoneProvider.INSTANCE.isPathing((LivingEntity)(Object)this)) {
//         float forwardSpeed = ((LivingEntity)(Object)this).forwardSpeed;
//         this.setSpeed((float)((LivingEntity)(Object)this).getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED));
//         ((LivingEntity)(Object)this).forwardSpeed = forwardSpeed;
//         ci.cancel();
//      }
//   }
//}
