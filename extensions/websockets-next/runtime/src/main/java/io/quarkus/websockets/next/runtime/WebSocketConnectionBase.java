package io.quarkus.websockets.next.runtime;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.jboss.logging.Logger;

import io.quarkus.vertx.core.runtime.VertxBufferImpl;
import io.quarkus.websockets.next.CloseReason;
import io.quarkus.websockets.next.HandshakeRequest;
import io.quarkus.websockets.next.WebSocketConnection.BroadcastSender;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.vertx.UniHelper;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.buffer.impl.BufferImpl;
import io.vertx.core.http.WebSocketBase;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public abstract class WebSocketConnectionBase {

    private static final Logger LOG = Logger.getLogger(WebSocketConnectionBase.class);

    protected final String identifier;

    protected final Map<String, String> pathParams;

    protected final Codecs codecs;

    protected final HandshakeRequest handshakeRequest;

    protected final Instant creationTime;

    WebSocketConnectionBase(Map<String, String> pathParams, Codecs codecs, HandshakeRequest handshakeRequest) {
        this.identifier = UUID.randomUUID().toString();
        this.pathParams = pathParams;
        this.codecs = codecs;
        this.handshakeRequest = handshakeRequest;
        this.creationTime = Instant.now();
    }

    abstract WebSocketBase webSocket();

    public String id() {
        return identifier;
    }

    public String pathParam(String name) {
        return pathParams.get(name);
    }

    public Uni<Void> sendText(String message) {
        return UniHelper.toUni(webSocket().writeTextMessage(message));
    }

    public Uni<Void> sendBinary(Buffer message) {
        return UniHelper.toUni(webSocket().writeBinaryMessage(message));
    }

    public <M> Uni<Void> sendText(M message) {
        String text;
        // Use the same conversion rules as defined for the OnTextMessage
        if (message instanceof JsonObject || message instanceof JsonArray || message instanceof BufferImpl
                || message instanceof VertxBufferImpl) {
            text = message.toString();
        } else if (message.getClass().isArray() && message.getClass().arrayType().equals(byte.class)) {
            text = Buffer.buffer((byte[]) message).toString();
        } else {
            text = codecs.textEncode(message, null);
        }
        return sendText(text);
    }

    public Uni<Void> sendPing(Buffer data) {
        return UniHelper.toUni(webSocket().writePing(data));
    }

    void sendAutoPing() {
        webSocket().writePing(Buffer.buffer("ping")).onComplete(r -> {
            if (r.failed()) {
                LOG.warnf("Unable to send auto-ping for %s: %s", this, r.cause().toString());
            }
        });
    }

    public Uni<Void> sendPong(Buffer data) {
        return UniHelper.toUni(webSocket().writePong(data));
    }

    public Uni<Void> close() {
        return close(CloseReason.NORMAL);
    }

    public Uni<Void> close(CloseReason reason) {
        if (isClosed()) {
            LOG.warnf("Connection already closed: %s", this);
            return Uni.createFrom().voidItem();
        }
        return UniHelper.toUni(webSocket().close((short) reason.getCode(), reason.getMessage()));
    }

    public boolean isSecure() {
        return webSocket().isSsl();
    }

    public boolean isClosed() {
        return webSocket().isClosed();
    }

    public HandshakeRequest handshakeRequest() {
        return handshakeRequest;
    }

    public Instant creationTime() {
        return creationTime;
    }

    public BroadcastSender broadcast() {
        throw new UnsupportedOperationException();
    }

    public CloseReason closeReason() {
        WebSocketBase ws = webSocket();
        if (ws.isClosed()) {
            return new CloseReason(ws.closeStatusCode(), ws.closeReason());
        }
        return null;
    }
}
