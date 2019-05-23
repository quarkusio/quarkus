package io.quarkus.tika;

@SuppressWarnings("serial")
public class ParseException extends RuntimeException {
    public ParseException() {
    }

    public ParseException(Throwable cause) {
        super(cause);
    }
}
