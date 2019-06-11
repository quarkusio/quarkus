package io.quarkus.creator.config.reader;

/**
 *
 * @author Alexey Loubyansky
 */
public interface PropertyLineConverter<T> {

    PropertyLine toPropertyLine(T t) throws PropertiesConfigReaderException;
}
