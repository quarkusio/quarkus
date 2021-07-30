package io.quarkus.smallrye.reactivemessaging.kafka.deployment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.ConfigValue;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.MethodParameterInfo;
import org.jboss.jandex.Type;

import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.deployment.builditem.RuntimeConfigSetupCompleteBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.smallrye.reactivemessaging.kafka.ReactiveMessagingKafkaConfig;
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

    /**
     * Handles the serializer/deserializer detection and whether or not the graceful shutdown should be used in dev mode.
     */
    @BuildStep
    public void defaultChannelConfiguration(
            LaunchModeBuildItem launchMode,
            ReactiveMessagingKafkaBuildTimeConfig buildTimeConfig,
            ReactiveMessagingKafkaConfig runtimeConfig,
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<RunTimeConfigurationDefaultBuildItem> defaultConfigProducer) {

        DefaultSerdeDiscoveryState discoveryState = new DefaultSerdeDiscoveryState(combinedIndex.getIndex());
        if (buildTimeConfig.serializerAutodetectionEnabled) {
            discoverDefaultSerdeConfig(discoveryState, defaultConfigProducer);
        }

        if (launchMode.getLaunchMode().isDevOrTest()) {
            if (!runtimeConfig.enableGracefulShutdownInDevAndTestMode) {
                List<AnnotationInstance> incomings = discoveryState.findAnnotationsOnMethods(DotNames.INCOMING);
                List<AnnotationInstance> channels = discoveryState.findAnnotationsOnInjectionPoints(DotNames.CHANNEL);
                List<AnnotationInstance> annotations = new ArrayList<>();
                annotations.addAll(incomings);
                annotations.addAll(channels);
                for (AnnotationInstance annotation : annotations) {
                    String channelName = annotation.value().asString();
                    if (!discoveryState.isKafkaConnector(true, channelName)) {
                        continue;
                    }
                    String key = "mp.messaging.incoming." + channelName + ".graceful-shutdown";
                    discoveryState.ifNotYetConfigured(key, () -> {
                        defaultConfigProducer.produce(new RunTimeConfigurationDefaultBuildItem(key, "false"));
                    });
                }
            }
        }
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

            Type incomingType = getIncomingTypeFromMethod(method);
            processIncomingType(discovery, incomingType, (keyDeserializer, valueDeserializer) -> {
                produceRuntimeConfigurationDefaultBuildItem(discovery, config,
                        "mp.messaging.incoming." + channelName + ".key.deserializer", keyDeserializer);
                produceRuntimeConfigurationDefaultBuildItem(discovery, config,
                        "mp.messaging.incoming." + channelName + ".value.deserializer", valueDeserializer);

                handleAdditionalProperties("mp.messaging.incoming." + channelName + ".", discovery,
                        config, keyDeserializer, valueDeserializer);
            });
        }

        for (AnnotationInstance annotation : discovery.findAnnotationsOnMethods(DotNames.OUTGOING)) {
            String channelName = annotation.value().asString();
            if (!discovery.isKafkaConnector(false, channelName)) {
                continue;
            }

            MethodInfo method = annotation.target().asMethod();

            Type outgoingType = getOutgoingTypeFromMethod(method);
            processOutgoingType(discovery, outgoingType, (keySerializer, valueSerializer) -> {
                produceRuntimeConfigurationDefaultBuildItem(discovery, config,
                        "mp.messaging.outgoing." + channelName + ".key.serializer", keySerializer);
                produceRuntimeConfigurationDefaultBuildItem(discovery, config,
                        "mp.messaging.outgoing." + channelName + ".value.serializer", valueSerializer);

                handleAdditionalProperties("mp.messaging.outgoing." + channelName + ".", discovery,
                        config, keySerializer, valueSerializer);
            });
        }

        for (AnnotationInstance annotation : discovery.findAnnotationsOnInjectionPoints(DotNames.CHANNEL)) {
            String channelName = annotation.value().asString();
            if (!discovery.isKafkaConnector(false, channelName)
                    && !discovery.isKafkaConnector(true, channelName)) {
                continue;
            }

            Type injectionPointType = getInjectionPointType(annotation);
            if (injectionPointType == null) {
                continue;
            }

            Type incomingType = getIncomingTypeFromChannelInjectionPoint(injectionPointType);
            processIncomingType(discovery, incomingType, (keyDeserializer, valueDeserializer) -> {
                produceRuntimeConfigurationDefaultBuildItem(discovery, config,
                        "mp.messaging.incoming." + channelName + ".key.deserializer", keyDeserializer);
                produceRuntimeConfigurationDefaultBuildItem(discovery, config,
                        "mp.messaging.incoming." + channelName + ".value.deserializer", valueDeserializer);

                handleAdditionalProperties("mp.messaging.incoming." + channelName + ".", discovery,
                        config, keyDeserializer, valueDeserializer);
            });

            Type outgoingType = getOutgoingTypeFromChannelInjectionPoint(injectionPointType);
            processOutgoingType(discovery, outgoingType, (keySerializer, valueSerializer) -> {
                produceRuntimeConfigurationDefaultBuildItem(discovery, config,
                        "mp.messaging.outgoing." + channelName + ".key.serializer", keySerializer);
                produceRuntimeConfigurationDefaultBuildItem(discovery, config,
                        "mp.messaging.outgoing." + channelName + ".value.serializer", valueSerializer);

                handleAdditionalProperties("mp.messaging.outgoing." + channelName + ".", discovery,
                        config, keySerializer, valueSerializer);
            });
        }
    }

    private Type getInjectionPointType(AnnotationInstance annotation) {
        switch (annotation.target().kind()) {
            case FIELD:
                return annotation.target().asField().type();
            case METHOD_PARAMETER:
                MethodParameterInfo parameter = annotation.target().asMethodParameter();
                return parameter.method().parameters().get(parameter.position());
            default:
                return null;
        }
    }

    private void handleAdditionalProperties(String configPropertyBase, DefaultSerdeDiscoveryState discovery,
            BuildProducer<RunTimeConfigurationDefaultBuildItem> config, Result... results) {
        for (Result result : results) {
            if (result == null) {
                continue;
            }

            result.additionalProperties.forEach((key, value) -> {
                produceRuntimeConfigurationDefaultBuildItem(discovery, config, configPropertyBase + key, value);
            });
        }
    }

    private void produceRuntimeConfigurationDefaultBuildItem(DefaultSerdeDiscoveryState discovery,
            BuildProducer<RunTimeConfigurationDefaultBuildItem> config, String key, Result result) {
        if (result == null) {
            return;
        }

        produceRuntimeConfigurationDefaultBuildItem(discovery, config, key, result.value);
    }

    private void produceRuntimeConfigurationDefaultBuildItem(DefaultSerdeDiscoveryState discovery,
            BuildProducer<RunTimeConfigurationDefaultBuildItem> config, String key, String value) {
        if (value == null) {
            return;
        }

        discovery.ifNotYetConfigured(key, () -> {
            config.produce(new RunTimeConfigurationDefaultBuildItem(key, value));
        });
    }

    private Type getIncomingTypeFromMethod(MethodInfo method) {
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
        return incomingType;
    }

    private Type getIncomingTypeFromChannelInjectionPoint(Type injectionPointType) {
        if (injectionPointType == null) {
            return null;
        }

        if (isPublisher(injectionPointType) || isPublisherBuilder(injectionPointType) || isMulti(injectionPointType)) {
            return injectionPointType.asParameterizedType().arguments().get(0);
        } else {
            return null;
        }
    }

    private void processIncomingType(DefaultSerdeDiscoveryState discovery, Type incomingType,
            BiConsumer<Result, Result> deserializerAcceptor) {
        extractKeyValueType(incomingType, (key, value) -> {
            Result keyDeserializer = deserializerFor(discovery, key);
            Result valueDeserializer = deserializerFor(discovery, value);
            deserializerAcceptor.accept(keyDeserializer, valueDeserializer);
        });
    }

    private Type getOutgoingTypeFromMethod(MethodInfo method) {
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
        return outgoingType;
    }

    private Type getOutgoingTypeFromChannelInjectionPoint(Type injectionPointType) {
        if (injectionPointType == null) {
            return null;
        }

        if (isEmitter(injectionPointType) || isMutinyEmitter(injectionPointType)) {
            return injectionPointType.asParameterizedType().arguments().get(0);
        } else {
            return null;
        }
    }

    private void processOutgoingType(DefaultSerdeDiscoveryState discovery, Type outgoingType,
            BiConsumer<Result, Result> serializerAcceptor) {
        extractKeyValueType(outgoingType, (key, value) -> {
            Result keySerializer = serializerFor(discovery, key);
            Result valueSerializer = serializerFor(discovery, value);
            serializerAcceptor.accept(keySerializer, valueSerializer);
        });
    }

    private void extractKeyValueType(Type type, BiConsumer<Type, Type> keyValueTypeAcceptor) {
        if (type == null) {
            return;
        }

        if (isMessage(type)) {
            List<Type> typeArguments = type.asParameterizedType().arguments();
            keyValueTypeAcceptor.accept(null, typeArguments.get(0));
        } else if (isKafkaRecord(type) || isRecord(type) || isProducerRecord(type) || isConsumerRecord(type)) {
            List<Type> typeArguments = type.asParameterizedType().arguments();
            keyValueTypeAcceptor.accept(typeArguments.get(0), typeArguments.get(1));
        } else if (isRawMessage(type)) {
            keyValueTypeAcceptor.accept(null, type);
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

    private Result deserializerFor(DefaultSerdeDiscoveryState discovery, Type type) {
        return serializerDeserializerFor(discovery, type, false);
    }

    private Result serializerFor(DefaultSerdeDiscoveryState discovery, Type type) {
        return serializerDeserializerFor(discovery, type, true);
    }

    private Result serializerDeserializerFor(DefaultSerdeDiscoveryState discovery, Type type, boolean serializer) {
        if (type == null) {
            return null;
        }
        DotName typeName = type.name();

        // statically known serializer/deserializer
        Map<DotName, String> map = serializer ? KNOWN_SERIALIZERS : KNOWN_DESERIALIZERS;
        if (map.containsKey(typeName)) {
            return Result.of(map.get(typeName));
        }

        // Avro generated class or GenericRecord (serializer/deserializer provided by Confluent or Apicurio)
        boolean isAvroGenerated = discovery.isAvroGenerated(typeName);
        if (isAvroGenerated || DotNames.AVRO_GENERIC_RECORD.equals(typeName)) {
            if (discovery.hasConfluent()) {
                return serializer
                        ? Result.of("io.confluent.kafka.serializers.KafkaAvroSerializer")
                        : Result.of("io.confluent.kafka.serializers.KafkaAvroDeserializer")
                                .with(isAvroGenerated, "specific.avro.reader", "true");
            } else if (discovery.hasApicurio1()) {
                return serializer
                        ? Result.of("io.apicurio.registry.utils.serde.AvroKafkaSerializer")
                        : Result.of("io.apicurio.registry.utils.serde.AvroKafkaDeserializer")
                                .with(isAvroGenerated, "apicurio.registry.use-specific-avro-reader", "true");
            } else if (discovery.hasApicurio2()) {
                return serializer
                        ? Result.of("io.apicurio.registry.serde.avro.AvroKafkaSerializer")
                        : Result.of("io.apicurio.registry.serde.avro.AvroKafkaDeserializer")
                                .with(isAvroGenerated, "apicurio.registry.use-specific-avro-reader", "true");
            }
        }

        // Jackson-based serializer/deserializer
        // note that Jackson is always present with Kafka, so no need to check
        {
            ClassInfo subclass = discovery.getSubclassOfWithTypeArgument(
                    serializer ? DotNames.OBJECT_MAPPER_SERIALIZER : DotNames.OBJECT_MAPPER_DESERIALIZER, typeName);
            if (subclass != null) {
                return Result.of(subclass.name().toString());
            }
        }

        // Jsonb-based serializer/deserializer
        if (discovery.hasJsonb()) {
            ClassInfo subclass = discovery.getSubclassOfWithTypeArgument(
                    serializer ? DotNames.JSONB_SERIALIZER : DotNames.JSONB_DESERIALIZER, typeName);
            if (subclass != null) {
                return Result.of(subclass.name().toString());
            }
        }

        // unknown
        return null;
    }

    // ---

    @BuildStep
    @Consume(RuntimeConfigSetupCompleteBuildItem.class)
    public void reflectiveValueSerializerPayload(CombinedIndexBuildItem combinedIndex,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        IndexView index = combinedIndex.getIndex();
        Config config = ConfigProvider.getConfig();

        processOutgoingForReflectiveClassPayload(index, config,
                (annotation, payloadType) -> produceReflectiveClass(reflectiveClass, payloadType));

        processOutgoingChannelForReflectiveClassPayload(index, config,
                (annotation, payloadType) -> produceReflectiveClass(reflectiveClass, payloadType));

        processIncomingForReflectiveClassPayload(index, config,
                (annotation, payloadType) -> produceReflectiveClass(reflectiveClass, payloadType));

        processIncomingChannelForReflectiveClassPayload(index, config,
                (annotation, payloadType) -> produceReflectiveClass(reflectiveClass, payloadType));

    }

    void produceReflectiveClass(BuildProducer<ReflectiveClassBuildItem> reflectiveClass, Type type) {
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, true, type.name().toString()));
    }

    // visible for testing
    void processOutgoingForReflectiveClassPayload(IndexView index, Config config,
            BiConsumer<AnnotationInstance, Type> annotationAcceptor) {
        processAnnotationsForReflectiveClassPayload(index, config, DotNames.OUTGOING, true,
                annotation -> getOutgoingTypeFromMethod(annotation.target().asMethod()), annotationAcceptor);
    }

    // visible for testing
    void processOutgoingChannelForReflectiveClassPayload(IndexView index, Config config,
            BiConsumer<AnnotationInstance, Type> annotationAcceptor) {
        processAnnotationsForReflectiveClassPayload(index, config, DotNames.CHANNEL, true,
                annotation -> getOutgoingTypeFromChannelInjectionPoint(getInjectionPointType(annotation)), annotationAcceptor);
    }

    // visible for testing
    void processIncomingForReflectiveClassPayload(IndexView index, Config config,
            BiConsumer<AnnotationInstance, Type> annotationAcceptor) {
        processAnnotationsForReflectiveClassPayload(index, config, DotNames.INCOMING, false,
                annotation -> getIncomingTypeFromMethod(annotation.target().asMethod()), annotationAcceptor);
    }

    // visible for testing
    void processIncomingChannelForReflectiveClassPayload(IndexView index, Config config,
            BiConsumer<AnnotationInstance, Type> annotationAcceptor) {
        processAnnotationsForReflectiveClassPayload(index, config, DotNames.CHANNEL, false,
                annotation -> getIncomingTypeFromChannelInjectionPoint(getInjectionPointType(annotation)),
                annotationAcceptor);
    }

    private void processAnnotationsForReflectiveClassPayload(IndexView index, Config config, DotName annotationType,
            boolean serializer, Function<AnnotationInstance, Type> typeExtractor,
            BiConsumer<AnnotationInstance, Type> annotationAcceptor) {
        for (AnnotationInstance annotation : index.getAnnotations(annotationType)) {
            String channelName = annotation.value().asString();
            Type type = typeExtractor.apply(annotation);
            extractKeyValueType(type, (key, value) -> {
                if (key != null && isSerdeJson(index, config, channelName, serializer, true)) {
                    annotationAcceptor.accept(annotation, key);
                }
                if (value != null && isSerdeJson(index, config, channelName, serializer, false)) {
                    annotationAcceptor.accept(annotation, value);
                }
            });
        }
    }

    private boolean isSerdeJson(IndexView index, Config config, String channelName, boolean serializer, boolean isKey) {
        ConfigValue configValue = config.getConfigValue(getConfigName(channelName, serializer, isKey));
        if (configValue.getValue() != null) {
            DotName serdeName = DotName.createSimple(configValue.getValue());
            return serializer ? isSubclassOfJsonSerializer(index, serdeName) : isSubclassOfJsonDeserializer(index, serdeName);
        }
        return false;
    }

    String getConfigName(String channelName, boolean serializer, boolean isKey) {
        return "mp.messaging." +
                (serializer ? "outgoing" : "incoming") + "." +
                channelName + "." +
                (isKey ? "key" : "value") + "." +
                (serializer ? "serializer" : "deserializer");
    }

    private boolean isSubclassOfJsonSerializer(IndexView index, DotName serializerName) {
        return isSubclassOf(index, DotNames.OBJECT_MAPPER_SERIALIZER, serializerName) ||
                isSubclassOf(index, DotNames.JSONB_SERIALIZER, serializerName);
    }

    private boolean isSubclassOfJsonDeserializer(IndexView index, DotName serializerName) {
        return isSubclassOf(index, DotNames.OBJECT_MAPPER_DESERIALIZER, serializerName) ||
                isSubclassOf(index, DotNames.JSONB_DESERIALIZER, serializerName);
    }

    private boolean isSubclassOf(IndexView index, DotName superclass, DotName expectedType) {
        if (superclass.equals(expectedType)) {
            return true;
        }
        return index.getKnownDirectSubclasses(superclass)
                .stream()
                .anyMatch(ci -> ci.name().equals(expectedType));
    }
}
