package io.quarkus.mongodb.panache.common.jsonb;

import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;

import org.bson.types.ObjectId;

public class ObjectIdSerializer implements JsonbSerializer<ObjectId> {

    @Override
    public void serialize(ObjectId obj, JsonGenerator generator, SerializationContext ctx) {
        if (obj != null) {
            generator.write(obj.toString());
        }
    }
}
