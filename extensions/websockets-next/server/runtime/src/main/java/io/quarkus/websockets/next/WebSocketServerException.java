package io.quarkus.websockets.next;

import io.smallrye.common.annotation.Experimental;

@Experimental("This API is experimental and may change in the future")
public class WebSocketServerException extends RuntimeException {

    private static final long serialVersionUID = 903932032264812404L;

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
