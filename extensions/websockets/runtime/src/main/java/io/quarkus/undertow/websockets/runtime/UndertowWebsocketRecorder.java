package io.quarkus.undertow.websockets.runtime;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.server.ServerApplicationConfig;
import javax.websocket.server.ServerEndpointConfig;

import org.jboss.logging.Logger;

import io.netty.channel.EventLoopGroup;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.ExecutorRecorder;
import io.quarkus.runtime.annotations.Recorder;
import io.undertow.websockets.WebSocketDeploymentInfo;
import io.undertow.websockets.util.ContextSetupHandler;
import io.undertow.websockets.util.ObjectFactory;
import io.undertow.websockets.util.ObjectHandle;
import io.undertow.websockets.util.ObjectIntrospecter;
import io.undertow.websockets.vertx.VertxServerWebSocketContainer;
import io.undertow.websockets.vertx.VertxWebSocketHandler;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class UndertowWebsocketRecorder {

    private static final Logger log = Logger.getLogger(UndertowWebsocketRecorder.class);

    public void setupWorker(Executor executor) {
        ExecutorSupplier.executor = executor;
    }

    @SuppressWarnings("unchecked")
    public WebSocketDeploymentInfo createDeploymentInfo(Set<String> annotatedEndpoints, Set<String> endpoints,
            Set<String> serverApplicationConfigClasses, int maxFrameSize, boolean dispatchToWorker) {
        WebSocketDeploymentInfo container = new WebSocketDeploymentInfo();
        container.setMaxFrameSize(maxFrameSize);
        container.setDispatchToWorkerThread(dispatchToWorker);
        container.setExecutor(new ExecutorSupplier());
        Set<Class<? extends Endpoint>> allScannedEndpointImplementations = new HashSet<>();
        for (String i : endpoints) {
            try {
                allScannedEndpointImplementations.add(
                        (Class<? extends Endpoint>) Class.forName(i, true, Thread.currentThread().getContextClassLoader()));
            } catch (Exception e) {
                log.error("Could not initialize websocket class " + i, e);
            }
        }
        Set<Class<?>> allScannedAnnotatedEndpoints = new HashSet<>();
        for (String i : annotatedEndpoints) {
            try {
                allScannedAnnotatedEndpoints.add(Class.forName(i, true, Thread.currentThread().getContextClassLoader()));
            } catch (Exception e) {
                log.error("Could not initialize websocket class " + i, e);
            }
        }
        Set<Class<?>> newAnnotatatedEndpoints = new HashSet<>();
        Set<ServerEndpointConfig> serverEndpointConfigurations = new HashSet<>();

        final Set<ServerApplicationConfig> configInstances = new HashSet<>();
        for (String clazzName : serverApplicationConfigClasses) {
            try {
                configInstances.add((ServerApplicationConfig) Class
                        .forName(clazzName, true, Thread.currentThread().getContextClassLoader()).newInstance());
            } catch (Exception e) {
                log.error("Could not initialize websocket config class " + clazzName, e);
            }
        }

        if (!configInstances.isEmpty()) {
            for (ServerApplicationConfig config : configInstances) {
                Set<Class<?>> returnedEndpoints = config.getAnnotatedEndpointClasses(allScannedAnnotatedEndpoints);
                if (returnedEndpoints != null) {
                    newAnnotatatedEndpoints.addAll(returnedEndpoints);
                }
                Set<ServerEndpointConfig> endpointConfigs = config.getEndpointConfigs(allScannedEndpointImplementations);
                if (endpointConfigs != null) {
                    serverEndpointConfigurations.addAll(endpointConfigs);
                }
            }
        } else {
            newAnnotatatedEndpoints.addAll(allScannedAnnotatedEndpoints);
        }

        //annotated endpoints first
        for (Class<?> endpoint : newAnnotatatedEndpoints) {
            if (endpoint != null) {
                container.addEndpoint(endpoint);
            }
        }

        for (final ServerEndpointConfig endpoint : serverEndpointConfigurations) {
            if (endpoint != null) {
                container.addEndpoint(endpoint);
            }
        }
        return container;
    }

    public Handler<RoutingContext> createHandler(BeanContainer beanContainer, Supplier<EventLoopGroup> eventLoopGroupSupplier,
            WebSocketDeploymentInfo info) throws DeploymentException {
        ManagedContext requestContext = Arc.container().requestContext();
        VertxServerWebSocketContainer container = new VertxServerWebSocketContainer(new ObjectIntrospecter() {
            @Override
            public <T> ObjectFactory<T> createInstanceFactory(Class<T> clazz) {
                BeanContainer.Factory<T> factory = beanContainer.instanceFactory(clazz);
                return new ObjectFactory<T>() {
                    @Override
                    public ObjectHandle<T> createInstance() {
                        BeanContainer.Instance<T> instance = factory.create();
                        return new ObjectHandle<T>() {
                            @Override
                            public T getInstance() {
                                return instance.get();
                            }

                            @Override
                            public void release() {
                                instance.close();
                            }
                        };
                    }
                };
            }
        }, Thread.currentThread().getContextClassLoader(), eventLoopGroupSupplier,
                Collections.singletonList(new ContextSetupHandler() {
                    @Override
                    public <T, C> Action<T, C> create(Action<T, C> action) {
                        return new Action<T, C>() {
                            @Override
                            public T call(C context) throws Exception {
                                requestContext.activate();
                                try {
                                    return action.call(context);
                                } finally {
                                    requestContext.terminate();
                                }
                            }
                        };
                    }
                }), false,
                new Supplier<Executor>() {
                    @Override
                    public Executor get() {
                        return ExecutorRecorder.getCurrent();
                    }
                });
        for (Class<?> i : info.getAnnotatedEndpoints()) {
            container.addEndpoint(i);
        }
        for (ServerEndpointConfig i : info.getProgramaticEndpoints()) {
            container.addEndpoint(i);
        }
        return new VertxWebSocketHandler(container, info);
    }

}
