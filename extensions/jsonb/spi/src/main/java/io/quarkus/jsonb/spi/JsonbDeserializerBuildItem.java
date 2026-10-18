package io.quarkus.jsonb.spi;

import java.util.Collection;
import java.util.Collections;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * BuildItem used to register a custom JsonbDeserializer with the default Jsonb bean
 *
 * Serializers and deserializers MUST contain a public no-args constructor
 */
public final class JsonbDeserializerBuildItem extends MultiBuildItem {

    private final Collection<String> deserializerClassNames;

    public JsonbDeserializerBuildItem(String deserializerClassName) {
        this(Collections.singletonList(deserializerClassName));
    }

    public JsonbDeserializerBuildItem(Collection<String> deserializerClassNames) {
        this.deserializerClassNames = deserializerClassNames;
    }

    public Collection<String> getDeserializerClassNames() {
        return deserializerClassNames;
    }
}
