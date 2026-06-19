package io.quarkus.resteasy.reactive.jackson.runtime.mappers;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

public class UnwrappingSerializer extends StdSerializer<Object> {

    private final GeneratedSerializer delegate;

    public UnwrappingSerializer(GeneratedSerializer delegate) {
        super(Object.class);
        this.delegate = delegate;
    }

    @Override
    public void serialize(Object value, JsonGenerator gen, SerializationContext ctxt) {
        delegate.serializeContent(value, gen, ctxt);
    }

    @Override
    public boolean isUnwrappingSerializer() {
        return true;
    }
}
