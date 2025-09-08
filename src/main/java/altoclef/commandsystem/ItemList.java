package altoclef.commandsystem;

import adris.altoclef.TaskCatalogue;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.helpers.FuzzySearchHelper;

import java.util.Collection;
import java.util.HashMap;

public class ItemList {
   public ItemTarget[] items;

   public ItemList(ItemTarget[] items) {
      this.items = items;
   }

   public static ItemList parseRemainder(String line) throws CommandException {
      line = line.trim();
      if (line.startsWith("[") && line.endsWith("]")) {
         line = line.substring(1, line.length() - 1);
         String[] parts = line.split(",");
         HashMap<String, Integer> items = new HashMap<>();

         for (String part : parts) {
            part = part.trim();
            String[] itemQuantityPair = part.split(" ");
            if (itemQuantityPair.length > 2 || itemQuantityPair.length <= 0) {
               throw new CommandException(
                  "Resource array element must be either \"item count\" or \"item\", but \"" + part + "\" has " + itemQuantityPair.length + " parts."
               );
            }

            String item = itemQuantityPair[0];
            int count = 1;
            if (itemQuantityPair.length > 1) {
               try {
                  count = Integer.parseInt(itemQuantityPair[1]);
               } catch (Exception var13) {
                  throw new CommandException("Failed to parse count for array element \"" + part + "\".");
               }
            }

            if (!TaskCatalogue.taskExists(item)) {
               Collection<String> allValidTargets = TaskCatalogue.resourceNames();
               String closestMatch = FuzzySearchHelper.getClosestMatchMinecraftItems(item, allValidTargets);
               if (closestMatch != null) {
                  throw new CommandException("Item not catalogued: \"" + item + "\". Did the user mean \"" + closestMatch + "\"?");
               }

               throw new CommandException("Item not catalogued: \"" + item + "\".");
            }

            items.put(item, items.getOrDefault(item, 0) + count);
         }

         if (items.size() != 0) {
            return new ItemList(items.entrySet().stream().map(entry -> new ItemTarget(entry.getKey(), entry.getValue())).toArray(ItemTarget[]::new));
         }
      } else {
         String[] items = line.split(" ");
         if (items.length >= 1) {
            String name = items[0];
            if (!TaskCatalogue.taskExists(name)) {
               Collection<String> allValidTargets = TaskCatalogue.resourceNames();
               String closestMatch = FuzzySearchHelper.getClosestMatchMinecraftItems(name, allValidTargets);
               if (closestMatch != null) {
                  throw new CommandException("Item not catalogued: \"" + name + "\". Did the user mean \"" + closestMatch + "\"?");
               }

               throw new CommandException("Item not catalogued: \"" + name + "\".");
            }

            int countx = 1;
            if (items.length == 2) {
               try {
                  countx = Integer.parseInt(items[1]);
               } catch (NumberFormatException var12) {
                  throw new CommandException("Failed to parse the following argument into type " + Integer.class + ": " + items[1] + ".");
               }
            } else if (items.length > 2) {
               throw new CommandException("Invalid item argument structure: Must be of form `<item>` or `<item> <count>`");
            }

            return new ItemList(new ItemTarget[]{new ItemTarget(name, countx)});
         }
      }

      return new ItemList(new ItemTarget[0]);
   }
}
