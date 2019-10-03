package io.quarkus.mongodb.panache.jackson;

import java.io.IOException;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class ObjectIdSerializer extends StdSerializer<ObjectId> {

    public ObjectIdSerializer() {
        super(ObjectId.class);
    }

    @Override
    public void serialize(ObjectId objectId, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
            throws IOException {
        if (objectId != null) {
            jsonGenerator.writeString(objectId.toString());
        }
    }
}
