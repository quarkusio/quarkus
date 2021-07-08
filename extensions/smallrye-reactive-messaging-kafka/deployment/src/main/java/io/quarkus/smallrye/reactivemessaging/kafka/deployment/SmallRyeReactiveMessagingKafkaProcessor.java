package io.quarkus.smallrye.reactivemessaging.kafka.deployment;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.MethodParameterInfo;
import org.jboss.jandex.Type;

import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.vertx.kafka.client.consumer.impl.KafkaReadStreamImpl;

public class SmallRyeReactiveMessagingKafkaProcessor {

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.SMALLRYE_REACTIVE_MESSAGING_KAFKA);
    }

    @BuildStep
    public void build(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        // Required for the throttled commit strategy
        reflectiveClass.produce(
                ReflectiveClassBuildItem.builder(KafkaReadStreamImpl.class)
                        .fields(true)
                        .methods(true)
                        .constructors(true)
                        .finalFieldsWritable(true)
                        .build());
    }

    @BuildStep
    public void defaultSerdeConfig(ReactiveMessagingKafkaBuildTimeConfig buildTimeConfig,
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<RunTimeConfigurationDefaultBuildItem> defaultConfigProducer) {

        if (!buildTimeConfig.serializerAutodetectionEnabled) {
            return;
        }

        DefaultSerdeDiscoveryState discoveryState = new DefaultSerdeDiscoveryState(combinedIndex.getIndex());

        discoverDefaultSerdeConfig(discoveryState, defaultConfigProducer);
    }

    // visible for testing
    void discoverDefaultSerdeConfig(DefaultSerdeDiscoveryState discovery,
            BuildProducer<RunTimeConfigurationDefaultBuildItem> config) {
        for (AnnotationInstance annotation : discovery.findAnnotationsOnMethods(DotNames.INCOMING)) {
            String channelName = annotation.value().asString();
            if (!discovery.isKafkaConnector(true, channelName)) {
                continue;
            }

            MethodInfo method = annotation.target().asMethod();

            processIncomingMethod(discovery, method, (keyDeserializer, valueDeserializer) -> {
                produceRuntimeConfigurationDefaultBuildItem(discovery, config,
                        "mp.messaging.incoming." + channelName + ".key.deserializer", keyDeserializer);
                produceRuntimeConfigurationDefaultBuildItem(discovery, config,
                        "mp.messaging.incoming." + channelName + ".value.deserializer", valueDeserializer);
            });
        }

        for (AnnotationInstance annotation : discovery.findAnnotationsOnMethods(DotNames.OUTGOING)) {
            String channelName = annotation.value().asString();
            if (!discovery.isKafkaConnector(false, channelName)) {
                continue;
            }

            MethodInfo method = annotation.target().asMethod();

            processOutgoingMethod(discovery, method, (keySerializer, valueSerializer) -> {
                produceRuntimeConfigurationDefaultBuildItem(discovery, config,
                        "mp.messaging.outgoing." + channelName + ".key.serializer", keySerializer);
                produceRuntimeConfigurationDefaultBuildItem(discovery, config,
                        "mp.messaging.outgoing." + channelName + ".value.serializer", valueSerializer);
            });
        }

        for (AnnotationInstance annotation : discovery.findAnnotationsOnInjectionPoints(DotNames.CHANNEL)) {
            String channelName = annotation.value().asString();
            if (!discovery.isKafkaConnector(false, channelName)
                    && !discovery.isKafkaConnector(true, channelName)) {
                continue;
            }

            Type injectionPointType;
            switch (annotation.target().kind()) {
                case FIELD:
                    injectionPointType = annotation.target().asField().type();
                    break;
                case METHOD_PARAMETER:
                    MethodParameterInfo parameter = annotation.target().asMethodParameter();
                    injectionPointType = parameter.method().parameters().get(parameter.position());
                    break;
                default:
                    continue;
            }

            processIncomingChannelInjectionPoint(discovery, injectionPointType, (keyDeserializer, valueDeserializer) -> {
                produceRuntimeConfigurationDefaultBuildItem(discovery, config,
                        "mp.messaging.incoming." + channelName + ".key.deserializer", keyDeserializer);
                produceRuntimeConfigurationDefaultBuildItem(discovery, config,
                        "mp.messaging.incoming." + channelName + ".value.deserializer", valueDeserializer);
            });

            processOutgoingChannelInjectionPoint(discovery, injectionPointType, (keySerializer, valueSerializer) -> {
                produceRuntimeConfigurationDefaultBuildItem(discovery, config,
                        "mp.messaging.outgoing." + channelName + ".key.serializer", keySerializer);
                produceRuntimeConfigurationDefaultBuildItem(discovery, config,
                        "mp.messaging.outgoing." + channelName + ".value.serializer", valueSerializer);
            });
        }
    }

    void produceRuntimeConfigurationDefaultBuildItem(DefaultSerdeDiscoveryState discovery,
            BuildProducer<RunTimeConfigurationDefaultBuildItem> config, String key, String value) {
        discovery.runIfConfigIsAbsent(key, value,
                () -> config.produce(new RunTimeConfigurationDefaultBuildItem(key, value)));
    }

    private void processIncomingMethod(DefaultSerdeDiscoveryState discovery, MethodInfo method,
            BiConsumer<String, String> deserializerAcceptor) {
        List<Type> parameterTypes = method.parameters();
        int parametersCount = parameterTypes.size();
        Type returnType = method.returnType();

        Type incomingType = null;

        // @Incoming
        if ((isVoid(returnType) && parametersCount == 1)
                || (isCompletionStage(returnType) && parametersCount == 1)
                || (isUni(returnType) && parametersCount == 1)) {
            incomingType = parameterTypes.get(0);
        } else if ((isSubscriber(returnType) && parametersCount == 0)
                || (isSubscriberBuilder(returnType) && parametersCount == 0)) {
            incomingType = returnType.asParameterizedType().arguments().get(0);
        }

        // @Incoming @Outgoing
        if (method.hasAnnotation(DotNames.OUTGOING)) {
            if ((isCompletionStage(returnType) && parametersCount == 1)
                    || (isUni(returnType) && parametersCount == 1)
                    || (isPublisher(returnType) && parametersCount == 1)
                    || (isPublisherBuilder(returnType) && parametersCount == 1)
                    || (isMulti(returnType) && parametersCount == 1)) {
                incomingType = parameterTypes.get(0);
            } else if ((isProcessor(returnType) && parametersCount == 0)
                    || (isProcessorBuilder(returnType) && parametersCount == 0)) {
                incomingType = returnType.asParameterizedType().arguments().get(0);
            } else if (parametersCount == 1) {
                incomingType = parameterTypes.get(0);
            }

            // @Incoming @Outgoing stream manipulation
            if (incomingType != null
                    && (isPublisher(incomingType) || isPublisherBuilder(incomingType) || isMulti(incomingType))) {
                incomingType = incomingType.asParameterizedType().arguments().get(0);
            }
        }

        processIncomingType(discovery, incomingType, deserializerAcceptor);
    }

    private void processIncomingChannelInjectionPoint(DefaultSerdeDiscoveryState discovery, Type injectionPointType,
            BiConsumer<String, String> deserializerAcceptor) {
        Type incomingType = null;

        if (isPublisher(injectionPointType) || isPublisherBuilder(injectionPointType) || isMulti(injectionPointType)) {
            incomingType = injectionPointType.asParameterizedType().arguments().get(0);
        }

        processIncomingType(discovery, incomingType, deserializerAcceptor);
    }

    private void processIncomingType(DefaultSerdeDiscoveryState discovery, Type incomingType,
            BiConsumer<String, String> deserializerAcceptor) {
        if (incomingType == null) {
            return;
        }

        if (isMessage(incomingType)) {
            List<Type> typeArguments = incomingType.asParameterizedType().arguments();
            String deserializer = deserializerFor(discovery, typeArguments.get(0));
            deserializerAcceptor.accept(null, deserializer);
        } else if (isKafkaRecord(incomingType) || isRecord(incomingType) || isConsumerRecord(incomingType)) {
            List<Type> typeArguments = incomingType.asParameterizedType().arguments();
            String keyDeserializer = deserializerFor(discovery, typeArguments.get(0));
            String valueDeserializer = deserializerFor(discovery, typeArguments.get(1));
            deserializerAcceptor.accept(keyDeserializer, valueDeserializer);
        } else if (isRawMessage(incomingType)) {
            String deserializer = deserializerFor(discovery, incomingType);
            deserializerAcceptor.accept(null, deserializer);
        }
    }

    private void processOutgoingMethod(DefaultSerdeDiscoveryState discovery, MethodInfo method,
            BiConsumer<String, String> serializerAcceptor) {
        List<Type> parameterTypes = method.parameters();
        int parametersCount = parameterTypes.size();
        Type returnType = method.returnType();

        Type outgoingType = null;

        // @Outgoing
        if ((isPublisher(returnType) && parametersCount == 0)
                || (isPublisherBuilder(returnType) && parametersCount == 0)
                || (isMulti(returnType) && parametersCount == 0)
                || (isCompletionStage(returnType) && parametersCount == 0)
                || (isUni(returnType) && parametersCount == 0)) {
            outgoingType = returnType.asParameterizedType().arguments().get(0);
        } else if (parametersCount == 0) {
            outgoingType = returnType;
        }

        // @Incoming @Outgoing
        if (method.hasAnnotation(DotNames.INCOMING)) {
            if ((isCompletionStage(returnType) && parametersCount == 1)
                    || (isUni(returnType) && parametersCount == 1)
                    || (isPublisher(returnType) && parametersCount == 1)
                    || (isPublisherBuilder(returnType) && parametersCount == 1)
                    || (isMulti(returnType) && parametersCount == 1)) {
                outgoingType = returnType.asParameterizedType().arguments().get(0);
            } else if ((isProcessor(returnType) && parametersCount == 0)
                    || (isProcessorBuilder(returnType) && parametersCount == 0)) {
                outgoingType = returnType.asParameterizedType().arguments().get(1);
            } else if (parametersCount == 1) {
                outgoingType = returnType;
            }

            // @Incoming @Outgoing stream manipulation
            if (outgoingType != null
                    && (isPublisher(outgoingType) || isPublisherBuilder(outgoingType) || isMulti(outgoingType))) {
                outgoingType = outgoingType.asParameterizedType().arguments().get(0);
            }
        }

        processOutgoingType(discovery, outgoingType, serializerAcceptor);
    }

    private void processOutgoingChannelInjectionPoint(DefaultSerdeDiscoveryState discovery, Type injectionPointType,
            BiConsumer<String, String> serializerAcceptor) {
        Type outgoingType = null;

        if (isEmitter(injectionPointType) || isMutinyEmitter(injectionPointType)) {
            outgoingType = injectionPointType.asParameterizedType().arguments().get(0);
        }

        processOutgoingType(discovery, outgoingType, serializerAcceptor);
    }

    private void processOutgoingType(DefaultSerdeDiscoveryState discovery, Type outgoingType,
            BiConsumer<String, String> serializerAcceptor) {
        if (outgoingType == null) {
            return;
        }

        if (isMessage(outgoingType)) {
            List<Type> typeArguments = outgoingType.asParameterizedType().arguments();
            String serializer = serializerFor(discovery, typeArguments.get(0));
            serializerAcceptor.accept(null, serializer);
        } else if (isKafkaRecord(outgoingType) || isRecord(outgoingType) || isProducerRecord(outgoingType)) {
            List<Type> typeArguments = outgoingType.asParameterizedType().arguments();
            String keySerializer = serializerFor(discovery, typeArguments.get(0));
            String valueSerializer = serializerFor(discovery, typeArguments.get(1));
            serializerAcceptor.accept(keySerializer, valueSerializer);
        } else if (isRawMessage(outgoingType)) {
            String serializer = serializerFor(discovery, outgoingType);
            serializerAcceptor.accept(null, serializer);
        }
    }

    // ---

    private static boolean isVoid(Type type) {
        return type.kind() == Type.Kind.VOID;
    }

    private static boolean isCompletionStage(Type type) {
        // raw type CompletionStage is wrong, must be CompletionStage<Something>
        return DotNames.COMPLETION_STAGE.equals(type.name())
                && type.kind() == Type.Kind.PARAMETERIZED_TYPE
                && type.asParameterizedType().arguments().size() == 1;
    }

    private static boolean isUni(Type type) {
        // raw type Uni is wrong, must be Uni<Something>
        return DotNames.UNI.equals(type.name())
                && type.kind() == Type.Kind.PARAMETERIZED_TYPE
                && type.asParameterizedType().arguments().size() == 1;
    }

    private static boolean isMulti(Type type) {
        // raw type Multi is wrong, must be Multi<Something>
        return DotNames.MULTI.equals(type.name())
                && type.kind() == Type.Kind.PARAMETERIZED_TYPE
                && type.asParameterizedType().arguments().size() == 1;
    }

    private static boolean isSubscriber(Type type) {
        // raw type Subscriber is wrong, must be Subscriber<Something>
        return DotNames.SUBSCRIBER.equals(type.name())
                && type.kind() == Type.Kind.PARAMETERIZED_TYPE
                && type.asParameterizedType().arguments().size() == 1;
    }

    private static boolean isSubscriberBuilder(Type type) {
        // raw type SubscriberBuilder is wrong, must be SubscriberBuilder<Something, SomethingElse>
        return DotNames.SUBSCRIBER_BUILDER.equals(type.name())
                && type.kind() == Type.Kind.PARAMETERIZED_TYPE
                && type.asParameterizedType().arguments().size() == 2;
    }

    private static boolean isPublisher(Type type) {
        // raw type Publisher is wrong, must be Publisher<Something>
        return DotNames.PUBLISHER.equals(type.name())
                && type.kind() == Type.Kind.PARAMETERIZED_TYPE
                && type.asParameterizedType().arguments().size() == 1;
    }

    private static boolean isPublisherBuilder(Type type) {
        // raw type PublisherBuilder is wrong, must be PublisherBuilder<Something, SomethingElse>
        return DotNames.PUBLISHER_BUILDER.equals(type.name())
                && type.kind() == Type.Kind.PARAMETERIZED_TYPE
                && type.asParameterizedType().arguments().size() == 1;
    }

    private static boolean isProcessor(Type type) {
        // raw type Processor is wrong, must be Processor<Something, SomethingElse>
        return DotNames.PROCESSOR.equals(type.name())
                && type.kind() == Type.Kind.PARAMETERIZED_TYPE
                && type.asParameterizedType().arguments().size() == 2;
    }

    private static boolean isProcessorBuilder(Type type) {
        // raw type ProcessorBuilder is wrong, must be ProcessorBuilder<Something, SomethingElse>
        return DotNames.PROCESSOR_BUILDER.equals(type.name())
                && type.kind() == Type.Kind.PARAMETERIZED_TYPE
                && type.asParameterizedType().arguments().size() == 2;
    }

    // ---

    private static boolean isEmitter(Type type) {
        // raw type Emitter is wrong, must be Emitter<Something>
        return DotNames.EMITTER.equals(type.name())
                && type.kind() == Type.Kind.PARAMETERIZED_TYPE
                && type.asParameterizedType().arguments().size() == 1;
    }

    private static boolean isMutinyEmitter(Type type) {
        // raw type MutinyEmitter is wrong, must be MutinyEmitter<Something>
        return DotNames.MUTINY_EMITTER.equals(type.name())
                && type.kind() == Type.Kind.PARAMETERIZED_TYPE
                && type.asParameterizedType().arguments().size() == 1;
    }

    // ---

    private static boolean isMessage(Type type) {
        // raw type Message is wrong, must be Message<Something>
        return DotNames.MESSAGE.equals(type.name())
                && type.kind() == Type.Kind.PARAMETERIZED_TYPE
                && type.asParameterizedType().arguments().size() == 1;
    }

    private static boolean isKafkaRecord(Type type) {
        // raw type KafkaRecord is wrong, must be KafkaRecord<Something, SomethingElse>
        return DotNames.KAFKA_RECORD.equals(type.name())
                && type.kind() == Type.Kind.PARAMETERIZED_TYPE
                && type.asParameterizedType().arguments().size() == 2;
    }

    private static boolean isRecord(Type type) {
        // raw type Record is wrong, must be Record<Something, SomethingElse>
        return DotNames.RECORD.equals(type.name())
                && type.kind() == Type.Kind.PARAMETERIZED_TYPE
                && type.asParameterizedType().arguments().size() == 2;
    }

    private static boolean isConsumerRecord(Type type) {
        // raw type ConsumerRecord is wrong, must be ConsumerRecord<Something, SomethingElse>
        return DotNames.CONSUMER_RECORD.equals(type.name())
                && type.kind() == Type.Kind.PARAMETERIZED_TYPE
                && type.asParameterizedType().arguments().size() == 2;
    }

    private static boolean isProducerRecord(Type type) {
        // raw type ProducerRecord is wrong, must be ProducerRecord<Something, SomethingElse>
        return DotNames.PRODUCER_RECORD.equals(type.name())
                && type.kind() == Type.Kind.PARAMETERIZED_TYPE
                && type.asParameterizedType().arguments().size() == 2;
    }

    private static boolean isRawMessage(Type type) {
        switch (type.kind()) {
            case PRIMITIVE:
            case CLASS:
            case ARRAY:
                return true;
            default:
                return false;
        }
    }

    // ---

    // @formatter:off
    private static final Map<DotName, String> KNOWN_DESERIALIZERS = Map.ofEntries(
        // Java types with built-in Kafka deserializer
        // primitives
        Map.entry(DotName.createSimple("short"),  org.apache.kafka.common.serialization.ShortDeserializer.class.getName()),
        Map.entry(DotName.createSimple("int"),    org.apache.kafka.common.serialization.IntegerDeserializer.class.getName()),
        Map.entry(DotName.createSimple("long"),   org.apache.kafka.common.serialization.LongDeserializer.class.getName()),
        Map.entry(DotName.createSimple("float"),  org.apache.kafka.common.serialization.FloatDeserializer.class.getName()),
        Map.entry(DotName.createSimple("double"), org.apache.kafka.common.serialization.DoubleDeserializer.class.getName()),
        // primitive wrappers
        Map.entry(DotName.createSimple(java.lang.Short.class.getName()),   org.apache.kafka.common.serialization.ShortDeserializer.class.getName()),
        Map.entry(DotName.createSimple(java.lang.Integer.class.getName()), org.apache.kafka.common.serialization.IntegerDeserializer.class.getName()),
        Map.entry(DotName.createSimple(java.lang.Long.class.getName()),    org.apache.kafka.common.serialization.LongDeserializer.class.getName()),
        Map.entry(DotName.createSimple(java.lang.Float.class.getName()),   org.apache.kafka.common.serialization.FloatDeserializer.class.getName()),
        Map.entry(DotName.createSimple(java.lang.Double.class.getName()),  org.apache.kafka.common.serialization.DoubleDeserializer.class.getName()),
        // arrays
        Map.entry(DotName.createSimple("[B"), org.apache.kafka.common.serialization.ByteArrayDeserializer.class.getName()),
        // other
        Map.entry(DotName.createSimple(java.lang.Void.class.getName()),      org.apache.kafka.common.serialization.VoidDeserializer.class.getName()),
        Map.entry(DotName.createSimple(java.lang.String.class.getName()),    org.apache.kafka.common.serialization.StringDeserializer.class.getName()),
        Map.entry(DotName.createSimple(java.util.UUID.class.getName()),      org.apache.kafka.common.serialization.UUIDDeserializer.class.getName()),
        Map.entry(DotName.createSimple(java.nio.ByteBuffer.class.getName()), org.apache.kafka.common.serialization.ByteBufferDeserializer.class.getName()),
        // Kafka types
        Map.entry(DotName.createSimple(org.apache.kafka.common.utils.Bytes.class.getName()), org.apache.kafka.common.serialization.BytesDeserializer.class.getName()),
        // Vert.x types
        Map.entry(DotName.createSimple(io.vertx.core.buffer.Buffer.class.getName()),   io.vertx.kafka.client.serialization.BufferDeserializer.class.getName()),
        Map.entry(DotName.createSimple(io.vertx.core.json.JsonObject.class.getName()), io.vertx.kafka.client.serialization.JsonObjectDeserializer.class.getName()),
        Map.entry(DotName.createSimple(io.vertx.core.json.JsonArray.class.getName()),  io.vertx.kafka.client.serialization.JsonArrayDeserializer.class.getName())
    );

    private static final Map<DotName, String> KNOWN_SERIALIZERS = Map.ofEntries(
        // Java types with built-in Kafka serializer
        // primitives
        Map.entry(DotName.createSimple("short"),  org.apache.kafka.common.serialization.ShortSerializer.class.getName()),
        Map.entry(DotName.createSimple("int"),    org.apache.kafka.common.serialization.IntegerSerializer.class.getName()),
        Map.entry(DotName.createSimple("long"),   org.apache.kafka.common.serialization.LongSerializer.class.getName()),
        Map.entry(DotName.createSimple("float"),  org.apache.kafka.common.serialization.FloatSerializer.class.getName()),
        Map.entry(DotName.createSimple("double"), org.apache.kafka.common.serialization.DoubleSerializer.class.getName()),
        // primitives wrappers
        Map.entry(DotName.createSimple(java.lang.Short.class.getName()),   org.apache.kafka.common.serialization.ShortSerializer.class.getName()),
        Map.entry(DotName.createSimple(java.lang.Integer.class.getName()), org.apache.kafka.common.serialization.IntegerSerializer.class.getName()),
        Map.entry(DotName.createSimple(java.lang.Long.class.getName()),    org.apache.kafka.common.serialization.LongSerializer.class.getName()),
        Map.entry(DotName.createSimple(java.lang.Float.class.getName()),   org.apache.kafka.common.serialization.FloatSerializer.class.getName()),
        Map.entry(DotName.createSimple(java.lang.Double.class.getName()),  org.apache.kafka.common.serialization.DoubleSerializer.class.getName()),
        // arrays
        Map.entry(DotName.createSimple("[B"), org.apache.kafka.common.serialization.ByteArraySerializer.class.getName()),
        // other
        Map.entry(DotName.createSimple(java.lang.Void.class.getName()),      org.apache.kafka.common.serialization.VoidSerializer.class.getName()),
        Map.entry(DotName.createSimple(java.lang.String.class.getName()),    org.apache.kafka.common.serialization.StringSerializer.class.getName()),
        Map.entry(DotName.createSimple(java.util.UUID.class.getName()),      org.apache.kafka.common.serialization.UUIDSerializer.class.getName()),
        Map.entry(DotName.createSimple(java.nio.ByteBuffer.class.getName()), org.apache.kafka.common.serialization.ByteBufferSerializer.class.getName()),
        // Kafka types
        Map.entry(DotName.createSimple(org.apache.kafka.common.utils.Bytes.class.getName()), org.apache.kafka.common.serialization.BytesSerializer.class.getName()),
        // Vert.x types
        Map.entry(DotName.createSimple(io.vertx.core.buffer.Buffer.class.getName()),   io.vertx.kafka.client.serialization.BufferSerializer.class.getName()),
        Map.entry(DotName.createSimple(io.vertx.core.json.JsonObject.class.getName()), io.vertx.kafka.client.serialization.JsonObjectSerializer.class.getName()),
        Map.entry(DotName.createSimple(io.vertx.core.json.JsonArray.class.getName()),  io.vertx.kafka.client.serialization.JsonArraySerializer.class.getName())
    );
    // @formatter:on

    private String deserializerFor(DefaultSerdeDiscoveryState discovery, Type type) {
        return serializerDeserializerFor(discovery, type, false);
    }

    private String serializerFor(DefaultSerdeDiscoveryState discovery, Type type) {
        return serializerDeserializerFor(discovery, type, true);
    }

    private String serializerDeserializerFor(DefaultSerdeDiscoveryState discovery, Type type, boolean serializer) {
        DotName typeName = type.name();

        // statically known serializer/deserializer
        Map<DotName, String> map = serializer ? KNOWN_SERIALIZERS : KNOWN_DESERIALIZERS;
        if (map.containsKey(typeName)) {
            return map.get(typeName);
        }

        // Avro generated class (serializer/deserializer provided by Confluent or Apicurio)
        if (discovery.isAvroGenerated(typeName)) {
            if (discovery.hasConfluent()) {
                return serializer
                        ? "io.confluent.kafka.serializers.KafkaAvroSerializer"
                        : "io.confluent.kafka.serializers.KafkaAvroDeserializer";
            } else if (discovery.hasApicurio1()) {
                return serializer
                        ? "io.apicurio.registry.utils.serde.AvroKafkaSerializer"
                        : "io.apicurio.registry.utils.serde.AvroKafkaDeserializer";
            } else if (discovery.hasApicurio2()) {
                return serializer
                        ? "io.apicurio.registry.serde.avro.AvroKafkaSerializer"
                        : "io.apicurio.registry.serde.avro.AvroKafkaDeserializer";
            }
        }

        // Jackson-based serializer/deserializer
        // note that Jackson is always present with Kafka, so no need to check
        {
            ClassInfo subclass = discovery.getSubclassOfWithTypeArgument(
                    serializer ? DotNames.OBJECT_MAPPER_SERIALIZER : DotNames.OBJECT_MAPPER_DESERIALIZER, typeName);
            if (subclass != null) {
                return subclass.name().toString();
            }
        }

        // Jsonb-based serializer/deserializer
        if (discovery.hasJsonb()) {
            ClassInfo subclass = discovery.getSubclassOfWithTypeArgument(
                    serializer ? DotNames.JSONB_SERIALIZER : DotNames.JSONB_DESERIALIZER, typeName);
            if (subclass != null) {
                return subclass.name().toString();
            }
        }

        // unknown
        return null;
    }
}
