/**
 *
 */
package io.quarkus.creator.config.reader;

/**
 *
 * @author Alexey Loubyansky
 */
public interface UnrecognizedNameElementHandler {

    void unrecognizedNameElement(PropertyLine line, int unrecognizedNameElement) throws PropertiesConfigReaderException;
}
