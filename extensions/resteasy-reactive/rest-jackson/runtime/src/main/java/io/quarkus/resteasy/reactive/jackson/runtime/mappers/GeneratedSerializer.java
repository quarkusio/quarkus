package io.quarkus.resteasy.reactive.jackson.runtime.mappers;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.util.NameTransformer;

public abstract class GeneratedSerializer extends StdSerializer<Object> {

    protected GeneratedSerializer(Class<?> cls) {
        super(cls, false);
    }

    public abstract void serializeContent(Object value, JsonGenerator gen,
            SerializerProvider prov) throws IOException;

    @Override
    public void serialize(Object value, JsonGenerator gen,
            SerializerProvider prov) throws IOException {
        gen.writeStartObject();
        serializeContent(value, gen, prov);
        gen.writeEndObject();
    }

    @Override
    public void serializeWithType(Object value, JsonGenerator gen,
            SerializerProvider prov, TypeSerializer typeSer) throws IOException {
        WritableTypeId typeIdDef = typeSer.writeTypePrefix(gen,
                typeSer.typeId(value, JsonToken.START_OBJECT));
        serializeContent(value, gen, prov);
        typeSer.writeTypeSuffix(gen, typeIdDef);
    }

    @Override
    public JsonSerializer<Object> unwrappingSerializer(NameTransformer transformer) {
        return new UnwrappingSerializer(this);
    }
}
