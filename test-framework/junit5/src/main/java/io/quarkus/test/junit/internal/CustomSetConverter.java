package io.quarkus.test.junit.internal;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.thoughtworks.xstream.converters.collections.CollectionConverter;
import com.thoughtworks.xstream.mapper.Mapper;

/**
 * A custom Set converter that always uses HashSet for unmarshalling.
 * This is probably not semantically correct 100% of the time, but it's likely fine
 * for all the cases where we are using marshalling / unmarshalling.
 *
 * The reason for doing this is to avoid XStream causing illegal access issues
 * for internal JDK sets
 */
public class CustomSetConverter extends CollectionConverter {

    // if we wanted to be 100% sure, we'd list all the Set.of methods, but I think it's pretty safe to say
    // that the JDK won't add custom implementations for the other classes
    private final Set<String> SUPPORTED_CLASS_NAMES = Set.of(
            Set.of().getClass().getName(),
            Set.of(Integer.MAX_VALUE).getClass().getName(),
            Collections.emptySet().getClass().getName());

    public CustomSetConverter(Mapper mapper) {
        super(mapper);
    }

    @Override
    public boolean canConvert(Class type) {
        return (type != null) && SUPPORTED_CLASS_NAMES.contains(type.getName());
    }

    @Override
    protected Object createCollection(Class type) {
        return new HashSet<>();
    }
}
