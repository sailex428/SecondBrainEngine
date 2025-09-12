package adris.altoclef.util.time;

import adris.altoclef.AltoClefController;
import adris.altoclef.Debug;

public class TimerGame extends BaseTimer {

   public TimerGame(double intervalSeconds) {
      super(intervalSeconds);
   }

   @Override
   protected double currentTime() {
      if (!AltoClefController.inGame()) {
         Debug.logError("Running game timer while not in game.");
         return 0.0;
      } else {
         return (double) System.currentTimeMillis() / 1000;
      }
   }
}
