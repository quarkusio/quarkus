package io.quarkus.mongodb.panache.jackson;

import java.io.IOException;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

public class ObjectIdDeserializer extends JsonDeserializer<ObjectId> {

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
