package io.quarkus.creator.config.reader;

/**
 *
 * @author Alexey Loubyansky
 */
public interface PropertiesHandler<T> {

    /**
     * An instance that will receive configuration.
     *
     * @return instance that will receive configuration
     */
    T getTarget() throws PropertiesConfigReaderException;

    @SuppressWarnings("unchecked")
    default boolean setOnObject(PropertyContext ctx) throws PropertiesConfigReaderException {
        return set((T) ctx.o, ctx);
    }

    default boolean set(T t, PropertyContext ctx) throws PropertiesConfigReaderException {
        return false;
    }

    default PropertiesHandler<?> getNestedHandler(String name) throws PropertiesConfigReaderException {
        return null;
    }

    @SuppressWarnings("unchecked")
    default void setNestedOnObject(Object o, String name, Object child) throws PropertiesConfigReaderException {
        setNested((T) o, name, child);
    }

    default void setNested(T t, String name, Object child) throws PropertiesConfigReaderException {
        throw new UnsupportedOperationException(t + ", " + name + ", " + child);
    }
}
