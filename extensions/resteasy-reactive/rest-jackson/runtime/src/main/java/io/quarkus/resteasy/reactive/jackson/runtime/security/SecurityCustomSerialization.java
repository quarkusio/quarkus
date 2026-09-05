package io.quarkus.resteasy.reactive.jackson.runtime.security;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.ser.std.SimpleFilterProvider;

public class SecurityCustomSerialization implements BiFunction<ObjectMapper, Type, ObjectWriter> {

    private static final Map<ObjectMapper, ObjectWriter> WRITERS = new ConcurrentHashMap<>();

    @Override
    public ObjectWriter apply(ObjectMapper objectMapper, Type type) {
        return WRITERS.computeIfAbsent(objectMapper, new Function<>() {

            @Override
            public ObjectWriter apply(ObjectMapper objectMapper) {
                return ((JsonMapper) objectMapper).rebuild()
                        .annotationIntrospector(new SecurityJacksonAnnotationIntrospector())
                        .build()
                        .writer()
                        .with(new SimpleFilterProvider().addFilter(SecurityPropertyFilter.FILTER_ID,
                                new SecurityPropertyFilter()));
            }
        });
    }
}
