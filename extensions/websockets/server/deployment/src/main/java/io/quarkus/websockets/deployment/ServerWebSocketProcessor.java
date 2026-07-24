package io.quarkus.websockets.deployment;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.websocket.Decoder;
import jakarta.websocket.Encoder;
import jakarta.websocket.server.ServerEndpoint;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.core.deployment.action.ActionBuilder;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.Phase;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;
import io.quarkus.deployment.util.JandexUtil;
import io.quarkus.vertx.http.deployment.FilterBuildItem;
import io.quarkus.vertx.http.runtime.security.SecurityHandlerPriorities;
import io.quarkus.websockets.client.deployment.AnnotatedWebsocketEndpointBuildItem;
import io.quarkus.websockets.client.deployment.ServerWebSocketContainerBuildItem;
import io.quarkus.websockets.client.deployment.ServerWebSocketContainerFactoryBuildItem;
import io.quarkus.websockets.client.deployment.WebSocketDeploymentInfoBuildItem;
import io.quarkus.websockets.client.deployment.WebsocketClientProcessor;
import io.quarkus.websockets.client.runtime.ServerWebSocketContainerFactory;
import io.quarkus.websockets.runtime.WebsocketServerRecorder;
import io.undertow.websockets.ServerWebSocketContainer;
import io.undertow.websockets.WebSocketDeploymentInfo;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class ServerWebSocketProcessor {

    private static final DotName SERVER_ENDPOINT = DotName.createSimple(ServerEndpoint.class.getName());

    public static final Collection<DotName> CODECS = List.of(
            Decoder.TextStream.class,
            Decoder.Text.class,
            Decoder.BinaryStream.class,
            Decoder.Binary.class,
            Encoder.TextStream.class,
            Encoder.Text.class,
            Encoder.BinaryStream.class,
            Encoder.Binary.class).stream().map(Class::getName).map(DotName::createSimple)
            .collect(Collectors.toList());

    @BuildStep
    void holdConfig(BuildProducer<FeatureBuildItem> feature) {
        feature.produce(new FeatureBuildItem(Feature.WEBSOCKETS));
    }

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
    }

    @BuildStep
    void buildIndexDependencies(BuildProducer<IndexDependencyBuildItem> indexDependencyProduer) {
        indexDependencyProduer.produce(new IndexDependencyBuildItem("jakarta.websocket", "jakarta.websocket-client-api"));
    }

    @BuildStep
    void scanForCodecs(CombinedIndexBuildItem index,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchyBuildItemProducer) {
        CODECS.stream().forEach(
                codec -> index.getIndex().getAllKnownImplementors(codec).stream()
                        .filter(implementor -> !Modifier.isAbstract(implementor.flags()))
                        .forEach(implementor -> JandexUtil.resolveTypeParameters(
                                implementor.name(),
                                codec, index.getIndex()).forEach(
                                        typeParameter -> reflectiveHierarchyBuildItemProducer.produce(
                                                ReflectiveHierarchyBuildItem.builder(typeParameter).build()))));
    }

    @BuildStep
    public ServerWebSocketContainerFactoryBuildItem factory(ActionBuilder action) {
        action
                .forService(ServerWebSocketContainerFactory.class)
                .atPhase(Phase.STATIC_INIT)
                .action(ctx -> WebsocketServerRecorder.createFactory());
        return new ServerWebSocketContainerFactoryBuildItem();
    }

    @SuppressWarnings("unchecked")
    @BuildStep
    public FilterBuildItem deploy(final CombinedIndexBuildItem indexBuildItem,
            ActionBuilder action,
            BuildProducer<ReflectiveClassBuildItem> reflection,
            Optional<WebSocketDeploymentInfoBuildItem> webSocketDeploymentInfoBuildItem,
            Optional<ServerWebSocketContainerBuildItem> serverWebSocketContainerBuildItem) throws Exception {
        if (webSocketDeploymentInfoBuildItem.isEmpty()) {
            return null;
        }

        final IndexView index = indexBuildItem.getIndex();
        WebsocketClientProcessor.registerCodersForReflection(reflection, index.getAnnotations(SERVER_ENDPOINT));

        action
                .forService(Handler.class, "io.quarkus.websocket.filter")
                .require(WebSocketDeploymentInfo.class)
                .require(ServerWebSocketContainer.class)
                .action((ctx, info, container) -> WebsocketServerRecorder.createHandler(info, container));

        int priority = 1 + SecurityHandlerPriorities.AUTHORIZATION;
        Handler<RoutingContext> handler = (Handler<RoutingContext>) action.serviceAsRecorderValue(
                Handler.class, "io.quarkus.websocket.filter");
        return new FilterBuildItem(handler, priority);
    }

    @BuildStep
    void beanDefiningAnnotations(BuildProducer<BeanDefiningAnnotationBuildItem> annotations) {
        annotations.produce(new BeanDefiningAnnotationBuildItem(SERVER_ENDPOINT));
    }

}
