package me.sailex.altoclef.util.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.util.Collection;

public abstract class AbstractVectorSerializer<T> extends StdSerializer<T> {
   protected AbstractVectorSerializer() {
      this(null);
   }

   protected AbstractVectorSerializer(Class<T> vc) {
      super(vc);
   }

   protected abstract Collection<String> getParts(T var1);

   @Override
   public void serialize(T value, JsonGenerator gen, SerializerProvider provider) throws IOException {
      Collection<String> parts = this.getParts(value);
      gen.writeString(String.join(",", parts));
   }
}
