package io.quarkus.test.junit.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import com.thoughtworks.xstream.converters.collections.CollectionConverter;
import com.thoughtworks.xstream.mapper.Mapper;

/**
 * A custom List converter that always uses ArrayList for unmarshalling.
 * This is probably not semantically correct 100% of the time, but it's likely fine
 * for all the cases where we are using marshalling / unmarshalling.
 *
 * The reason for doing this is to avoid XStream causing illegal access issues
 * for internal JDK lists
 */
public class CustomListConverter extends CollectionConverter {

    // if we wanted to be 100% sure, we'd list all the List.of methods, but I think it's pretty safe to say
    // that the JDK won't add custom implementations for the other classes

    private final Predicate<String> supported = new Predicate<String>() {

        private final Set<String> JDK_LIST_CLASS_NAMES = Set.of(
                List.of().getClass().getName(),
                List.of(Integer.MAX_VALUE).getClass().getName(),
                Arrays.asList(Integer.MAX_VALUE).getClass().getName(),
                Collections.unmodifiableList(List.of()).getClass().getName(),
                Collections.emptyList().getClass().getName(),
                List.of(Integer.MIN_VALUE, Integer.MAX_VALUE).subList(0, 1).getClass().getName());

        @Override
        public boolean test(String className) {
            return JDK_LIST_CLASS_NAMES.contains(className);
        }
    }.or(new Predicate<>() {

        private static final String GUAVA_LISTS_PACKAGE = "com.google.common.collect.Lists";

        @Override
        public boolean test(String className) {
            return className.startsWith(GUAVA_LISTS_PACKAGE);
        }
    });

    public CustomListConverter(Mapper mapper) {
        super(mapper);
    }

    @Override
    public boolean canConvert(Class type) {
        return (type != null) && supported.test(type.getName());
    }

    @Override
    protected Object createCollection(Class type) {
        return new ArrayList<>();
    }
}
