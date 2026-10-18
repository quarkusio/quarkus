package io.quarkus.websockets.next;

public class WebSocketClientException extends WebSocketException {

    private static final long serialVersionUID = -4213710383874397185L;

    public WebSocketClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public WebSocketClientException(String message) {
        super(message);
    }

    public WebSocketClientException(Throwable cause) {
        super(cause);
    }

}
