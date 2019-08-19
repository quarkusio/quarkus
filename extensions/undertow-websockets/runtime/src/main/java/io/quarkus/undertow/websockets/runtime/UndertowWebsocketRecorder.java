package io.quarkus.undertow.websockets.runtime;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;

import javax.websocket.Endpoint;
import javax.websocket.server.ServerApplicationConfig;
import javax.websocket.server.ServerEndpointConfig;

import org.jboss.logging.Logger;

import io.quarkus.runtime.annotations.Recorder;
import io.undertow.websockets.jsr.WebSocketDeploymentInfo;

@Recorder
public class UndertowWebsocketRecorder {

    private static final Logger log = Logger.getLogger(UndertowWebsocketRecorder.class);

    public void setupWorker(Executor executor) {
        ExecutorSupplier.executor = executor;
    }

    @SuppressWarnings("unchecked")
    public WebSocketDeploymentInfo createDeploymentInfo(Set<String> annotatedEndpoints, Set<String> endpoints,
            Set<String> serverApplicationConfigClasses) {
        WebSocketDeploymentInfo container = new WebSocketDeploymentInfo();
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

}
