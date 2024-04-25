package io.quarkus.websockets.next;

import io.smallrye.common.annotation.Experimental;

@Experimental("This API is experimental and may change in the future")
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
