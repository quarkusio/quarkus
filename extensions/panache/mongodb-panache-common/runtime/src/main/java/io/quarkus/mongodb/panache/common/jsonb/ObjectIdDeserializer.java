package io.quarkus.mongodb.panache.common.jsonb;

import java.lang.reflect.Type;

import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.stream.JsonParser;

import org.bson.types.ObjectId;

public class ObjectIdDeserializer implements JsonbDeserializer<ObjectId> {
    @Override
    public ObjectId deserialize(JsonParser parser, DeserializationContext ctx, Type rtType) {
        String id = parser.getString();
        if (id != null) {
            return new ObjectId(id);
        }
        return null;
    }
}
