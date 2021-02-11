package io.quarkus.mongodb.panache.jsonb;

import javax.json.bind.serializer.JsonbSerializer;
import javax.json.bind.serializer.SerializationContext;
import javax.json.stream.JsonGenerator;

import org.bson.types.ObjectId;

public class ObjectIdSerializer implements JsonbSerializer<ObjectId> {

    @Override
    public void serialize(ObjectId obj, JsonGenerator generator, SerializationContext ctx) {
        if (obj != null) {
            generator.write(obj.toString());
        }
    }
}
