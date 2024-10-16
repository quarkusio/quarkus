package io.quarkus.websockets.next;

import io.smallrye.common.annotation.Experimental;

@Experimental("This API is experimental and may change in the future")
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
