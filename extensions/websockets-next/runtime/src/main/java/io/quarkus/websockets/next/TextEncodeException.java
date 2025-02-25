package io.quarkus.websockets.next;

/**
 *
 * @see TextMessageCodec
 */
public class TextEncodeException extends WebSocketException {

    private static final long serialVersionUID = 837621296462089705L;

    private final Object encodedObject;

    public TextEncodeException(Object encodedObject, String message) {
        this(encodedObject, message, null);
    }

    public TextEncodeException(Object encodedObject, String message, Throwable cause) {
        super(message, cause);
        this.encodedObject = encodedObject;
    }

    public Object getEncodedObject() {
        return encodedObject;
    }

}
