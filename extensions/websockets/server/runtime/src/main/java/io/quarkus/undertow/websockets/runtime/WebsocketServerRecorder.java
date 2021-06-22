package io.quarkus.undertow.websockets.runtime;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import javax.websocket.DeploymentException;
import javax.websocket.Extension;

import org.jboss.logging.Logger;

import io.netty.channel.EventLoopGroup;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.undertow.websockets.client.runtime.ServerWebSocketContainerFactory;
import io.quarkus.undertow.websockets.client.runtime.WebsocketCoreRecorder;
import io.undertow.websockets.ServerWebSocketContainer;
import io.undertow.websockets.WebSocketDeploymentInfo;
import io.undertow.websockets.WebSocketReconnectHandler;
import io.undertow.websockets.util.ContextSetupHandler;
import io.undertow.websockets.util.ObjectIntrospecter;
import io.undertow.websockets.vertx.VertxServerWebSocketContainer;
import io.undertow.websockets.vertx.VertxWebSocketHandler;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class WebsocketServerRecorder {

    private static final Logger log = Logger.getLogger(WebsocketCoreRecorder.class);

    public Handler<RoutingContext> createHandler(RuntimeValue<WebSocketDeploymentInfo> info,
            RuntimeValue<ServerWebSocketContainer> container) throws DeploymentException {
        return new VertxWebSocketHandler(container.getValue(), info.getValue());
    }

    public ServerWebSocketContainerFactory createFactory() {
        return new ServerWebSocketContainerFactory() {
            @Override
            public ServerWebSocketContainer create(ObjectIntrospecter objectIntrospecter, ClassLoader classLoader,
                    Supplier<EventLoopGroup> eventLoopSupplier, List<ContextSetupHandler> contextSetupHandlers,
                    boolean dispatchToWorker, InetSocketAddress clientBindAddress, WebSocketReconnectHandler reconnectHandler,
                    Supplier<Executor> executorSupplier, List<Extension> installedExtensions, int maxFrameSize) {
                return new VertxServerWebSocketContainer(objectIntrospecter, classLoader, eventLoopSupplier,
                        contextSetupHandlers, dispatchToWorker, clientBindAddress, reconnectHandler, executorSupplier,
                        installedExtensions, maxFrameSize);
            }
        };
    }
}
