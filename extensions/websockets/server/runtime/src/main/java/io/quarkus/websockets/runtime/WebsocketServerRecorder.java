package io.quarkus.websockets.runtime;

import java.net.InetSocketAddress;
import java.security.Principal;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import javax.websocket.DeploymentException;
import javax.websocket.Extension;

import org.jboss.logging.Logger;

import io.netty.channel.EventLoopGroup;
import io.quarkus.arc.Arc;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
import io.quarkus.websockets.client.runtime.ServerWebSocketContainerFactory;
import io.quarkus.websockets.client.runtime.WebSocketPrincipal;
import io.quarkus.websockets.client.runtime.WebsocketCoreRecorder;
import io.undertow.websockets.ServerWebSocketContainer;
import io.undertow.websockets.WebSocketDeploymentInfo;
import io.undertow.websockets.WebSocketReconnectHandler;
import io.undertow.websockets.util.ContextSetupHandler;
import io.undertow.websockets.util.ObjectIntrospecter;
import io.undertow.websockets.vertx.VertxServerWebSocketContainer;
import io.undertow.websockets.vertx.VertxWebSocketHandler;
import io.undertow.websockets.vertx.VertxWebSocketHttpExchange;
import io.vertx.core.Handler;
import io.vertx.core.net.impl.ConnectionBase;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class WebsocketServerRecorder {

    private static final Logger log = Logger.getLogger(WebsocketCoreRecorder.class);

    public Handler<RoutingContext> createHandler(RuntimeValue<WebSocketDeploymentInfo> info,
            RuntimeValue<ServerWebSocketContainer> container) throws DeploymentException {
        return new VertxWebSocketHandler(container.getValue(), info.getValue()) {
            @Override
            protected VertxWebSocketHttpExchange createHttpExchange(RoutingContext event) {
                return new QuarkusVertxWebSocketHttpExchange(executor, event);
            }
        };
    }

    public ServerWebSocketContainerFactory createFactory() {
        return new ServerWebSocketContainerFactory() {
            @Override
            public ServerWebSocketContainer create(ObjectIntrospecter objectIntrospecter, ClassLoader classLoader,
                    Supplier<EventLoopGroup> eventLoopSupplier, List<ContextSetupHandler> contextSetupHandlers,
                    boolean dispatchToWorker, InetSocketAddress clientBindAddress, WebSocketReconnectHandler reconnectHandler,
                    Supplier<Executor> executorSupplier, List<Extension> installedExtensions, int maxFrameSize,
                    Supplier<Principal> currentUserSupplier) {
                var identity = Arc.container().instance(CurrentIdentityAssociation.class);
                return new VertxServerWebSocketContainer(objectIntrospecter, classLoader, eventLoopSupplier,
                        contextSetupHandlers, dispatchToWorker, clientBindAddress, reconnectHandler, executorSupplier,
                        installedExtensions, maxFrameSize, currentUserSupplier) {
                    @Override
                    protected VertxWebSocketHttpExchange createHttpExchange(RoutingContext routingContext) {
                        QuarkusHttpUser user = (QuarkusHttpUser) routingContext.user();
                        if (user != null) {
                            //close the connection when the identity expires
                            Long expire = user.getSecurityIdentity().getAttribute("quarkus.identity.expire-time");
                            if (expire != null) {
                                ((ConnectionBase) routingContext.request().connection()).channel().eventLoop()
                                        .schedule(new Runnable() {
                                            @Override
                                            public void run() {
                                                routingContext.request().connection().close();
                                            }
                                        }, expire - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
                            }
                        }
                        return new QuarkusVertxWebSocketHttpExchange(getExecutorSupplier().get(), routingContext);
                    }
                };
            }
        };
    }

    private static class QuarkusVertxWebSocketHttpExchange extends VertxWebSocketHttpExchange {

        private final RoutingContext routingContext;

        public QuarkusVertxWebSocketHttpExchange(Executor executor, RoutingContext routingContext) {
            super(executor, routingContext);
            this.routingContext = routingContext;
        }

        @Override
        public Principal getUserPrincipal() {
            QuarkusHttpUser user = (QuarkusHttpUser) routingContext.user();
            if (user != null) {
                return new WebSocketPrincipal(user.getSecurityIdentity());
            }
            return null;
        }
    }
}
