package io.quarkus.creator.config.reader;

/**
 *
 * @author Alexey Loubyansky
 */
public class PropertiesConfigUtils {

    public static void unrecognizedProperty(PropertyLine line) throws PropertiesConfigReaderException {
        throw new PropertiesConfigReaderException("Unrecorgnized property " + line.getName());
    }
}
