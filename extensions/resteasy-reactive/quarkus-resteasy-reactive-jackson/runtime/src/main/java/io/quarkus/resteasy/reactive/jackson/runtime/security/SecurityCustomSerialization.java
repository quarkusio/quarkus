package io.quarkus.resteasy.reactive.jackson.runtime.security;

import java.lang.reflect.Type;
import java.util.function.BiFunction;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

public class SecurityCustomSerialization implements BiFunction<ObjectMapper, Type, ObjectWriter> {

    private static volatile ObjectWriter writer;

    @Override
    public ObjectWriter apply(ObjectMapper objectMapper, Type type) {
        if (writer == null) {
            synchronized (this) {
                if (writer == null) {
                    writer = objectMapper
                            .copy() // we need to make the copy in order to avoid adding the Introspector to the default mapper
                            .setAnnotationIntrospector(new SecurityJacksonAnnotationIntrospector()) // needed in order to trigger the inclusion of the filter
                            .writer(
                                    new SimpleFilterProvider().addFilter(SecurityPropertyFilter.FILTER_ID,
                                            new SecurityPropertyFilter())); // register the actual filter
                }
            }
        }
        return writer; // we can use the same writer for all usages of @SecurityCustomSerialization because there is nothing resource method specific to it
    }
}
