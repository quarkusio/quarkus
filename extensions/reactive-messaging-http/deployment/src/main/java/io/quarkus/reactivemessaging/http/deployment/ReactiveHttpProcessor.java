package io.quarkus.reactivemessaging.http.deployment;

import java.lang.reflect.Modifier;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.reactivemessaging.http.runtime.QuarkusHttpConnector;
import io.quarkus.reactivemessaging.http.runtime.QuarkusWebSocketConnector;
import io.quarkus.reactivemessaging.http.runtime.ReactiveHttpHandlerBean;
import io.quarkus.reactivemessaging.http.runtime.ReactiveHttpRecorder;
import io.quarkus.reactivemessaging.http.runtime.ReactiveWebSocketHandlerBean;
import io.quarkus.reactivemessaging.http.runtime.config.HttpStreamConfig;
import io.quarkus.reactivemessaging.http.runtime.config.ReactiveHttpConfig;
import io.quarkus.reactivemessaging.http.runtime.config.WebSocketStreamConfig;
import io.quarkus.reactivemessaging.http.runtime.converters.JsonArrayConverter;
import io.quarkus.reactivemessaging.http.runtime.converters.JsonObjectConverter;
import io.quarkus.reactivemessaging.http.runtime.converters.ObjectConverter;
import io.quarkus.reactivemessaging.http.runtime.converters.StringConverter;
import io.quarkus.reactivemessaging.http.runtime.serializers.Serializer;
import io.quarkus.reactivemessaging.http.runtime.serializers.SerializerFactoryBase;
import io.quarkus.vertx.http.deployment.BodyHandlerBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class ReactiveHttpProcessor {

    @Inject
    BuildProducer<RouteBuildItem> routeProducer;

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void registerHttpConnector(BuildProducer<AdditionalBeanBuildItem> beanProducer,
            BuildProducer<GeneratedBeanBuildItem> generatedBeanProducer,
            BodyHandlerBuildItem bodyHandler,
            ReactiveHttpRecorder recorder) {
        beanProducer.produce(new AdditionalBeanBuildItem(QuarkusHttpConnector.class));
        beanProducer.produce(new AdditionalBeanBuildItem(QuarkusWebSocketConnector.class));
        beanProducer.produce(new AdditionalBeanBuildItem(ReactiveHttpConfig.class));
        beanProducer.produce(new AdditionalBeanBuildItem(ReactiveHttpHandlerBean.class));
        beanProducer.produce(new AdditionalBeanBuildItem(ReactiveWebSocketHandlerBean.class));

        beanProducer.produce(new AdditionalBeanBuildItem(JsonArrayConverter.class));
        beanProducer.produce(new AdditionalBeanBuildItem(JsonObjectConverter.class));
        beanProducer.produce(new AdditionalBeanBuildItem(ObjectConverter.class));
        beanProducer.produce(new AdditionalBeanBuildItem(StringConverter.class));

        List<HttpStreamConfig> httpConfigs = ReactiveHttpConfig.readIncomingHttpConfigs();
        List<WebSocketStreamConfig> wsConfigs = ReactiveHttpConfig.readIncomingWebSocketConfigs();

        if (!httpConfigs.isEmpty()) {
            Handler<RoutingContext> handler = recorder.createHttpHandler();

            httpConfigs.stream()
                    .map(HttpStreamConfig::path)
                    .distinct()
                    .forEach(path -> {
                        routeProducer.produce(new RouteBuildItem(path, bodyHandler.getHandler()));
                        routeProducer.produce(new RouteBuildItem(path, handler));
                    });
        }
        if (!wsConfigs.isEmpty()) {
            Handler<RoutingContext> handler = recorder.createWebSocketeHandler();

            wsConfigs.stream()
                    .map(WebSocketStreamConfig::path)
                    .distinct()
                    .forEach(path -> routeProducer.produce(new RouteBuildItem(path, handler)));
        }

        initSerializers(ReactiveHttpConfig.readSerializers(), generatedBeanProducer);
    }

    private void initSerializers(List<String> serializers, BuildProducer<GeneratedBeanBuildItem> generatedBeans) {
        ClassOutput classOutput = new GeneratedBeanGizmoAdaptor(generatedBeans);
        try (ClassCreator factory = ClassCreator.builder().classOutput(classOutput)
                .className("io.quarkus.reactivemessaging.http.runtime.serializers.SerializerFactory")
                .superClass(SerializerFactoryBase.class)
                .build()) {
            factory.addAnnotation(ApplicationScoped.class);

            try (MethodCreator init = factory.getMethodCreator("initAdditionalSerializers", void.class)) {
                init.setModifiers(Modifier.PROTECTED);
                MethodDescriptor addSerializer = MethodDescriptor.ofMethod(SerializerFactoryBase.class, "addSerializer",
                        void.class, String.class, Serializer.class);

                for (String serializerName : serializers) {
                    ResultHandle serializer = init.newInstance(MethodDescriptor.ofConstructor(serializerName));
                    init.invokeVirtualMethod(addSerializer, init.getThis(), init.load(serializerName), serializer);
                }
                init.returnValue(null);
            }
        }
    }
}
