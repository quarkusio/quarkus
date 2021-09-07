package io.quarkus.undertow.websockets.client.deployment;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.websocket.ClientEndpoint;
import javax.websocket.ClientEndpointConfig;
import javax.websocket.ContainerProvider;
import javax.websocket.Endpoint;
import javax.websocket.server.ServerApplicationConfig;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;

import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ExecutorBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.undertow.deployment.ServletContextAttributeBuildItem;
import io.quarkus.undertow.websockets.client.runtime.WebsocketCoreRecorder;
import io.undertow.websockets.DefaultContainerConfigurator;
import io.undertow.websockets.ServerWebSocketContainer;
import io.undertow.websockets.UndertowContainerProvider;
import io.undertow.websockets.WebSocketDeploymentInfo;

public class WebsocketClientProcessor {

    private static final DotName CLIENT_ENDPOINT = DotName.createSimple(ClientEndpoint.class.getName());
    private static final DotName SERVER_APPLICATION_CONFIG = DotName.createSimple(ServerApplicationConfig.class.getName());
    private static final DotName ENDPOINT = DotName.createSimple(Endpoint.class.getName());

    @BuildStep
    void holdConfig(BuildProducer<FeatureBuildItem> feature) {
        feature.produce(new FeatureBuildItem(Feature.WEBSOCKETS_CLIENT));
    }

    @BuildStep
    void scanForAnnotatedEndpoints(CombinedIndexBuildItem indexBuildItem,
            BuildProducer<AnnotatedWebsocketEndpointBuildItem> annotatedProducer) {

        final IndexView index = indexBuildItem.getIndex();
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
    public ServerWebSocketContainerBuildItem deploy(final CombinedIndexBuildItem indexBuildItem,
            WebsocketCoreRecorder recorder,
            BuildProducer<ReflectiveClassBuildItem> reflection,
            List<AnnotatedWebsocketEndpointBuildItem> annotatedEndpoints,
            BeanContainerBuildItem beanContainerBuildItem,
            WebsocketConfig websocketConfig,
            BuildProducer<WebSocketDeploymentInfoBuildItem> infoBuildItemBuildProducer,
            Optional<ServerWebSocketContainerFactoryBuildItem> factoryBuildItem,
            BuildProducer<ServletContextAttributeBuildItem> servletContextAttributeBuildItemBuildProducer) throws Exception {

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

        registerCodersForReflection(reflection, index.getAnnotations(CLIENT_ENDPOINT));

        reflection.produce(
                new ReflectiveClassBuildItem(true, true, ClientEndpointConfig.Configurator.class.getName()));

        RuntimeValue<WebSocketDeploymentInfo> deploymentInfo = recorder.createDeploymentInfo(annotated, endpoints, config,
                websocketConfig.maxFrameSize,
                websocketConfig.dispatchToWorker);
        infoBuildItemBuildProducer.produce(new WebSocketDeploymentInfoBuildItem(deploymentInfo));
        RuntimeValue<ServerWebSocketContainer> serverContainer = recorder.createServerContainer(
                beanContainerBuildItem.getValue(),
                deploymentInfo,
                factoryBuildItem.map(ServerWebSocketContainerFactoryBuildItem::getFactory).orElse(null));
        servletContextAttributeBuildItemBuildProducer
                .produce(new ServletContextAttributeBuildItem(ServerContainer.class.getName(), serverContainer));
        return new ServerWebSocketContainerBuildItem(
                serverContainer);
    }

    public static void registerCodersForReflection(BuildProducer<ReflectiveClassBuildItem> reflection,
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

    static void registerForReflection(BuildProducer<ReflectiveClassBuildItem> reflection, AnnotationValue types) {
        if (types != null && types.asClassArray() != null) {
            for (Type type : types.asClassArray()) {
                reflection.produce(new ReflectiveClassBuildItem(true, false, type.name().toString()));
            }
        }
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    ServiceStartBuildItem setupWorker(WebsocketCoreRecorder recorder, ExecutorBuildItem exec) {
        recorder.setupWorker(exec.getExecutorProxy());
        return new ServiceStartBuildItem("Websockets");
    }

    @BuildStep
    ServiceProviderBuildItem registerContainerProviderService() {
        return new ServiceProviderBuildItem(ContainerProvider.class.getName(),
                UndertowContainerProvider.class.getName());
    }

    @BuildStep
    ServiceProviderBuildItem registerConfiguratorServiceProvider() {
        return new ServiceProviderBuildItem(ServerEndpointConfig.Configurator.class.getName(),
                DefaultContainerConfigurator.class.getName());
    }

    @BuildStep
    void beanDefiningAnnotations(BuildProducer<BeanDefiningAnnotationBuildItem> annotations) {
        annotations.produce(new BeanDefiningAnnotationBuildItem(ENDPOINT));
        annotations.produce(new BeanDefiningAnnotationBuildItem(CLIENT_ENDPOINT));
    }
}
