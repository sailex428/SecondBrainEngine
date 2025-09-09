package adris.altoclef.util.time;

import adris.altoclef.AltoClefController;
import adris.altoclef.Debug;
import adris.altoclef.mixins.ClientConnectionAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.ClientConnection;

public class TimerGame extends BaseTimer {
   private ClientConnection lastConnection;

   public TimerGame(double intervalSeconds) {
      super(intervalSeconds);
   }

   private static double getTime(ClientConnection connection) {
      return connection == null ? 0.0 : ((ClientConnectionAccessor)connection).getTicks() / 20.0;
   }

   @Override
   protected double currentTime() {
      if (!AltoClefController.inGame()) {
         Debug.logError("Running game timer while not in game.");
         return 0.0;
      } else {
         ClientConnection currentConnection = null;
         if (MinecraftClient.getInstance().getNetworkHandler() != null) {
            currentConnection = MinecraftClient.getInstance().getNetworkHandler().getConnection();
         }

         if (currentConnection != this.lastConnection) {
            if (this.lastConnection != null) {
               double prevTimeTotal = getTime(this.lastConnection);
               Debug.logInternal("(TimerGame: New connection detected, offsetting by " + prevTimeTotal + " seconds)");
               this.setPrevTimeForce(this.getPrevTime() - prevTimeTotal);
            }

            this.lastConnection = currentConnection;
         }

         return getTime(currentConnection);
      }
   }
}
