package io.quarkus.reactivemessaging.http.deployment;

import static io.quarkus.arc.processor.DotNames.OBJECT;
import static io.quarkus.arc.processor.DotNames.STRING;
import static java.util.Arrays.asList;

import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.streams.operators.ProcessorBuilder;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.SubscriberBuilder;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;
import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
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
import io.smallrye.mutiny.Multi;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class ReactiveHttpProcessor {

    private static final DotName JSON_ARRAY = DotName.createSimple(JsonArray.class.getName());
    private static final DotName JSON_OBJECT = DotName.createSimple(JsonObject.class.getName());
    private static final DotName MESSAGE = DotName.createSimple(Message.class.getName());
    private static final DotName MULTI = DotName.createSimple(Multi.class.getName());
    private static final DotName PROCESSOR = DotName.createSimple(Processor.class.getName());
    private static final DotName PROCESSOR_BUILDER = DotName.createSimple(ProcessorBuilder.class.getName());
    private static final DotName PUBLISHER = DotName.createSimple(Publisher.class.getName());
    private static final DotName PUBLISHER_BUILDER = DotName.createSimple(PublisherBuilder.class.getName());
    private static final DotName SUBSCRIBER = DotName.createSimple(Subscriber.class.getName());
    private static final DotName SUBSCRIBER_BUILDER = DotName.createSimple(SubscriberBuilder.class.getName());

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void registerHttpConnector(BuildProducer<AdditionalBeanBuildItem> beanProducer,
            BuildProducer<GeneratedBeanBuildItem> generatedBeanProducer,
            BuildProducer<RouteBuildItem> routeProducer,
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
                        routeProducer.produce(RouteBuildItem.builder().route(path).handler(bodyHandler.getHandler()).build());
                        routeProducer.produce(RouteBuildItem.builder().route(path).handler(handler).build());
                    });
        }
        if (!wsConfigs.isEmpty()) {
            Handler<RoutingContext> handler = recorder.createWebSocketHandler();

            wsConfigs.stream()
                    .map(WebSocketStreamConfig::path)
                    .distinct()
                    .forEach(path -> routeProducer.produce(RouteBuildItem.builder().route(path).handler(handler).build()));
        }

        initSerializers(ReactiveHttpConfig.readSerializers(), generatedBeanProducer);
    }

    @BuildStep
    void registerMessagePayloadClassesForReflection(BeanArchiveIndexBuildItem index,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {

        Set<String> payloadClasses = new HashSet<>();
        for (AnnotationInstance incoming : index.getIndex().getAnnotations(DotName.createSimple(Incoming.class.getName()))) {
            MethodInfo methodInfo = incoming.target().asMethod();
            List<Type> parameters = methodInfo.parameters();

            if (parameters.size() == 1) {
                Type type = parameters.get(0);
                // payload can be consumed as Publisher[Builder]<PayloadObject> or Publisher[Builder]<Message<PayloadObject>>
                // or Multi<PayloadObject>, Multi<Message<PayloadObject>>
                DotName typeName = type.name();
                if (type.kind() == Type.Kind.PARAMETERIZED_TYPE
                        && (typeName.equals(PUBLISHER_BUILDER) || typeName.equals(PUBLISHER) || typeName.equals(MULTI))) {
                    List<Type> arguments = type.asParameterizedType().arguments();
                    if (arguments.size() > 0) {
                        collectPayloadType(payloadClasses, arguments.get(0));
                    }
                } else {
                    collectPayloadType(payloadClasses, type);
                }
            } else if (parameters.size() == 0) {
                // @Incoming method can also return a Subscriber[Builder] or Processor[Builder] for message payloads:
                Type returnType = methodInfo.returnType();
                if ((returnType.name().equals(SUBSCRIBER_BUILDER)
                        || returnType.name().equals(PROCESSOR_BUILDER)
                        || returnType.name().equals(SUBSCRIBER)
                        || returnType.name().equals(PROCESSOR))
                        && returnType.kind() == Type.Kind.PARAMETERIZED_TYPE) {
                    ParameterizedType parameterizedType = returnType.asParameterizedType();
                    List<Type> arguments = parameterizedType.arguments();
                    if (arguments.size() > 0) {
                        collectPayloadType(payloadClasses, arguments.get(0));
                    }
                }
            }
        }

        payloadClasses.removeAll(asList(JSON_OBJECT.toString(), OBJECT.toString(), JSON_ARRAY.toString(), STRING.toString()));

        reflectiveClasses.produce(new ReflectiveClassBuildItem(true, false, payloadClasses.toArray(new String[] {})));
    }

    private void collectPayloadType(Set<String> payloadClasses, Type type) {
        if (type.kind() != Type.Kind.CLASS && type.kind() != Type.Kind.PARAMETERIZED_TYPE) {
            return;
        }
        if (type.name().equals(MESSAGE)) {
            // wrapped in a message
            if (type.kind() == Type.Kind.PARAMETERIZED_TYPE) {
                Type payloadType = type.asParameterizedType().arguments().get(0);
                if (payloadType.kind() == Type.Kind.CLASS) {
                    payloadClasses.add(payloadType.name().toString());
                }
            }
        } else {
            // or used directly
            payloadClasses.add(type.name().toString());
        }
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
