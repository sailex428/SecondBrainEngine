package adris.altoclef.mixins.baritone;

import baritone.Automatone;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.ExecutorService;
import net.minecraft.util.Util;

//@Mixin({Util.class})
//public abstract class MixinUtil {
//   @Shadow
//   private static void shutdownExecutor(ExecutorService service) {
//   }
//
//   @Inject(
//      method = {"shutdownExecutors"},
//      at = {@At("RETURN")}
//   )
//   private static void shutdownBaritoneExecutor(CallbackInfo ci) {
//      shutdownExecutor(Automatone.getExecutor());
//   }
//}
