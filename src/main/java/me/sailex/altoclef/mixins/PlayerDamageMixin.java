package me.sailex.altoclef.mixins;

import me.sailex.altoclef.eventbus.EventBus;
import me.sailex.altoclef.eventbus.events.PlayerDamageEvent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({LivingEntity.class})
public class PlayerDamageMixin {
   @Inject(
      method = {"damage"},
      at = {@At("HEAD")}
   )
   public void applyDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> ci) {
      EventBus.publish(new PlayerDamageEvent((LivingEntity)(Object)this, source, amount));
   }
}
