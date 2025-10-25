package me.sailex.mixins;

import me.sailex.altoclef.eventbus.EventBus;
import me.sailex.altoclef.eventbus.events.PlayerDamageEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class PlayerDamageMixin extends Entity {

    protected PlayerDamageMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(
      method = {"damage"},
      at = {@At("HEAD")}
    )
    //? >=1.21.8 {
    /*public void applyDamage(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> ci) {
        EventBus.publish(new PlayerDamageEvent(this, source, amount));
    }
    *///?} else {
    public void applyDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> ci) {
      EventBus.publish(new PlayerDamageEvent(this, source, amount));
    }
    //?}
}
