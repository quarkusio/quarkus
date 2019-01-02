/**
 *
 */
package org.jboss.shamrock.creator.config.reader;

/**
 *
 * @author Alexey Loubyansky
 */
public interface UnrecognizedPropertyHandler {

    void unrecognizedProperty(PropertyLine line) throws PropertiesConfigReaderException;
}
