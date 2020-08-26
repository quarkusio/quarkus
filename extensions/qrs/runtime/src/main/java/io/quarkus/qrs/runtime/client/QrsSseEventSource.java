package io.quarkus.qrs.runtime.client;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.sse.InboundSseEvent;
import javax.ws.rs.sse.SseEvent;
import javax.ws.rs.sse.SseEventSource;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpConnection;
import io.vertx.core.net.impl.ConnectionBase;

public class QrsSseEventSource implements SseEventSource, Handler<Buffer> {

    private static final byte CR = '\r';
    private static final byte LF = '\n';
    private static final byte COLON = ':';
    private static final byte SPACE = ' ';

    // FIXME: support
    private TimeUnit reconnectUnit;
    // FIXME: support
    private long reconnectDelay;

    private WebTarget endpoint;
    private volatile boolean isOpen;
    private List<Consumer<InboundSseEvent>> consumers = new ArrayList<>();
    private List<Consumer<Throwable>> errorListeners = new ArrayList<>();
    private List<Runnable> completionListeners = new ArrayList<>();
    private HttpConnection connection;

    //
    // The following are used while parsing

    /**
     * Will be non-empty while parsing. When not parsing, may hold partial data from the last chunk received
     */
    private byte[] bytes;
    /**
     * Index in {@link #bytes} where we're parsing, or where we should resume parsing partial data if not parsing.
     */
    private int i;

    /**
     * Holds the current event's comment data as we read them
     */
    private StringBuffer commentBuffer = new StringBuffer();
    /**
     * Holds the current event's field name as we read a field
     */
    private StringBuffer nameBuffer = new StringBuffer();
    /**
     * Holds the current event's field value as we read a field
     */
    private StringBuffer valueBuffer = new StringBuffer();
    /**
     * True if we're at the very beginning of the data stream and could see a BOM
     */
    private boolean firstByte = true;

    /**
     * The event type we're reading. Defaults to "message" and changes with "event" fields
     */
    private String eventType;
    /**
     * The event data we're reading. Defaults to "" and changes with "data" fields
     */
    private StringBuffer dataBuffer = new StringBuffer();
    /**
     * The event's last id we're reading. Defaults to null and changes with "id" fields (cannot be reset)
     */
    private String lastEventId;
    /**
     * The event connect time we're reading. Defaults to -1 and changes with "retry" fields (in ms)
     */
    private long eventReconnectTime;

    public QrsSseEventSource(WebTarget endpoint, long reconnectDelay, TimeUnit reconnectUnit) {
        this.endpoint = endpoint;
        this.reconnectDelay = reconnectDelay;
        this.reconnectUnit = reconnectUnit;
    }

    @Override
    public void register(Consumer<InboundSseEvent> onEvent) {
        consumers.add(onEvent);
    }

    @Override
    public void register(Consumer<InboundSseEvent> onEvent, Consumer<Throwable> onError) {
        consumers.add(onEvent);
        errorListeners.add(onError);
    }

    @Override
    public void register(Consumer<InboundSseEvent> onEvent, Consumer<Throwable> onError, Runnable onComplete) {
        consumers.add(onEvent);
        errorListeners.add(onError);
        completionListeners.add(onComplete);
    }

    @Override
    public void open() {
        if (isOpen)
            return;
        isOpen = true;
        QrsAsyncInvoker invoker = (QrsAsyncInvoker) endpoint.request().rx();
        // FIXME: this dual invocation target is shitty
        invoker.performRequestInternal("GET", null, null, event -> {
            // make sure we get exceptions on the response, like close events, otherwise they
            // will be logged as errors by vertx
            event.exceptionHandler(t -> {
                if (t == ConnectionBase.CLOSED_EXCEPTION) {
                    // we can ignore this one since we registered a closeHandler
                } else {
                    // FIXME: handle real exceptions
                    t.printStackTrace();
                }
            });
            connection = event.request().connection();
            connection.closeHandler(v -> {
                System.err.println("Client was closed by server");
            });
            event.handler(this);
        }).handle((response, throwable) -> {
            if (throwable != null)
                receiveThrowable(throwable);
            else
                handleInitialResponse(response);
            return null;
        });
    }

    private void receiveThrowable(Throwable throwable) {
        // TODO Auto-generated method stub

    }

    private void handleInitialResponse(Response response) {
        if (response.getStatus() != Response.Status.OK.getStatusCode()) {
            // FIXME: error
            return;
        }
        if (response.getMediaType() == null
                || !MediaType.SERVER_SENT_EVENTS_TYPE.isCompatible(response.getMediaType())) {
            // FIXME: error
            return;
        }
        // all checks done, let's wait for body parts
    }

    @Override
    public boolean isOpen() {
        return isOpen;
    }

    @Override
    public boolean close(long timeout, TimeUnit unit) {
        // FIXME: should wait for stuff?
        isOpen = false;
        connection.close();
        return true;
    }

    @Override
    public void handle(Buffer event) {
        byte[] newBytes = event.getBytes();
        // check if we have partial data remaining
        if (bytes != null) {
            // concat old and new data
            byte[] totalBytes = new byte[bytes.length - i + newBytes.length];
            System.arraycopy(bytes, i, totalBytes, 0, bytes.length - i);
            System.arraycopy(newBytes, 0, totalBytes, bytes.length - i, newBytes.length);
            bytes = totalBytes;
        } else {
            bytes = newBytes;
        }
        i = 0;

        while (hasByte()) {
            boolean lastFirstByte = firstByte;
            nameBuffer.setLength(0);
            valueBuffer.setLength(0);
            commentBuffer.setLength(0);
            dataBuffer.setLength(0);
            // SSE spec says default is "message" but JAX-RS says null
            eventType = null;
            eventReconnectTime = SseEvent.RECONNECT_NOT_SET;
            // SSE spec says ID is persistent

            int lastEventStart = i;
            try {
                parseEvent();
            } catch (NeedsMoreDataException x) {
                // save the remaining bytes for later
                i = lastEventStart;
                // be ready to rescan the BOM, but only if we didn't already see it in a previous event
                firstByte = lastFirstByte;
                return;
            }
        }
        // we ate all the data
        bytes = null;
    }

    private void parseEvent() {
        // optional BOM
        if (firstByte && i == 0 && 1 < bytes.length) {
            if (bytes[0] == (byte)0xFE
                    && bytes[1] == (byte)0xFF) {
                i = 2;
            }
        }
        // comment or field
        while (hasByte()) {
            int c = readChar();
            firstByte = false;
            if (c == COLON) {
                parseComment();
            } else if (isNameChar(c)) {
                parseField(c);
            } else if (isEofWithSideEffect(c)) {
                dispatchEvent();
                return;
            } else {
                throw illegalEventException();
            }
        }
    }

    private void dispatchEvent() {
        // ignore empty events
        if (dataBuffer.length() == 0)
            return;
        QrsInboundSseEvent event = new QrsInboundSseEvent();
        // SSE spec says empty string is the default, but JAX-RS says null if not specified
        event.setComment(commentBuffer.length() == 0 ? null : commentBuffer.toString());
        // SSE spec says empty string is the default, but JAX-RS says null if not specified
        event.setId(lastEventId);
        event.setData(dataBuffer.toString());
        // SSE spec says "message" is the default, but JAX-RS says null if not specified
        event.setName(eventType);
        event.setReconnectDelay(eventReconnectTime);
        fireEvent(event);
    }

    private void fireEvent(QrsInboundSseEvent event) {
        for (Consumer<InboundSseEvent> consumer : consumers) {
            consumer.accept(event);
        }
    }

    private byte peekByte() {
        return bytes[i];
    }

    private byte readByte() {
        if (i >= bytes.length)
            throw new NeedsMoreDataException();
        return bytes[i++];
    }

    private boolean hasByte() {
        return i < bytes.length;
    }

    private void parseComment() {
        // comment       = colon *any-char end-of-line
        while (true) {
            int c = readChar();
            if (isAnyChar(c)) {
                commentBuffer.appendCodePoint(c);
            } else if (isEofWithSideEffect(c)) {
                // we're done
                return;
            } else {
                throw illegalEventException();
            }
        }
    }

    private void parseField(int c) {
        boolean readingName = true;
        nameBuffer.appendCodePoint(c);
        // field         = 1*name-char [ colon [ space ] *any-char ] end-of-line
        while (true) {
            c = readChar();
            if (isEofWithSideEffect(c)) {
                // the colon is optional, so is the data, which we treat as an empty string
                processField(nameBuffer.toString(), valueBuffer.toString());
                nameBuffer.setLength(0);
                valueBuffer.setLength(0);
                // we're done
                return;
            }
            if (readingName && isNameChar(c)) {
                nameBuffer.appendCodePoint(c);
            } else if (readingName && c == COLON) {
                readingName = false;
                // optional space
                if (hasByte() && peekByte() == SPACE) {
                    i++;
                }
            } else if (!readingName && isAnyChar(c)) {
                valueBuffer.appendCodePoint(c);
            } else {
                throw illegalEventException();
            }
        }
    }

    private void processField(String name, String value) {
        switch (name) {
            case "event":
                eventType = value;
                break;
            case "data":
                if (dataBuffer.length() > 0) {
                    dataBuffer.append((char) LF);
                }
                dataBuffer.append(value);
                break;
            case "id":
                if (value.indexOf(0) == -1) {
                    lastEventId = value;
                }
                break;
            case "retry":
                try {
                    eventReconnectTime = Long.parseUnsignedLong(value, 10);
                } catch (NumberFormatException x) {
                    // spec says to ignore it
                }
                break;
            // default is to ignore the field
        }
    }

    private boolean isEofWithSideEffect(int c) {
        if (c == CR) {
            // eat a LF if there's one left
            // FIXME: if our buffer cuts here that's a bad spot
            if (hasByte() && peekByte() == LF) {
                i++;
            }
            // we're done
            return true;
        } else if (c == LF) {
            // we're done
            return true;
        }
        return false;
    }

    private boolean isAnyChar(int c) {
        return (c >= 0x0000 && c <= 0x0009)
                || (c >= 0x000B && c <= 0x000C)
                || (c >= 0x000E && c <= 0x10_FFFF);
    }

    private boolean isNameChar(int c) {
        return (c >= 0x0000 && c <= 0x0009)
                || (c >= 0x000B && c <= 0x000C)
                || (c >= 0x000E && c <= 0x0039)
                || (c >= 0x003B && c <= 0x10_FFFF);
    }

    private int readChar() {
        byte b0 = readByte();
        // single byte
        if ((b0 & 0b1000_0000) == 0) {
            return b0;
        }
        // two bytes
        if ((b0 & 0b1110_0000) == 0b1100_0000) {
            byte b1 = readByte();
            if ((b1 & 0b1100_0000) != 0b1000_0000) {
                throw utf8Exception();
            }
            return ((b0 & 0b0001_1111) << 6)
                    | (b1 & 0b0011_1111);
        }
        // three bytes
        if ((b0 & 0b1111_0000) == 0b1110_0000) {
            byte b1 = readByte();
            if ((b1 & 0b1100_0000) != 0b1000_0000) {
                throw utf8Exception();
            }
            byte b2 = readByte();
            if ((b2 & 0b1100_0000) != 0b1000_0000) {
                throw utf8Exception();
            }
            return ((b0 & 0b0000_1111) << 12)
                    | ((b1 & 0b0011_1111) << 6)
                    | (b2 & 0b0011_1111);
        }
        // four bytes
        if ((b0 & 0b1111_1000) == 0b1111_0000) {
            byte b1 = readByte();
            if ((b1 & 0b1100_0000) != 0b1000_0000) {
                throw utf8Exception();
            }
            byte b2 = readByte();
            if ((b2 & 0b1100_0000) != 0b1000_0000) {
                throw utf8Exception();
            }
            byte b3 = readByte();
            if ((b3 & 0b1100_0000) != 0b1000_0000) {
                throw utf8Exception();
            }
            return ((b0 & 0b0000_0111) << 18)
                    | ((b1 & 0b0011_1111) << 12)
                    | ((b2 & 0b0011_1111) << 6)
                    | (b3 & 0b0011_1111);
        }
        throw utf8Exception();
    }

    private IllegalStateException utf8Exception() {
        return new IllegalStateException("Illegal UTF8 input");
    }

    private IllegalStateException illegalEventException() {
        return new IllegalStateException("Illegal Server-Sent Event input at byte index " + i + " while parsing: "
                + new String(bytes, StandardCharsets.UTF_8));
    }
}
