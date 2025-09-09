package adris.altoclef.mixins.baritone;

import baritone.BaritoneProvider;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
