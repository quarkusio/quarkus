package io.quarkus.resteasy.reactive.jackson.runtime.mappers;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class UnwrappingSerializer extends StdSerializer<Object> {

    private final GeneratedSerializer delegate;

    public UnwrappingSerializer(GeneratedSerializer delegate) {
        super(Object.class);
        this.delegate = delegate;
    }

    @Override
    public void serialize(Object value, JsonGenerator gen, SerializerProvider prov) throws IOException {
        delegate.serializeContent(value, gen, prov);
    }

    @Override
    public boolean isUnwrappingSerializer() {
        return true;
    }
}
