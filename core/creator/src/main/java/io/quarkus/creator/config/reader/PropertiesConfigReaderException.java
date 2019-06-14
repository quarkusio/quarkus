package io.quarkus.creator.config.reader;

/**
 *
 * @author Alexey Loubyansky
 */
public class PropertiesConfigReaderException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public PropertiesConfigReaderException(String message, Throwable cause) {
        super(message, cause);
    }

    public PropertiesConfigReaderException(String message) {
        super(message);
    }
}
