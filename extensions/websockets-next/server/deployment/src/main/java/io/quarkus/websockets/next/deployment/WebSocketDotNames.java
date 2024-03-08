package io.quarkus.websockets.next.deployment;

import org.jboss.jandex.DotName;

import io.quarkus.websockets.next.OnBinaryMessage;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnPongMessage;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

final class WebSocketDotNames {

    static final DotName WEB_SOCKET = DotName.createSimple(WebSocket.class);
    static final DotName WEB_SOCKET_CONNECTION = DotName.createSimple(WebSocketConnection.class);
    static final DotName ON_OPEN = DotName.createSimple(OnOpen.class);
    static final DotName ON_TEXT_MESSAGE = DotName.createSimple(OnTextMessage.class);
    static final DotName ON_BINARY_MESSAGE = DotName.createSimple(OnBinaryMessage.class);
    static final DotName ON_PONG_MESSAGE = DotName.createSimple(OnPongMessage.class);
    static final DotName ON_CLOSE = DotName.createSimple(OnClose.class);
    static final DotName UNI = DotName.createSimple(Uni.class);
    static final DotName MULTI = DotName.createSimple(Multi.class);
    static final DotName RUN_ON_VIRTUAL_THREAD = DotName.createSimple(RunOnVirtualThread.class);
    static final DotName BLOCKING = DotName.createSimple(Blocking.class);
    static final DotName STRING = DotName.createSimple(String.class);
    static final DotName BUFFER = DotName.createSimple(Buffer.class);
    static final DotName JSON_OBJECT = DotName.createSimple(JsonObject.class);
    static final DotName JSON_ARRAY = DotName.createSimple(JsonArray.class);
    static final DotName VOID = DotName.createSimple(Void.class);
}
