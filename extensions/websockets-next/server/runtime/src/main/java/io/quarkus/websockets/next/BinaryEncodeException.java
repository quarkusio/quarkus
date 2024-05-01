package io.quarkus.websockets.next;

import io.smallrye.common.annotation.Experimental;

/**
 *
 * @see BinaryMessageCodec
 */
@Experimental("This API is experimental and may change in the future")
public class BinaryEncodeException extends WebSocketException {

    private static final long serialVersionUID = -8042792962717461873L;

    private final Object encodedObject;

    public BinaryEncodeException(Object encodedObject, String message) {
        this(encodedObject, message, null);
    }

    public BinaryEncodeException(Object encodedObject, String message, Throwable cause) {
        super(message, cause);
        this.encodedObject = encodedObject;
    }

    public Object getEncodedObject() {
        return encodedObject;
    }

}
