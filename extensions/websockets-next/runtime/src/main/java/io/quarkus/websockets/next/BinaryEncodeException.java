package io.quarkus.websockets.next;

/**
 *
 * @see BinaryMessageCodec
 */
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
