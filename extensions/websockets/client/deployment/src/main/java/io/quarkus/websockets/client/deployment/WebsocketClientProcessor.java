package io.quarkus.websockets.client.deployment;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.Endpoint;
import jakarta.websocket.server.ServerApplicationConfig;
import jakarta.websocket.server.ServerContainer;
import jakarta.websocket.server.ServerEndpointConfig;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;

import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.core.deployment.action.ActionBuilder;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.Phase;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ExecutorBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.NativeImageFeatureBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.undertow.deployment.ServletContextAttributeBuildItem;
import io.quarkus.websockets.client.runtime.DisableLoggingFeature;
import io.quarkus.websockets.client.runtime.ServerWebSocketContainerFactory;
import io.quarkus.websockets.client.runtime.WebsocketCoreRecorder;
import io.undertow.websockets.DefaultContainerConfigurator;
import io.undertow.websockets.ServerWebSocketContainer;
import io.undertow.websockets.UndertowContainerProvider;
import io.undertow.websockets.WebSocketDeploymentInfo;

public class WebsocketClientProcessor {

    private static final DotName CLIENT_ENDPOINT = DotName.createSimple(ClientEndpoint.class.getName());
    private static final DotName SERVER_APPLICATION_CONFIG = DotName.createSimple(ServerApplicationConfig.class.getName());
    private static final DotName ENDPOINT = DotName.createSimple(Endpoint.class.getName());

    @BuildStep
    NativeImageFeatureBuildItem nativeImageFeature() {
        return new NativeImageFeatureBuildItem(DisableLoggingFeature.class);
    }

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
    public ServerWebSocketContainerBuildItem deploy(final CombinedIndexBuildItem indexBuildItem,
            ActionBuilder action,
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
                ReflectiveClassBuildItem.builder(annotated.toArray(new String[annotated.size()]))
                        .reason(getClass().getName())
                        .methods().build());

        registerCodersForReflection(reflection, index.getAnnotations(CLIENT_ENDPOINT));

        reflection.produce(
                ReflectiveClassBuildItem.builder(ClientEndpointConfig.Configurator.class.getName()).methods().fields()
                        .build());

        // extract build-time config values into locals (BUILD_TIME config cannot be captured directly)
        int maxFrameSize = websocketConfig.maxFrameSize();
        boolean dispatchToWorker = websocketConfig.dispatchToWorker();

        // convert annotated/endpoints/config to immutable sets for lambda capture
        Set<String> capturedAnnotated = Set.copyOf(annotated);
        Set<String> capturedEndpoints = Set.copyOf(endpoints);
        Set<String> capturedConfig = Set.copyOf(config);

        action
                .forService(WebSocketDeploymentInfo.class)
                .atPhase(Phase.STATIC_INIT)
                .action(ctx -> WebsocketCoreRecorder.createDeploymentInfo(
                        capturedAnnotated, capturedEndpoints, capturedConfig,
                        maxFrameSize, dispatchToWorker));
        infoBuildItemBuildProducer.produce(new WebSocketDeploymentInfoBuildItem());

        // if the server module didn't register a factory, register the default
        if (factoryBuildItem.isEmpty()) {
            action
                    .forService(ServerWebSocketContainerFactory.class)
                    .atPhase(Phase.STATIC_INIT)
                    .action(ctx -> (ServerWebSocketContainerFactory) ServerWebSocketContainer::new);
        }

        action
                .forService(ServerWebSocketContainer.class)
                .atPhase(Phase.STATIC_INIT)
                .require(WebSocketDeploymentInfo.class)
                .require(ServerWebSocketContainerFactory.class)
                .require(BeanContainer.class)
                .action((ctx, info, factory, beanContainer) -> WebsocketCoreRecorder.createServerContainer(beanContainer, info,
                        factory));

        servletContextAttributeBuildItemBuildProducer
                .produce(new ServletContextAttributeBuildItem(ServerContainer.class.getName(),
                        action.staticInitServiceAsRuntimeValue(ServerWebSocketContainer.class)));
        return new ServerWebSocketContainerBuildItem();
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
                reflection
                        .produce(ReflectiveClassBuildItem.builder(type.name().toString()).methods().build());
            }
        }
    }

    @BuildStep
    ServiceStartBuildItem setupWorker(ActionBuilder action, ExecutorBuildItem exec) {
        action
                .forService("io.quarkus.websocket.worker-setup")
                .require(ScheduledExecutorService.class)
                .action((ctx, executor) -> WebsocketCoreRecorder.setupWorker(executor));
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
