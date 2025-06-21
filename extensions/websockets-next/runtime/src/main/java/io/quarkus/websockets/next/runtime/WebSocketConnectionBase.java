package io.quarkus.websockets.next.runtime;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import javax.net.ssl.SSLSession;

import org.jboss.logging.Logger;

import io.netty.handler.codec.http.websocketx.WebSocketCloseStatus;
import io.quarkus.vertx.utils.NoBoundChecksBuffer;
import io.quarkus.websockets.next.CloseReason;
import io.quarkus.websockets.next.Connection;
import io.quarkus.websockets.next.HandshakeRequest;
import io.quarkus.websockets.next.UserData;
import io.quarkus.websockets.next.WebSocketConnection.BroadcastSender;
import io.quarkus.websockets.next.runtime.telemetry.SendingInterceptor;
import io.smallrye.mutiny.Uni;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.buffer.impl.BufferImpl;
import io.vertx.core.http.WebSocketBase;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public abstract class WebSocketConnectionBase implements Connection {

    private static final Logger LOG = Logger.getLogger(WebSocketConnectionBase.class);

    protected final String identifier;

    protected final Map<String, String> pathParams;

    protected final Codecs codecs;

    protected final HandshakeRequest handshakeRequest;

    protected final Instant creationTime;

    protected final TrafficLogger trafficLogger;

    private final UserData userData;

    private final SendingInterceptor sendingInterceptor;

    WebSocketConnectionBase(Map<String, String> pathParams, Codecs codecs, HandshakeRequest handshakeRequest,
            TrafficLogger trafficLogger, UserData userData, SendingInterceptor sendingInterceptor) {
        this.identifier = UUID.randomUUID().toString();
        this.pathParams = pathParams;
        this.codecs = codecs;
        this.handshakeRequest = handshakeRequest;
        this.creationTime = Instant.now();
        this.trafficLogger = trafficLogger;
        this.userData = userData;
        this.sendingInterceptor = sendingInterceptor;
    }

    abstract WebSocketBase webSocket();

    @Override
    public String id() {
        return identifier;
    }

    @Override
    public String pathParam(String name) {
        return pathParams.get(name);
    }

    @Override
    public Uni<Void> sendText(String message) {
        Uni<Void> uni = Uni.createFrom().completionStage(() -> webSocket().writeTextMessage(message).toCompletionStage());
        if (sendingInterceptor != null) {
            uni = uni.invoke(() -> sendingInterceptor.onSend(message));
        }
        return trafficLogger == null ? uni : uni.invoke(() -> {
            trafficLogger.textMessageSent(this, message);
        });
    }

    @Override
    public Uni<Void> sendBinary(Buffer message) {
        Uni<Void> uni = Uni.createFrom().completionStage(() -> webSocket().writeBinaryMessage(message).toCompletionStage());
        if (sendingInterceptor != null) {
            uni = uni.invoke(() -> sendingInterceptor.onSend(message));
        }
        return trafficLogger == null ? uni : uni.invoke(() -> trafficLogger.binaryMessageSent(this, message));
    }

    @Override
    public <M> Uni<Void> sendText(M message) {
        String text;
        // Use the same conversion rules as defined for the OnTextMessage
        if (message instanceof JsonObject || message instanceof JsonArray || message instanceof BufferImpl
                || message instanceof NoBoundChecksBuffer) {
            text = message.toString();
        } else if (message.getClass().isArray() && message.getClass().arrayType().equals(byte.class)) {
            text = Buffer.buffer((byte[]) message).toString();
        } else {
            text = codecs.textEncode(message, null);
        }
        return sendText(text);
    }

    @Override
    public Uni<Void> sendPing(Buffer data) {
        return Uni.createFrom().completionStage(() -> webSocket().writePing(data).toCompletionStage());
    }

    void sendAutoPing() {
        webSocket().writePing(Buffer.buffer("ping")).onComplete(r -> {
            if (r.failed()) {
                LOG.warnf("Unable to send auto-ping for %s: %s", this, r.cause().toString());
            }
        });
    }

    @Override
    public Uni<Void> sendPong(Buffer data) {
        return Uni.createFrom().completionStage(() -> webSocket().writePong(data).toCompletionStage());
    }

    @Override
    public Uni<Void> close() {
        return close(CloseReason.NORMAL);
    }

    @Override
    public Uni<Void> close(CloseReason reason) {
        if (isClosed()) {
            LOG.warnf("Connection already closed: %s", this);
            return Uni.createFrom().voidItem();
        }
        return Uni.createFrom()
                .completionStage(() -> webSocket().close((short) reason.getCode(), reason.getMessage()).toCompletionStage());
    }

    @Override
    public boolean isSecure() {
        return webSocket().isSsl();
    }

    @Override
    public SSLSession sslSession() {
        return webSocket().sslSession();
    }

    @Override
    public boolean isClosed() {
        return webSocket().isClosed();
    }

    @Override
    public HandshakeRequest handshakeRequest() {
        return handshakeRequest;
    }

    @Override
    public String subprotocol() {
        return webSocket().subProtocol();
    }

    @Override
    public Instant creationTime() {
        return creationTime;
    }

    public BroadcastSender broadcast() {
        throw new UnsupportedOperationException();
    }

    @Override
    public CloseReason closeReason() {
        WebSocketBase ws = webSocket();
        if (ws.isClosed()) {
            Short code = ws.closeStatusCode();
            if (code == null || code == WebSocketCloseStatus.EMPTY.code()) {
                // This could happen if the connection is terminated abruptly
                return CloseReason.EMPTY;
            }
            if (code == WebSocketCloseStatus.ABNORMAL_CLOSURE.code()) {
                // This could happen if a close frame is never received
                return CloseReason.ABNORMAL;
            }
            return new CloseReason(code, ws.closeReason());
        }
        return null;
    }

    @Override
    public UserData userData() {
        return userData;
    }

}
