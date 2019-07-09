package io.quarkus.undertow.websockets.deployment;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.websocket.ClientEndpoint;
import javax.websocket.ClientEndpointConfig;
import javax.websocket.ContainerProvider;
import javax.websocket.Endpoint;
import javax.websocket.server.ServerApplicationConfig;
import javax.websocket.server.ServerEndpoint;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;

import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.substrate.ServiceProviderBuildItem;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.undertow.deployment.ServletContextAttributeBuildItem;
import io.quarkus.undertow.deployment.UndertowBuildItem;
import io.quarkus.undertow.websockets.runtime.UndertowWebsocketRecorder;
import io.undertow.websockets.jsr.JsrWebSocketFilter;
import io.undertow.websockets.jsr.UndertowContainerProvider;
import io.undertow.websockets.jsr.WebSocketDeploymentInfo;

public class UndertowWebsocketProcessor {

    private static final DotName SERVER_ENDPOINT = DotName.createSimple(ServerEndpoint.class.getName());
    private static final DotName CLIENT_ENDPOINT = DotName.createSimple(ClientEndpoint.class.getName());
    private static final DotName SERVER_APPLICATION_CONFIG = DotName.createSimple(ServerApplicationConfig.class.getName());
    private static final DotName ENDPOINT = DotName.createSimple(Endpoint.class.getName());

    @BuildStep
    void scanForAnnotatedEndpoints(CombinedIndexBuildItem indexBuildItem,
            BuildProducer<AnnotatedWebsocketEndpointBuildItem> annotatedProducer) {

        final IndexView index = indexBuildItem.getIndex();

        final Collection<AnnotationInstance> serverEndpoints = index.getAnnotations(SERVER_ENDPOINT);
        for (AnnotationInstance endpoint : serverEndpoints) {
            if (endpoint.target() instanceof ClassInfo) {
                ClassInfo clazz = (ClassInfo) endpoint.target();
                if (!Modifier.isAbstract(clazz.flags())) {
                    annotatedProducer.produce(new AnnotatedWebsocketEndpointBuildItem(clazz.name().toString(), false));
                }
            }
        }

        final Collection<AnnotationInstance> clientEndpoints = index.getAnnotations(CLIENT_ENDPOINT);
        for (AnnotationInstance endpoint : clientEndpoints) {
            if (endpoint.target() instanceof ClassInfo) {
                ClassInfo clazz = (ClassInfo) endpoint.target();
                if (!Modifier.isAbstract(clazz.flags())) {
                    annotatedProducer.produce(new AnnotatedWebsocketEndpointBuildItem(clazz.name().toString(), true));
                }
            }
        }

    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    public ServletContextAttributeBuildItem deploy(final CombinedIndexBuildItem indexBuildItem,
            UndertowWebsocketRecorder recorder,
            BuildProducer<ReflectiveClassBuildItem> reflection, BuildProducer<FeatureBuildItem> feature,
            List<AnnotatedWebsocketEndpointBuildItem> annotatedEndpoints) throws Exception {

        feature.produce(new FeatureBuildItem(FeatureBuildItem.UNDERTOW_WEBSOCKETS));

        final Set<String> endpoints = new HashSet<>();
        final Set<String> config = new HashSet<>();

        final IndexView index = indexBuildItem.getIndex();
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
        Set<String> annotated = new HashSet<>();
        for (AnnotatedWebsocketEndpointBuildItem i : annotatedEndpoints) {
            annotated.add(i.className);
        }
        reflection.produce(
                new ReflectiveClassBuildItem(true, false, annotated.toArray(new String[annotated.size()])));
        reflection.produce(new ReflectiveClassBuildItem(false, false, JsrWebSocketFilter.class.getName()));

        registerCodersForReflection(reflection, index.getAnnotations(SERVER_ENDPOINT));
        registerCodersForReflection(reflection, index.getAnnotations(CLIENT_ENDPOINT));

        reflection.produce(
                new ReflectiveClassBuildItem(true, true, ClientEndpointConfig.Configurator.class.getName()));

        return new ServletContextAttributeBuildItem(WebSocketDeploymentInfo.ATTRIBUTE_NAME,
                recorder.createDeploymentInfo(annotated, endpoints, config));
    }

    private void registerCodersForReflection(BuildProducer<ReflectiveClassBuildItem> reflection,
            Collection<AnnotationInstance> endpoints) {
        for (AnnotationInstance endpoint : endpoints) {
            if (endpoint.target() instanceof ClassInfo) {
                ClassInfo clazz = (ClassInfo) endpoint.target();
                if (!Modifier.isAbstract(clazz.flags())) {
                    registerForReflection(reflection, endpoint.value("encoders"));
                    registerForReflection(reflection, endpoint.value("decoders"));
                }
            }
        }
    }

    private void registerForReflection(BuildProducer<ReflectiveClassBuildItem> reflection, AnnotationValue types) {
        if (types != null && types.asClassArray() != null) {
            for (Type type : types.asClassArray()) {
                reflection.produce(new ReflectiveClassBuildItem(true, false, type.name().toString()));
            }
        }
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    ServiceStartBuildItem setupWorker(UndertowWebsocketRecorder recorder, UndertowBuildItem undertow) {
        recorder.setupWorker(undertow.getUndertow());
        return new ServiceStartBuildItem("Websockets");
    }

    @BuildStep
    ServiceProviderBuildItem registerContainerProviderService() {
        return new ServiceProviderBuildItem(ContainerProvider.class.getName(),
                UndertowContainerProvider.class.getName());
    }

    @BuildStep
    void beanDefiningAnnotations(BuildProducer<BeanDefiningAnnotationBuildItem> annotations) {
        annotations.produce(new BeanDefiningAnnotationBuildItem(SERVER_ENDPOINT));
        annotations.produce(new BeanDefiningAnnotationBuildItem(ENDPOINT));
        annotations.produce(new BeanDefiningAnnotationBuildItem(CLIENT_ENDPOINT));
    }

    @ConfigRoot
    static class HotReloadConfig {

        /**
         * The security key for remote hot deployment
         */
        String password;

        /**
         * The remote URL to connect to
         */
        String url;
    }
}
