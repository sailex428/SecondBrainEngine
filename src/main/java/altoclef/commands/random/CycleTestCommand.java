package altoclef.commands.random;

import adris.altoclef.AltoClefController;
import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.tasks.speedrun.OneCycleTask;

public class CycleTestCommand extends Command {
   public CycleTestCommand() {
      super("cycle", "One cycles the dragon B)");
   }

   @Override
   protected void call(AltoClefController mod, ArgParser parser) {
      mod.runUserTask(new OneCycleTask(), () -> this.finish());
   }
}
