package io.quarkus.tika;

@SuppressWarnings("serial")
public class TikaParseException extends RuntimeException {
    public TikaParseException() {
    }

    public TikaParseException(Throwable cause) {
        this(null, cause);
    }

    public TikaParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
