package io.quarkus.mongodb.panache.jackson;

import java.io.IOException;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class ObjectIdDeserializer extends StdDeserializer<ObjectId> {

    public ObjectIdDeserializer() {
        super(ObjectId.class);
    }

    @Override
    public ObjectId deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
            throws IOException, JsonProcessingException {
        String value = jsonParser.getValueAsString();
        if (value != null) {
            return new ObjectId(value);
        }
        return null;
    }
}
