package adris.altoclef.player2api.utils;

import adris.altoclef.player2api.Character;
import adris.altoclef.player2api.Player2APIService;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Map;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.network.PacketByteBuf;

public class CharacterUtils {
   public static Character DEFAULT_CHARACTER = new Character(
      "AI agent", "AI", "Greetings", "You are a helpful AI Agent", "minecraft:textures/entity/player/wide/steve.png", new String[0]
   );

   public static Character parseFirstCharacter(Map<String, JsonElement> responseMap) {
      Character[] characters = parseCharacters(responseMap);
      return characters.length > 0 ? characters[0] : DEFAULT_CHARACTER;
   }

   public static Character[] parseCharacters(Map<String, JsonElement> responseMap) {
      try {
         if (!responseMap.containsKey("characters")) {
            throw new Exception("No characters found in API response.");
         } else {
            JsonArray charactersArray = responseMap.get("characters").getAsJsonArray();
            if (charactersArray.isEmpty()) {
               throw new Exception("Character list is empty.");
            } else {
               Character[] characters = new Character[charactersArray.size()];

               for (int i = 0; i < charactersArray.size(); i++) {
                  JsonObject firstCharacter = charactersArray.get(i).getAsJsonObject();
                  String name = Utils.getStringJsonSafely(firstCharacter, "name");
                  if (name == null) {
                     throw new Exception("Character is missing 'name'.");
                  }

                  String shortName = Utils.getStringJsonSafely(firstCharacter, "short_name");
                  if (shortName == null) {
                     throw new Exception("Character is missing 'short_name'.");
                  }

                  String greeting = Utils.getStringJsonSafely(firstCharacter, "greeting");
                  String description = Utils.getStringJsonSafely(firstCharacter, "description");
                  String[] voiceIds = Utils.getStringArrayJsonSafely(firstCharacter, "voice_ids");
                  JsonObject meta = firstCharacter.get("meta").getAsJsonObject();
                  String skinURL = Utils.getStringJsonSafely(meta, "skin_url");
                  characters[i] = new Character(name, shortName, greeting, description, skinURL, voiceIds);
               }

               return characters;
            }
         }
      } catch (Exception var12) {
         System.err.println("Warning, getSelectedCharacter failed, reverting to default. Error message: " + var12.getMessage());
         return new Character[0];
      }
   }

   public static Character[] requestCharacters(String player2GameId) {
      try {
         Map<String, JsonElement> responseMap = HTTPUtils.sendRequest( "/v1/selected_characters", false, null, Player2APIService.getHeaders(player2GameId));
         return parseCharacters(responseMap);
      } catch (Exception var2) {
         return new Character[0];
      }
   }

   public static Character requestFirstCharacter(String player2GameId) {
      try {
         Map<String, JsonElement> responseMap = HTTPUtils.sendRequest( "/v1/selected_characters", false, null, Player2APIService.getHeaders(player2GameId));
         return parseFirstCharacter(responseMap);
      } catch (Exception var2) {
         return DEFAULT_CHARACTER;
      }
   }

   public static Character readFromBuf(PacketByteBuf buf) {
      String name = buf.readString();
      String shortName = buf.readString();
      String greetingInfo = buf.readString();
      String description = buf.readString();
      String skinURL = buf.readString();
      int arrSize = buf.readInt();
      String[] voiceIds = new String[arrSize];

      for (int i = 0; i < arrSize; i++) {
         voiceIds[i] = buf.readString();
      }

      return new Character(name, shortName, greetingInfo, description, skinURL, voiceIds);
   }

   public static void writeToBuf(PacketByteBuf buf, Character character) {
      buf.writeString(character.name());
      buf.writeString(character.shortName());
      buf.writeString(character.greetingInfo());
      buf.writeString(character.description());
      buf.writeString(character.skinURL());
      buf.writeInt(character.voiceIds().length);

      for (String id : character.voiceIds()) {
         buf.writeString(id);
      }
   }

   public static Character readFromNBT(NbtCompound compound) {
      String name = compound.getString("name");
      String shortName = compound.getString("shortName");
      String greetingInfo = compound.getString("greetingInfo");
      String description = compound.getString("description");
      String skinURL = compound.getString("skinURL");
      NbtList voiceIdsList = compound.getList("voiceIds", 8);
      String[] voiceIds = new String[voiceIdsList.size()];

      for (int i = 0; i < voiceIdsList.size(); i++) {
         voiceIds[i] = voiceIdsList.getString(i);
      }

      return new Character(name, shortName, greetingInfo, description, skinURL, voiceIds);
   }

   public static void writeToNBT(NbtCompound compound, Character character) {
      compound.putString("name", character.name());
      compound.putString("shortName", character.shortName());
      compound.putString("greetingInfo", character.greetingInfo());
      compound.putString("description", character.description());
      compound.putString("skinURL", character.skinURL());
      NbtList voiceIds = new NbtList();

      for (String id : character.voiceIds()) {
         voiceIds.add(NbtString.of(id));
      }

      compound.put("voiceIds", voiceIds);
   }
}