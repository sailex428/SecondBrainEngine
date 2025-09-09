package adris.altoclef.multiversion;

import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.MessageType.Parameters;

public class MessageTypeVer {
   public static MessageType getMessageType(Parameters parameters) {
      return parameters.type();
   }
}
