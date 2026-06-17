package io.quarkus.resteasy.reactive.data.hibernate.runtime;

import java.io.IOException;

import jakarta.data.page.Page;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * Jackson serializer for {@link Page} that includes pagination metadata
 * instead of serializing as a plain array (which is the default because
 * {@link Page} extends {@link Iterable}).
 */
@SuppressWarnings("rawtypes")
public class PageSerializer extends StdSerializer<Page> {

    public PageSerializer() {
        super(Page.class);
    }

    @Override
    public void serialize(Page page, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();

        gen.writeObjectField("content", page.content());
        gen.writeBooleanField("hasNext", page.hasNext());
        gen.writeBooleanField("hasPrevious", page.hasPrevious());

        if (page.hasTotals()) {
            gen.writeNumberField("totalElements", page.totalElements());
            gen.writeNumberField("totalPages", page.totalPages());
        }

        gen.writeEndObject();
    }
}
