package io.quarkus.websockets.client.runtime;

import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import javax.enterprise.inject.Instance;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.server.ServerApplicationConfig;
import javax.websocket.server.ServerEndpointConfig;

import org.jboss.logging.Logger;

import io.netty.channel.EventLoopGroup;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.vertx.core.runtime.VertxCoreRecorder;
import io.undertow.websockets.ServerWebSocketContainer;
import io.undertow.websockets.UndertowContainerProvider;
import io.undertow.websockets.UndertowSession;
import io.undertow.websockets.WebSocketDeploymentInfo;
import io.undertow.websockets.util.ContextSetupHandler;
import io.undertow.websockets.util.ObjectFactory;
import io.undertow.websockets.util.ObjectHandle;
import io.undertow.websockets.util.ObjectIntrospecter;
import io.vertx.core.impl.VertxInternal;

@Recorder
public class WebsocketCoreRecorder {

    private static final Logger log = Logger.getLogger(WebsocketCoreRecorder.class);

    public void setupWorker(Executor executor) {
        ExecutorSupplier.executor = executor;
    }

    @SuppressWarnings("unchecked")
    public RuntimeValue<WebSocketDeploymentInfo> createDeploymentInfo(Set<String> annotatedEndpoints, Set<String> endpoints,
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
        Set<Class<?>> newAnnotatedEndpoints = new HashSet<>();
        Set<ServerEndpointConfig> serverEndpointConfigurations = new HashSet<>();

        final Set<ServerApplicationConfig> configInstances = new HashSet<>();
        for (String clazzName : serverApplicationConfigClasses) {
            try {
                configInstances.add((ServerApplicationConfig) Class
                        .forName(clazzName, true, Thread.currentThread().getContextClassLoader()).getDeclaredConstructor()
                        .newInstance());
            } catch (Exception e) {
                log.error("Could not initialize websocket config class " + clazzName, e);
            }
        }

        if (!configInstances.isEmpty()) {
            for (ServerApplicationConfig config : configInstances) {
                Set<Class<?>> returnedEndpoints = config.getAnnotatedEndpointClasses(allScannedAnnotatedEndpoints);
                if (returnedEndpoints != null) {
                    newAnnotatedEndpoints.addAll(returnedEndpoints);
                }
                Set<ServerEndpointConfig> endpointConfigs = config.getEndpointConfigs(allScannedEndpointImplementations);
                if (endpointConfigs != null) {
                    serverEndpointConfigurations.addAll(endpointConfigs);
                }
            }
        } else {
            newAnnotatedEndpoints.addAll(allScannedAnnotatedEndpoints);
        }

        //annotated endpoints first
        for (Class<?> endpoint : newAnnotatedEndpoints) {
            if (endpoint != null) {
                container.addEndpoint(endpoint);
            }
        }

        for (final ServerEndpointConfig endpoint : serverEndpointConfigurations) {
            if (endpoint != null) {
                container.addEndpoint(endpoint);
            }
        }
        return new RuntimeValue<>(container);
    }

    public RuntimeValue<ServerWebSocketContainer> createServerContainer(BeanContainer beanContainer,
            RuntimeValue<WebSocketDeploymentInfo> infoVal, ServerWebSocketContainerFactory serverContainerFactory)
            throws DeploymentException {
        WebSocketDeploymentInfo info = infoVal.getValue();
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        ManagedContext requestContext = Arc.container().requestContext();
        if (serverContainerFactory == null) {
            serverContainerFactory = ServerWebSocketContainer::new;
        }
        Instance<CurrentIdentityAssociation> currentIdentityAssociation = Arc.container()
                .select(CurrentIdentityAssociation.class);

        ServerWebSocketContainer container = serverContainerFactory.create(new ObjectIntrospecter() {
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
        }, Thread.currentThread().getContextClassLoader(), new Supplier<EventLoopGroup>() {
            @Override
            public EventLoopGroup get() {
                return ((VertxInternal) VertxCoreRecorder.getVertx().get()).getEventLoopGroup();
            }
        },
                Collections.singletonList(new ContextSetupHandler() {
                    @Override
                    public <T, C> Action<T, C> create(Action<T, C> action) {
                        return new Action<T, C>() {

                            CurrentIdentityAssociation getCurrentIdentityAssociation() {
                                if (currentIdentityAssociation.isResolvable()) {
                                    return currentIdentityAssociation.get();
                                }
                                return null;
                            }

                            @Override
                            public T call(C context, UndertowSession session) throws Exception {
                                ClassLoader old = Thread.currentThread().getContextClassLoader();
                                Thread.currentThread().setContextClassLoader(cl);
                                boolean required = !requestContext.isActive();
                                if (required) {
                                    requestContext.activate();
                                    Principal p = session.getUserPrincipal();
                                    if (p instanceof WebSocketPrincipal) {
                                        var current = getCurrentIdentityAssociation();
                                        if (current != null) {
                                            current.setIdentity(((WebSocketPrincipal) p).getSecurityIdentity());
                                        }
                                    }
                                }
                                try {
                                    return action.call(context, session);
                                } finally {
                                    try {
                                        if (required) {
                                            requestContext.terminate();
                                        }
                                    } finally {
                                        Thread.currentThread().setContextClassLoader(old);
                                    }
                                }
                            }
                        };
                    }
                }),
                info.isDispatchToWorkerThread(),
                null,
                null,
                info.getExecutor(),
                Collections.emptyList(),
                info.getMaxFrameSize(), new Supplier<Principal>() {
                    @Override
                    public Principal get() {
                        if (currentIdentityAssociation.isResolvable()) {
                            return new WebSocketPrincipal(currentIdentityAssociation.get().getIdentity());
                        }
                        return null;
                    }
                });
        for (Class<?> i : info.getAnnotatedEndpoints()) {
            container.addEndpoint(i);
        }
        for (ServerEndpointConfig i : info.getProgramaticEndpoints()) {
            container.addEndpoint(i);
        }
        UndertowContainerProvider.setDefaultContainer(container);
        return new RuntimeValue<>(container);
    }

}
