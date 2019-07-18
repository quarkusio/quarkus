package io.quarkus.resteasy.jsonb.runtime.serializers;

import javax.json.bind.serializer.JsonbSerializer;

import org.eclipse.yasson.internal.model.JsonbPropertyInfo;
import org.eclipse.yasson.internal.serializer.ContainerSerializerProvider;

public class SimpleContainerSerializerProvider implements ContainerSerializerProvider {

    private final JsonbSerializer<?> serializer;

    public SimpleContainerSerializerProvider(JsonbSerializer<?> serializer) {
        this.serializer = serializer;
    }

    @Override
    public JsonbSerializer<?> provideSerializer(JsonbPropertyInfo propertyInfo) {
        return serializer;
    }
}
