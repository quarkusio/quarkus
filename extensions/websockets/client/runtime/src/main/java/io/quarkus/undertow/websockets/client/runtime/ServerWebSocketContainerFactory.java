package io.quarkus.undertow.websockets.client.runtime;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import javax.websocket.Extension;

import io.netty.channel.EventLoopGroup;
import io.undertow.websockets.ServerWebSocketContainer;
import io.undertow.websockets.WebSocketReconnectHandler;
import io.undertow.websockets.util.ContextSetupHandler;
import io.undertow.websockets.util.ObjectIntrospecter;

public interface ServerWebSocketContainerFactory {
    ServerWebSocketContainer create(ObjectIntrospecter objectIntrospecter, ClassLoader classLoader,
            Supplier<EventLoopGroup> eventLoopSupplier, List<ContextSetupHandler> contextSetupHandlers,
            boolean dispatchToWorker, InetSocketAddress clientBindAddress, WebSocketReconnectHandler reconnectHandler,
            Supplier<Executor> executorSupplier, List<Extension> installedExtensions, int maxFrameSize);
}
