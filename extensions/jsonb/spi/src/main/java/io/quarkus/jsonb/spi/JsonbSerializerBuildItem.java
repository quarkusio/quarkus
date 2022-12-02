package io.quarkus.jsonb.spi;

import java.util.Collection;
import java.util.Collections;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * BuildItem used to register a custom JsonbSerializer with the default Jsonb bean
 *
 * Serializers and deserializers MUST contain a public no-args constructor
 */
public final class JsonbSerializerBuildItem extends MultiBuildItem {

    private final Collection<String> serializerClassNames;

    public JsonbSerializerBuildItem(String serializerClassName) {
        this(Collections.singletonList(serializerClassName));
    }

    public JsonbSerializerBuildItem(Collection<String> serializerClassNames) {
        this.serializerClassNames = serializerClassNames;
    }

    public Collection<String> getSerializerClassNames() {
        return serializerClassNames;
    }
}
