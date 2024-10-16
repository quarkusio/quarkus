package io.quarkus.websockets.next;

import io.smallrye.common.annotation.Experimental;

@Experimental("This API is experimental and may change in the future")
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
