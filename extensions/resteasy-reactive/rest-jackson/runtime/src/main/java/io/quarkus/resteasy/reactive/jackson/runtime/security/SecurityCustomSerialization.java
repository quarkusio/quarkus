package io.quarkus.resteasy.reactive.jackson.runtime.security;

import java.lang.reflect.Type;
import java.util.function.BiFunction;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.ser.std.SimpleFilterProvider;

public class SecurityCustomSerialization implements BiFunction<ObjectMapper, Type, ObjectWriter> {

    private static volatile ObjectWriter writer;

    @Override
    public ObjectWriter apply(ObjectMapper objectMapper, Type type) {
        if (writer == null) {
            synchronized (this) {
                if (writer == null) {
                    writer = ((JsonMapper) objectMapper).rebuild()
                            .annotationIntrospector(new SecurityJacksonAnnotationIntrospector())
                            .build()
                            .writer()
                            .with(new SimpleFilterProvider().addFilter(SecurityPropertyFilter.FILTER_ID,
                                    new SecurityPropertyFilter()));
                }
            }
        }
        return writer; // we can use the same writer for all usages of @SecurityCustomSerialization because there is nothing resource method specific to it
    }
}
