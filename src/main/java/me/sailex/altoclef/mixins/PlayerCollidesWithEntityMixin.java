package me.sailex.altoclef.mixins;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({PlayerEntity.class})
public class PlayerCollidesWithEntityMixin {
   @Redirect(
      method = {"collideWithEntity"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/Entity;onPlayerCollision(Lnet/minecraft/entity/player/PlayerEntity;)V"
      )
   )
   private void onCollideWithEntity(Entity self, PlayerEntity player) {
//      if (player instanceof ClientPlayerEntity) {
//         EventBus.publish(new PlayerCollidedWithEntityEvent(player, self));
//      }

      self.onPlayerCollision(player);
   }
}
