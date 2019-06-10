package io.quarkus.tika;

@SuppressWarnings("serial")
public class TikaParseException extends RuntimeException {
    public TikaParseException() {
    }

    public TikaParseException(Throwable cause) {
        super(cause);
    }
}
