package me.sailex.altoclef.commands;

import me.sailex.altoclef.AltoClefController;
import me.sailex.altoclef.commandsystem.ArgParser;
import me.sailex.altoclef.commandsystem.Command;
import me.sailex.altoclef.tasks.movement.IdleTask;

public class IdleCommand extends Command {
   public IdleCommand() {
      super("idle", "Stand still");
   }

   @Override
   protected void call(AltoClefController mod, ArgParser parser) {
      mod.runUserTask(new IdleTask(), () -> this.finish());
   }
}
