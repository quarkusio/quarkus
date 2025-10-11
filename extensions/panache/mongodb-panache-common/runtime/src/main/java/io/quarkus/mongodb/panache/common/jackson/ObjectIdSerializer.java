package io.quarkus.mongodb.panache.common.jackson;

import java.io.IOException;

import org.bson.types.ObjectId;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

public class ObjectIdSerializer extends StdSerializer<ObjectId> {

    public ObjectIdSerializer() {
        super(ObjectId.class);
    }

    @Override
    public void serialize(ObjectId objectId, JsonGenerator jsonGenerator, SerializationContext serializerProvider)
            throws IOException {
        if (objectId != null) {
            jsonGenerator.writeString(objectId.toString());
        }
    }
}
