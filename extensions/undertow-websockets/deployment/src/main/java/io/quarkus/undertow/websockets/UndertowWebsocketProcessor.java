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

package io.quarkus.undertow.websockets;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.websocket.ClientEndpoint;
import javax.websocket.Endpoint;
import javax.websocket.server.ServerApplicationConfig;
import javax.websocket.server.ServerEndpoint;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.undertow.ServletContextAttributeBuildItem;
import io.quarkus.undertow.UndertowBuildItem;
import io.quarkus.undertow.websockets.runtime.UndertowWebsocketTemplate;

import io.undertow.websockets.jsr.JsrWebSocketFilter;
import io.undertow.websockets.jsr.WebSocketDeploymentInfo;

public class UndertowWebsocketProcessor {

    private static final DotName SERVER_ENDPOINT = DotName.createSimple(ServerEndpoint.class.getName());
    private static final DotName CLIENT_ENDPOINT = DotName.createSimple(ClientEndpoint.class.getName());
    private static final DotName SERVER_APPLICATION_CONFIG = DotName.createSimple(ServerApplicationConfig.class.getName());
    private static final DotName ENDPOINT = DotName.createSimple(Endpoint.class.getName());


    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    public ServletContextAttributeBuildItem deploy(final CombinedIndexBuildItem indexBuildItem, UndertowWebsocketTemplate template,
            BuildProducer<ReflectiveClassBuildItem> reflection, BuildProducer<FeatureBuildItem> feature) throws Exception {

        feature.produce(new FeatureBuildItem(FeatureBuildItem.UNDERTOW_WEBSOCKETS));

        final Set<String> annotatedEndpoints = new HashSet<>();
        final Set<String> endpoints = new HashSet<>();
        final Set<String> config = new HashSet<>();

        final IndexView index = indexBuildItem.getIndex();

        final Collection<AnnotationInstance> serverEndpoints = index.getAnnotations(SERVER_ENDPOINT);
        for (AnnotationInstance endpoint : serverEndpoints) {
            if (endpoint.target() instanceof ClassInfo) {
                ClassInfo clazz = (ClassInfo) endpoint.target();
                if (!Modifier.isAbstract(clazz.flags())) {
                    annotatedEndpoints.add(clazz.name().toString());
                }
            }
        }

        final Collection<AnnotationInstance> clientEndpoints = index.getAnnotations(CLIENT_ENDPOINT);
        for (AnnotationInstance endpoint : clientEndpoints) {
            if (endpoint.target() instanceof ClassInfo) {
                ClassInfo clazz = (ClassInfo) endpoint.target();
                if (!Modifier.isAbstract(clazz.flags())) {
                    annotatedEndpoints.add(clazz.name().toString());
                }
            }
        }

        final Collection<ClassInfo> subclasses = index.getAllKnownImplementors(SERVER_APPLICATION_CONFIG);

        for (final ClassInfo clazz : subclasses) {
            if (!Modifier.isAbstract(clazz.flags())) {
                config.add(clazz.name().toString());
            }
        }

        final Collection<ClassInfo> epClasses = index.getAllKnownSubclasses(ENDPOINT);

        for (final ClassInfo clazz : epClasses) {
            if (!Modifier.isAbstract(clazz.flags())) {
                endpoints.add(clazz.name().toString());
            }
        }
        if (annotatedEndpoints.isEmpty() &&
                endpoints.isEmpty() &&
                config.isEmpty()) {
            return null;
        }
        reflection.produce(new ReflectiveClassBuildItem(true, false , annotatedEndpoints.toArray(new String[annotatedEndpoints.size()])));
        reflection.produce(new ReflectiveClassBuildItem(false, false , JsrWebSocketFilter.class.getName()));
        
        return new ServletContextAttributeBuildItem(WebSocketDeploymentInfo.ATTRIBUTE_NAME, template.createDeploymentInfo(annotatedEndpoints, endpoints, config));
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    ServiceStartBuildItem setupWorker(UndertowWebsocketTemplate template, UndertowBuildItem undertow) {
        template.setupWorker(undertow.getUndertow());
        return new ServiceStartBuildItem("Websockets");
    }
}
