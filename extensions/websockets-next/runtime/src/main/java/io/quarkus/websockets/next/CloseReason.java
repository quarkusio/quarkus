package io.quarkus.websockets.next;

import io.netty.handler.codec.http.websocketx.WebSocketCloseStatus;

/**
 * Indicates a reason for closing a connection. See also RFC-6455
 * <a href="https://datatracker.ietf.org/doc/html/rfc6455#section-5.5.1">section 5.5.1</a>. The pre-defined status codes are
 * listed in <a href="https://datatracker.ietf.org/doc/html/rfc6455#section-7.4.1">section 7.4.1</a>.
 *
 * @see WebSocketCloseStatus
 * @see WebSocketConnection#close(CloseReason)
 * @see WebSocketClientConnection#close(CloseReason)
 */
public class CloseReason {

    public static final CloseReason NORMAL = new CloseReason(WebSocketCloseStatus.NORMAL_CLOSURE.code());

    public static final CloseReason ABNORMAL = new CloseReason(WebSocketCloseStatus.ABNORMAL_CLOSURE.code(), null, false);

    public static final CloseReason EMPTY = new CloseReason(WebSocketCloseStatus.EMPTY.code(), null, false);

    public static final CloseReason INTERNAL_SERVER_ERROR = new CloseReason(WebSocketCloseStatus.INTERNAL_SERVER_ERROR.code());

    private final int code;

    private final String message;

    private CloseReason(int code, String message, boolean validate) {
        if (validate && !WebSocketCloseStatus.isValidStatusCode(code)) {
            throw new IllegalArgumentException("Invalid status code: " + code);
        }
        this.code = code;
        this.message = message;
    }

    /**
     *
     * @param code The status code must comply with RFC-6455
     */
    public CloseReason(int code) {
        this(code, null, true);
    }

    /**
     *
     * @param code The status code must comply with RFC-6455
     * @param message
     */
    public CloseReason(int code, String message) {
        this(code, message, true);
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "CloseReason [code=" + code + ", " + (message != null ? "message=" + message : "") + "]";
    }

}
