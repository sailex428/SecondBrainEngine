package adris.altoclef.mixins.baritone;

import baritone.api.IBaritone;
import baritone.api.utils.IEntityAccessor;
import baritone.behavior.PathingBehavior;
import net.minecraft.entity.Entity;
import net.minecraft.entity.Entity.RemovalReason;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
