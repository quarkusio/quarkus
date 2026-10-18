package io.quarkus.bootstrap.model;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

/**
 * Implemented by types that can be represented as a {@link Map}.
 */
public interface Mappable {

    static <T extends Mappable> Collection<Object> asMaps(Collection<T> col, MappableCollectionFactory factory) {
        if (col == null) {
            return null;
        }
        var result = factory.newCollection(col.size());
        for (var c : col) {
            result.add(c.asMap(factory));
        }
        return result;
    }

    static <T extends Mappable> Collection<Object> iterableAsMaps(Iterable<T> col, MappableCollectionFactory factory) {
        if (col == null) {
            return null;
        }
        var result = factory.newCollection();
        for (var c : col) {
            result.add(c.asMap(factory));
        }
        return result;
    }

    /**
     * Formats a collection of items as a comma-separated string.
     * If the argument is null, the method will return null.
     * If the argument is an empty collection, the method will return an empty string.
     *
     * @param col collection
     * @return command-separated collection of items
     */
    static Collection<Object> iterableToStringCollection(Iterable<?> col, MappableCollectionFactory factory) {
        if (col == null) {
            return null;
        }
        final Collection<Object> result = factory.newCollection();
        for (Object c : col) {
            result.add(c.toString());
        }
        return result;
    }

    /**
     * Formats a collection of items as a comma-separated string.
     * If the argument is null, the method will return null.
     * If the argument is an empty collection, the method will return an empty string.
     *
     * @param col collection
     * @return command-separated collection of items
     */
    static Collection<Object> toStringCollection(Collection<?> col, MappableCollectionFactory factory) {
        return toStringCollection(col, Object::toString, factory);
    }

    /**
     * Formats a collection of items as a comma-separated string.
     * If the argument is null, the method will return null.
     * If the argument is an empty collection, the method will return an empty string.
     *
     * @param col collection
     * @param converter converts an object to string
     * @return command-separated collection of items
     */
    static <T> Collection<Object> toStringCollection(Collection<T> col, Function<T, String> converter,
            MappableCollectionFactory factory) {
        if (col == null) {
            return null;
        }
        final Collection<Object> result = factory.newCollection(col.size());
        for (var c : col) {
            result.add(converter.apply(c));
        }
        return result;
    }

    /**
     * Invokes {@link #asMap(MappableCollectionFactory)} with the default {@link MappableCollectionFactory} implementation.
     *
     * @return a map representing this instance
     */
    default Map<String, Object> asMap() {
        return asMap(MappableCollectionFactory.defaultInstance());
    }

    /**
     * Returns an instance of a {@link Map} that represents this instance.
     *
     * @param factory collection factory
     * @return a map representing this instance
     */
    Map<String, Object> asMap(MappableCollectionFactory factory);
}
