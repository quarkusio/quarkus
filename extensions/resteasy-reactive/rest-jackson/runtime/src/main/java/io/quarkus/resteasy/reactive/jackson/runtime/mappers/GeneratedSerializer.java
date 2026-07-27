package io.quarkus.resteasy.reactive.jackson.runtime.mappers;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonToken;
import tools.jackson.core.type.WritableTypeId;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.std.StdSerializer;
import tools.jackson.databind.util.NameTransformer;

public abstract class GeneratedSerializer extends StdSerializer<Object> {

    protected GeneratedSerializer(Class<?> cls) {
        super(cls, false);
    }

    public abstract void serializeContent(Object value, JsonGenerator gen,
            SerializationContext ctxt);

    @Override
    public void serialize(Object value, JsonGenerator gen,
            SerializationContext ctxt) {
        gen.writeStartObject();
        serializeContent(value, gen, ctxt);
        gen.writeEndObject();
    }

    @Override
    public void serializeWithType(Object value, JsonGenerator gen,
            SerializationContext ctxt, TypeSerializer typeSer) {
        WritableTypeId typeIdDef = typeSer.writeTypePrefix(gen, ctxt,
                typeSer.typeId(value, JsonToken.START_OBJECT));
        serializeContent(value, gen, ctxt);
        typeSer.writeTypeSuffix(gen, ctxt, typeIdDef);
    }

    @Override
    public ValueSerializer<Object> unwrappingSerializer(NameTransformer transformer) {
        return new UnwrappingSerializer(this);
    }
}
