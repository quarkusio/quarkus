/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.undertow.websockets.runtime;

import java.util.HashSet;
import java.util.Set;

import javax.websocket.Endpoint;
import javax.websocket.server.ServerApplicationConfig;
import javax.websocket.server.ServerEndpointConfig;

import org.jboss.logging.Logger;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Template;
import io.undertow.Undertow;
import io.undertow.websockets.jsr.WebSocketDeploymentInfo;

@Template
public class UndertowWebsocketTemplate {

    private static final Logger log = Logger.getLogger(UndertowWebsocketTemplate.class);

    public void setupWorker(RuntimeValue<Undertow> undertow) {
        WorkerSupplier.worker = undertow.getValue().getWorker();
    }

    @SuppressWarnings("unchecked")
    public WebSocketDeploymentInfo createDeploymentInfo(Set<String> annotatedEndpoints, Set<String> endpoints,
            Set<String> serverApplicationConfigClasses) {
        WebSocketDeploymentInfo container = new WebSocketDeploymentInfo();
        container.setWorker(new WorkerSupplier());
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
