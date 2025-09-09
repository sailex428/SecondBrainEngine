package adris.altoclef;

import adris.altoclef.commands.*;
import adris.altoclef.commands.random.ScanCommand;
import adris.altoclef.commandsystem.CommandException;

public class AltoClefCommands {
   public static void init(AltoClefController controller) throws CommandException {
      controller.getCommandExecutor()
         .registerNewCommand(
            new GetCommand(),
            new EquipCommand(),
            new BodyLanguageCommand(),
            new DepositCommand(),
            new GotoCommand(),
            new IdleCommand(),
            new HeroCommand(),
            new LocateStructureCommand(),
            new StopCommand(),
            new FoodCommand(),
            new MeatCommand(),
            new ReloadSettingsCommand(),
            new ResetMemoryCommand(),
            new GamerCommand(),
            new FollowCommand(),
            new GiveCommand(),
            new ScanCommand(),
            new AttackPlayerOrMobCommand(),
            new SetAIBridgeEnabledCommand(),
            new FarmCommand(),
            new FishCommand()
         );
   }
}
