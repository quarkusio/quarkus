package io.quarkus.resteasy.reactive.data.hibernate.runtime;

import jakarta.data.page.Page;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

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
    public void serialize(Page page, JsonGenerator gen, SerializationContext serializers) {
        gen.writeStartObject();

        gen.writePOJOProperty("content", page.content());
        gen.writeBooleanProperty("hasNext", page.hasNext());
        gen.writeBooleanProperty("hasPrevious", page.hasPrevious());

        if (page.hasTotals()) {
            gen.writeNumberProperty("totalElements", page.totalElements());
            gen.writeNumberProperty("totalPages", page.totalPages());
        }

        gen.writeEndObject();
    }
}
