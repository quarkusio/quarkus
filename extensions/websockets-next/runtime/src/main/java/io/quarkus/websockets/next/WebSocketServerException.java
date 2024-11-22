package io.quarkus.websockets.next;

public class WebSocketServerException extends WebSocketException {

    private static final long serialVersionUID = 815788270725783535L;

    public WebSocketServerException(String message, Throwable cause) {
        super(message, cause);
    }

    public WebSocketServerException(String message) {
        super(message);
    }

    public WebSocketServerException(Throwable cause) {
        super(cause);
    }

}
