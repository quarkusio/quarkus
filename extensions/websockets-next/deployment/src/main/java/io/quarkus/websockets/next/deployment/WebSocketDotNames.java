package io.quarkus.websockets.next.deployment;

import java.util.List;

import org.jboss.jandex.DotName;

import io.quarkus.websockets.next.CloseReason;
import io.quarkus.websockets.next.Connection;
import io.quarkus.websockets.next.HandshakeRequest;
import io.quarkus.websockets.next.OnBinaryMessage;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnError;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnPingMessage;
import io.quarkus.websockets.next.OnPongMessage;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.PathParam;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketClient;
import io.quarkus.websockets.next.WebSocketClientConnection;
import io.quarkus.websockets.next.WebSocketConnection;
import io.quarkus.websockets.next.WebSocketConnector;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

final class WebSocketDotNames {

    static final DotName WEB_SOCKET = DotName.createSimple(WebSocket.class);
    static final DotName WEB_SOCKET_CLIENT = DotName.createSimple(WebSocketClient.class);
    static final DotName CONNECTION = DotName.createSimple(Connection.class);
    static final DotName WEB_SOCKET_CONNECTION = DotName.createSimple(WebSocketConnection.class);
    static final DotName WEB_SOCKET_CLIENT_CONNECTION = DotName.createSimple(WebSocketClientConnection.class);
    static final DotName WEB_SOCKET_CONNECTOR = DotName.createSimple(WebSocketConnector.class);
    static final DotName ON_OPEN = DotName.createSimple(OnOpen.class);
    static final DotName ON_TEXT_MESSAGE = DotName.createSimple(OnTextMessage.class);
    static final DotName ON_BINARY_MESSAGE = DotName.createSimple(OnBinaryMessage.class);
    static final DotName ON_PING_MESSAGE = DotName.createSimple(OnPingMessage.class);
    static final DotName ON_PONG_MESSAGE = DotName.createSimple(OnPongMessage.class);
    static final DotName ON_CLOSE = DotName.createSimple(OnClose.class);
    static final DotName ON_ERROR = DotName.createSimple(OnError.class);
    static final DotName UNI = DotName.createSimple(Uni.class);
    static final DotName MULTI = DotName.createSimple(Multi.class);
    static final DotName RUN_ON_VIRTUAL_THREAD = DotName.createSimple(RunOnVirtualThread.class);
    static final DotName BLOCKING = DotName.createSimple(Blocking.class);
    static final DotName NON_BLOCKING = DotName.createSimple(NonBlocking.class);
    static final DotName STRING = DotName.createSimple(String.class);
    static final DotName BUFFER = DotName.createSimple(Buffer.class);
    static final DotName JSON_OBJECT = DotName.createSimple(JsonObject.class);
    static final DotName JSON_ARRAY = DotName.createSimple(JsonArray.class);
    static final DotName VOID = DotName.createSimple(Void.class);
    static final DotName PATH_PARAM = DotName.createSimple(PathParam.class);
    static final DotName HANDSHAKE_REQUEST = DotName.createSimple(HandshakeRequest.class);
    static final DotName THROWABLE = DotName.createSimple(Throwable.class);
    static final DotName CLOSE_REASON = DotName.createSimple(CloseReason.class);
    static final DotName TRANSACTIONAL = DotName.createSimple("jakarta.transaction.Transactional");

    static final List<DotName> CALLBACK_ANNOTATIONS = List.of(ON_OPEN, ON_CLOSE, ON_BINARY_MESSAGE, ON_TEXT_MESSAGE,
            ON_PING_MESSAGE, ON_PONG_MESSAGE, ON_ERROR);
}
