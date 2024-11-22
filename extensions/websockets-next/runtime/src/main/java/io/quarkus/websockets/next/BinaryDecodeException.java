package io.quarkus.websockets.next;

import io.vertx.core.buffer.Buffer;

/**
 *
 * @see BinaryMessageCodec
 */
public class BinaryDecodeException extends WebSocketException {

    private static final long serialVersionUID = 6814319993301938091L;

    private final Buffer bytes;

    public BinaryDecodeException(Buffer bytes, String message) {
        this(bytes, message, null);
    }

    public BinaryDecodeException(Buffer bytes, String message, Throwable cause) {
        super(message, cause);
        this.bytes = bytes;
    }

    public Buffer getBytes() {
        return bytes;
    }

}
