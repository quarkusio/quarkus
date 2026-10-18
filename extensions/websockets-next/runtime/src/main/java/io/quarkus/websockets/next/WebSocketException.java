package io.quarkus.websockets.next;

public class WebSocketException extends RuntimeException {

    private static final long serialVersionUID = 903932032264812404L;

    public WebSocketException(String message, Throwable cause) {
        super(message, cause);
    }

    public WebSocketException(String message) {
        super(message);
    }

    public WebSocketException(Throwable cause) {
        super(cause);
    }

}
