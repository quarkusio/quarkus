package org.jboss.shamrock.websockets.runtime;

import java.util.HashSet;
import java.util.Set;

import javax.websocket.Endpoint;
import javax.websocket.server.ServerApplicationConfig;
import javax.websocket.server.ServerEndpointConfig;

import org.jboss.logging.Logger;
import org.jboss.shamrock.runtime.Template;

import io.undertow.websockets.jsr.WebSocketDeploymentInfo;

@Template
public class WebsocketTemplate {

    private static final Logger log = Logger.getLogger(WebsocketTemplate.class);

    public WebSocketDeploymentInfo createDeploymentInfo(Set<String> annotatedEndpoints, Set<String> endpoints, Set<String> serverApplicationConfigClasses) {
        WebSocketDeploymentInfo container = new WebSocketDeploymentInfo();
        Set<Class<? extends Endpoint>> allScannedEndpointImplementations = new HashSet<>();
        for (String i : endpoints) {
            try {
                allScannedEndpointImplementations.add((Class<? extends Endpoint>) Class.forName(i));
            } catch (Exception e) {
                log.error("Could not initialize websocket class " + i, e);
            }
        }
        Set<Class<?>> allScannedAnnotatedEndpoints = new HashSet<>();
        for (String i : annotatedEndpoints) {
            try {
                allScannedAnnotatedEndpoints.add(Class.forName(i));
            } catch (Exception e) {
                log.error("Could not initialize websocket class " + i, e);
            }
        }
        Set<Class<?>> newAnnotatatedEndpoints = new HashSet<>();
        Set<ServerEndpointConfig> serverEndpointConfigurations = new HashSet<>();


        final Set<ServerApplicationConfig> configInstances = new HashSet<>();
        for (String clazzName : serverApplicationConfigClasses) {
            try {
                configInstances.add((ServerApplicationConfig) Class.forName(clazzName).newInstance());
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
