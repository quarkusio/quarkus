package io.quarkus.resteasy.reactive.jackson.runtime.security;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

public class SecurityCustomSerialization implements BiFunction<ObjectMapper, Type, ObjectWriter> {

    private static final Map<ObjectMapper, ObjectWriter> WRITERS = new ConcurrentHashMap<>();

    @Override
    public ObjectWriter apply(ObjectMapper objectMapper, Type type) {
        return WRITERS.computeIfAbsent(objectMapper, new Function<>() {

            @Override
            public ObjectWriter apply(ObjectMapper objectMapper) {
                return objectMapper
                        .copy()
                        .setAnnotationIntrospector(new SecurityJacksonAnnotationIntrospector())
                        .writer(
                                new SimpleFilterProvider().addFilter(SecurityPropertyFilter.FILTER_ID,
                                        new SecurityPropertyFilter()));
            }
        });
    }
}
