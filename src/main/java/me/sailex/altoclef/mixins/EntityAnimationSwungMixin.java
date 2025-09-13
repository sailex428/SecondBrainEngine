package me.sailex.altoclef.mixins;

import me.sailex.altoclef.eventbus.EventBus;
import me.sailex.altoclef.eventbus.events.EntitySwungEvent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LivingEntity.class})
public abstract class EntityAnimationSwungMixin {
   @Inject(
      method = {"swingHand(Lnet/minecraft/util/Hand;)V"},
      at = {@At("HEAD")}
   )
   private void onEntityAnimation(Hand hand, CallbackInfo ci) {
      EventBus.publish(new EntitySwungEvent((LivingEntity)(Object)this));
   }
}
