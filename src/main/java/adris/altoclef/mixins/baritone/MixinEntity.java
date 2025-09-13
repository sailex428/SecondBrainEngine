package adris.altoclef.mixins.baritone;

//@Mixin({Entity.class})
//public abstract class MixinEntity implements IEntityAccessor {
//   @Shadow
//   public abstract World level();
//
//   @Invoker("getEyeHeight")
//   @Override
//   public abstract float automatone$invokeGetEyeHeight(EntityPose var1, EntityDimensions var2);
//
//   @Inject(
//      method = {"setRemoved"},
//      at = {@At("RETURN")}
//   )
//   private void shutdownPathingOnUnloading(RemovalReason reason, CallbackInfo ci) {
//      if (!this.level().isClient() && ((Object)this) instanceof LivingEntity) {
//         IBaritone.KEY.maybeGet((LivingEntity)(Object)this).ifPresent(b -> ((PathingBehavior)b.getPathingBehavior()).shutdown());
//      }
//   }
//}
