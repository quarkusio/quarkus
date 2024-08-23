package io.quarkus.websockets.next;

import io.smallrye.common.annotation.Experimental;

/**
 *
 * @see TextMessageCodec
 */
@Experimental("This API is experimental and may change in the future")
public class TextDecodeException extends WebSocketException {

    private static final long serialVersionUID = 6814319993301938091L;

    private final String text;

    public TextDecodeException(String text, String message) {
        this(text, message, null);
    }

    public TextDecodeException(String text, String message, Throwable cause) {
        super(message, cause);
        this.text = text;
    }

    public String getText() {
        return text;
    }

}
