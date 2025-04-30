package io.quarkus.smallrye.reactivemessaging.pulsar.deployment;

import static io.quarkus.smallrye.reactivemessaging.deployment.SmallRyeReactiveMessagingProcessor.getChannelPropertyKey;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.apache.pulsar.common.schema.SchemaType;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.processor.KotlinUtils;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.pulsar.SchemaProviderRecorder;
import io.quarkus.smallrye.reactivemessaging.deployment.ReactiveMessagingDotNames;
import io.quarkus.smallrye.reactivemessaging.deployment.items.ConnectorManagedChannelBuildItem;

public class PulsarSchemaDiscoveryProcessor {

    static Logger log = Logger.getLogger(PulsarSchemaDiscoveryProcessor.class);

    /**
     * Handles the serializer/deserializer detection and whether the graceful shutdown should be used in dev mode.
     */
    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    public void defaultChannelConfiguration(
            ReactiveMessagingPulsarBuildTimeConfig buildTimeConfig,
            CombinedIndexBuildItem combinedIndex,
            List<ConnectorManagedChannelBuildItem> channelsManagedByConnectors,
            BuildProducer<RunTimeConfigurationDefaultBuildItem> defaultConfigProducer,
            BuildProducer<SyntheticBeanBuildItem> syntheticBean,
            RecorderContext recorderContext,
            SchemaProviderRecorder recorder) {
        if (buildTimeConfig.schemaAutodetectionEnabled()) {
            DefaultSchemaDiscoveryState discoveryState = new DefaultSchemaDiscoveryState(combinedIndex.getIndex());
            discoverDefaultSerdeConfig(discoveryState, channelsManagedByConnectors, defaultConfigProducer,
                    buildTimeConfig.schemaGenerationEnabled()
                            ? new SyntheticBeanBuilder(syntheticBean, recorder, recorderContext)
                            : null);
        }
    }

    // visible for testing
    void discoverDefaultSerdeConfig(DefaultSchemaDiscoveryState discovery,
            List<ConnectorManagedChannelBuildItem> channelsManagedByConnectors,
            BuildProducer<RunTimeConfigurationDefaultBuildItem> config,
            SyntheticBeanBuilder syntheticBean) {
        for (AnnotationInstance annotation : discovery.findRepeatableAnnotationsOnMethods(DotNames.INCOMING)) {
            String channelName = annotation.value().asString();
            if (!discovery.isPulsarConnector(channelsManagedByConnectors, true, channelName)) {
                continue;
            }

            MethodInfo method = annotation.target().asMethod();

            Type incomingType = getIncomingTypeFromMethod(method);
            processIncomingType(discovery, config, incomingType, channelName, syntheticBean);
        }

        for (AnnotationInstance annotation : discovery.findRepeatableAnnotationsOnMethods(DotNames.OUTGOING)) {
            String channelName = annotation.value().asString();
            if (!discovery.isPulsarConnector(channelsManagedByConnectors, false, channelName)) {
                continue;
            }

            MethodInfo method = annotation.target().asMethod();

            Type outgoingType = getOutgoingTypeFromMethod(method);
            processOutgoingType(discovery, config, outgoingType, channelName, syntheticBean);
        }

        for (AnnotationInstance annotation : discovery.findAnnotationsOnInjectionPoints(DotNames.CHANNEL)) {
            String channelName = annotation.value().asString();
            if (!discovery.isPulsarConnector(channelsManagedByConnectors, false, channelName)
                    && !discovery.isPulsarConnector(channelsManagedByConnectors, true, channelName)) {
                continue;
            }

            Type injectionPointType = getInjectionPointType(annotation);
            if (injectionPointType == null) {
                continue;
            }

            Type incomingType = getIncomingTypeFromChannelInjectionPoint(injectionPointType);
            processIncomingType(discovery, config, incomingType, channelName, syntheticBean);
            processPulsarTransactions(discovery, config, channelName, injectionPointType);

            Type outgoingType = getOutgoingTypeFromChannelInjectionPoint(injectionPointType);
            processOutgoingType(discovery, config, outgoingType, channelName, syntheticBean);
        }
    }

    private static String outgoingSchemaKey(String channelName) {
        return getChannelPropertyKey(channelName, "schema", false);
    }

    private void processPulsarTransactions(DefaultSchemaDiscoveryState discovery,
            BuildProducer<RunTimeConfigurationDefaultBuildItem> config,
            String channelName,
            Type injectionPointType) {
        if (injectionPointType != null && isPulsarEmitter(injectionPointType)) {
            String enableTransactionKey = getChannelPropertyKey(channelName, "enableTransaction", false);
            log.infof("Transactional producer detected for channel '%s', setting following default config values: "
                    + "'" + enableTransactionKey + "=true'", channelName);
            produceRuntimeConfigurationDefaultBuildItem(discovery, config, enableTransactionKey, "true");
        }
    }

    private void processIncomingType(DefaultSchemaDiscoveryState discovery,
            BuildProducer<RunTimeConfigurationDefaultBuildItem> config,
            Type incomingType,
            String channelName,
            SyntheticBeanBuilder syntheticBean) {
        extractValueType(incomingType, (value, isBatch) -> {
            if (discovery.findProvidedSchemaWithIdentifier(channelName).isEmpty()) {
                if (discovery.hasObjectMapperConfigSchema(value, channelName, true)) {
                    objectMapperSchemaFor(SyntheticBeanBuilder.objectMapperSchemaId(value), value, syntheticBean);
                } else {
                    String schema = schemaFor(discovery, value, syntheticBean);
                    produceRuntimeConfigurationDefaultBuildItem(discovery, config, incomingSchemaKey(channelName), schema);
                }
            }
            if (Boolean.TRUE.equals(isBatch)) {
                produceRuntimeConfigurationDefaultBuildItem(discovery, config,
                        getChannelPropertyKey(channelName, "batchReceive", true), "true");
            }
        });
    }

    private static String incomingSchemaKey(String channelName) {
        return getChannelPropertyKey(channelName, "schema", true);
    }

    private Type getInjectionPointType(AnnotationInstance annotation) {
        return switch (annotation.target().kind()) {
            case FIELD -> handleInstanceChannelInjection(annotation.target().asField().type());
            case METHOD_PARAMETER -> handleInstanceChannelInjection(annotation.target().asMethodParameter().type());
            default -> null;
        };
    }

    private Type handleInstanceChannelInjection(Type type) {
        return (DotNames.INSTANCE.equals(type.name())
                || DotNames.PROVIDER.equals(type.name())
                || DotNames.INJECTABLE_INSTANCE.equals(type.name()))
                        ? type.asParameterizedType().arguments().get(0)
                        : type;
    }

    private void produceRuntimeConfigurationDefaultBuildItem(DefaultSchemaDiscoveryState discovery,
            BuildProducer<RunTimeConfigurationDefaultBuildItem> config,
            String key, String value) {
        if (value == null) {
            return;
        }

        if (discovery.shouldNotConfigure(key)) {
            return;
        }

        discovery.ifNotYetConfigured(key, () -> config.produce(new RunTimeConfigurationDefaultBuildItem(key, value)));
    }

    private Type getIncomingTypeFromMethod(MethodInfo method) {
        List<Type> parameterTypes = method.parameterTypes();
        int parametersCount = parameterTypes.size();
        Type returnType = method.returnType();

        Type incomingType = null;

        // @Incoming
        if ((isVoid(returnType) && parametersCount >= 1)
                || (isCompletionStage(returnType) && parametersCount >= 1)
                || (isUni(returnType) && parametersCount >= 1)) {
            incomingType = parameterTypes.get(0);
        } else if ((isSubscriber(returnType) && parametersCount == 0)
                || (isSubscriberBuilder(returnType) && parametersCount == 0)) {
            incomingType = returnType.asParameterizedType().arguments().get(0);
        } else if (KotlinUtils.isKotlinSuspendMethod(method)) {
            incomingType = parameterTypes.get(0);
        }

        // @Incoming @Outgoing
        if (method.hasAnnotation(DotNames.OUTGOING) || method.hasAnnotation(DotNames.OUTGOINGS)) {
            if ((isCompletionStage(returnType) && parametersCount >= 1)
                    || (isUni(returnType) && parametersCount >= 1)
                    || (isPublisher(returnType) && parametersCount == 1)
                    || (isPublisherBuilder(returnType) && parametersCount == 1)
                    || (isFlowPublisher(returnType) && parametersCount == 1)
                    || (isMulti(returnType) && parametersCount == 1)) {
                incomingType = parameterTypes.get(0);
            } else if ((isProcessor(returnType) && parametersCount == 0)
                    || (isProcessorBuilder(returnType) && parametersCount == 0)) {
                incomingType = returnType.asParameterizedType().arguments().get(0);
            } else if (parametersCount >= 1) {
                incomingType = parameterTypes.get(0);
            } else if (KotlinUtils.isKotlinSuspendMethod(method)) {
                incomingType = parameterTypes.get(0);
            }

            // @Incoming @Outgoing stream manipulation
            if (incomingType != null
                    && (isPublisher(incomingType) || isPublisherBuilder(incomingType)
                            || isMulti(incomingType) || isFlowPublisher(incomingType))) {
                incomingType = incomingType.asParameterizedType().arguments().get(0);
            }
        }
        return incomingType;
    }

    private Type getIncomingTypeFromChannelInjectionPoint(Type injectionPointType) {
        if (injectionPointType == null) {
            return null;
        }

        if (isPublisher(injectionPointType) || isPublisherBuilder(injectionPointType)
                || isMulti(injectionPointType) || isFlowPublisher(injectionPointType)) {
            return injectionPointType.asParameterizedType().arguments().get(0);
        } else {
            return null;
        }
    }

    private Type getOutgoingTypeFromMethod(MethodInfo method) {
        List<Type> parameterTypes = method.parameterTypes();
        int parametersCount = parameterTypes.size();
        Type returnType = method.returnType();

        Type outgoingType = null;

        // @Outgoing
        if ((isPublisher(returnType) && parametersCount == 0)
                || (isPublisherBuilder(returnType) && parametersCount == 0)
                || (isMulti(returnType) && parametersCount == 0)
                || (isFlowPublisher(returnType) && parametersCount == 0)
                || (isCompletionStage(returnType) && parametersCount == 0)
                || (isUni(returnType) && parametersCount == 0)) {
            outgoingType = returnType.asParameterizedType().arguments().get(0);
        } else if (parametersCount == 0) {
            outgoingType = returnType;
        } else if (KotlinUtils.isKotlinSuspendMethod(method)) {
            outgoingType = getReturnTypeFromKotlinSuspendMethod(method);
        }

        // @Incoming @Outgoing
        if (method.hasAnnotation(DotNames.INCOMING) || method.hasAnnotation(DotNames.INCOMINGS)) {
            if (isCompletionStage(returnType) || isUni(returnType) || isMulti(returnType)) {
                outgoingType = returnType.asParameterizedType().arguments().get(0);
            } else if ((isPublisher(returnType) && parametersCount == 1)
                    || (isPublisherBuilder(returnType) && parametersCount == 1)
                    || (isFlowPublisher(returnType) && parametersCount == 1)
                    || (isMultiSplitter(returnType) && parametersCount == 1)) {
                outgoingType = returnType.asParameterizedType().arguments().get(0);
            } else if ((isProcessor(returnType) && parametersCount == 0)
                    || (isProcessorBuilder(returnType) && parametersCount == 0)) {
                outgoingType = returnType.asParameterizedType().arguments().get(1);
            } else if (KotlinUtils.isKotlinSuspendMethod(method)) {
                outgoingType = getReturnTypeFromKotlinSuspendMethod(method);
            } else {
                outgoingType = returnType;
            }

            // @Incoming @Outgoing stream manipulation
            if (outgoingType != null
                    && (isPublisher(outgoingType) || isPublisherBuilder(outgoingType)
                            || isMulti(outgoingType) || isFlowPublisher(outgoingType))) {
                outgoingType = outgoingType.asParameterizedType().arguments().get(0);
            }
        }
        return outgoingType;
    }

    private static Type getReturnTypeFromKotlinSuspendMethod(MethodInfo method) {
        Type continuationReturnType = method.parameterType(method.parametersCount() - 1);

        if (continuationReturnType.kind() == Type.Kind.PARAMETERIZED_TYPE) {
            Type firstGenericType = continuationReturnType.asParameterizedType().arguments().get(0);
            if (firstGenericType.kind() == Type.Kind.WILDCARD_TYPE) {
                return firstGenericType.asWildcardType().superBound();
            }
        }

        return null;
    }

    private Type getOutgoingTypeFromChannelInjectionPoint(Type injectionPointType) {
        if (injectionPointType == null) {
            return null;
        }

        if (isEmitter(injectionPointType) || isMutinyEmitter(injectionPointType)
                || isContextualEmitter(injectionPointType) || isPulsarEmitter(injectionPointType)) {
            return injectionPointType.asParameterizedType().arguments().get(0);
        } else {
            return null;
        }
    }

    private void processOutgoingType(DefaultSchemaDiscoveryState discovery,
            BuildProducer<RunTimeConfigurationDefaultBuildItem> config,
            Type outgoingType,
            String channelName, SyntheticBeanBuilder syntheticBean) {
        extractValueType(outgoingType, (value, isBatch) -> {
            if (discovery.findProvidedSchemaWithIdentifier(channelName).isEmpty()) {
                if (discovery.hasObjectMapperConfigSchema(value, channelName, false)) {
                    objectMapperSchemaFor(SyntheticBeanBuilder.objectMapperSchemaId(value), value, syntheticBean);
                } else {
                    String schema = schemaFor(discovery, value, syntheticBean);
                    produceRuntimeConfigurationDefaultBuildItem(discovery, config, outgoingSchemaKey(channelName), schema);
                }
            }
        });
    }

    private void extractValueType(Type type, BiConsumer<Type, Boolean> schemaAcceptor) {
        if (type == null) {
            return;
        }

        if (isTargeted(type)) {
            return;
        }

        if (isGenericPayload(type)) {
            extractValueType(type.asParameterizedType().arguments().get(0), schemaAcceptor);
            return;
        }

        if (isMessage(type)) {
            List<Type> typeArguments = type.asParameterizedType().arguments();
            Type messageTypeParameter = typeArguments.get(0);
            if (isList(messageTypeParameter)) {
                List<Type> messageListTypeArguments = messageTypeParameter.asParameterizedType().arguments();
                schemaAcceptor.accept(messageListTypeArguments.get(0), true);
            } else {
                schemaAcceptor.accept(messageTypeParameter, false);
            }
        } else if (isList(type)) {
            List<Type> typeArguments = type.asParameterizedType().arguments();
            schemaAcceptor.accept(typeArguments.get(0), true);
        } else if (isPulsarMessage(type) || isPulsarApiMessage(type) || isOutgoingMessage(type)) {
            List<Type> typeArguments = type.asParameterizedType().arguments();
            schemaAcceptor.accept(typeArguments.get(0), false);
        } else if (isPulsarApiMessages(type) || isPulsarBatchMessage(type)) {
            List<Type> typeArguments = type.asParameterizedType().arguments();
            schemaAcceptor.accept(typeArguments.get(0), true);
        } else if (isKeyedMulti(type)) {
            List<Type> typeArguments = type.asParameterizedType().arguments();
            schemaAcceptor.accept(typeArguments.get(1), false);
        } else if (isRawMessage(type)) {
            schemaAcceptor.accept(type, false);
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

    private static boolean isMultiSplitter(Type type) {
        // raw type MultiSplitter is wrong, must be MultiSplitter<Something, KeyEnum>
        return DotNames.MULTI_SPLITTER.equals(type.name())
                && type.kind() == Type.Kind.PARAMETERIZED_TYPE
                && type.asParameterizedType().arguments().size() == 2;
    }

    private static boolean isTargeted(Type type) {
        return DotNames.TARGETED.equals(type.name())
                || DotNames.TARGETED_MESSAGES.equals(type.name());
    }

    private static boolean isFlowPublisher(Type type) {
        // raw type Flow.Publisher is wrong, must be Multi<Something>
        return DotNames.FLOW_PUBLISHER.equals(type.name())
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

    private static boolean isContextualEmitter(Type type) {
        // raw type MutinyEmitter is wrong, must be MutinyEmitter<Something>
        return DotNames.CONTEXTUAL_EMITTER.equals(type.name())
                && type.kind() == Type.Kind.PARAMETERIZED_TYPE
                && type.asParameterizedType().arguments().size() == 1;
    }

    private static boolean isPulsarEmitter(Type type) {
        // raw type PulsarTransactions is wrong, must be PulsarTransactions<Something>
        return DotNames.PULSAR_EMITTER.equals(type.name())
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

    private static boolean isGenericPayload(Type type) {
        // raw type Message is wrong, must be Message<Something>
        return DotNames.GENERIC_PAYLOAD.equals(type.name())
                && type.kind() == Type.Kind.PARAMETERIZED_TYPE
                && type.asParameterizedType().arguments().size() == 1;
    }

    private static boolean isPulsarMessage(Type type) {
        // raw type PulsarMessage is wrong, must be PulsarMessage<SomethingElse>
        return DotNames.PULSAR_MESSAGE.equals(type.name())
                && type.kind() == Type.Kind.PARAMETERIZED_TYPE
                && type.asParameterizedType().arguments().size() == 1;
    }

    private static boolean isOutgoingMessage(Type type) {
        // raw type Record is wrong, must be Record<Something, SomethingElse>
        return DotNames.OUTGOING_MESSAGE.equals(type.name())
                && type.kind() == Type.Kind.PARAMETERIZED_TYPE
                && type.asParameterizedType().arguments().size() == 1;
    }

    private static boolean isPulsarApiMessage(Type type) {
        // raw type ConsumerRecord is wrong, must be ConsumerRecord<Something, SomethingElse>
        return DotNames.PULSAR_API_MESSAGE.equals(type.name())
                && type.kind() == Type.Kind.PARAMETERIZED_TYPE
                && type.asParameterizedType().arguments().size() == 1;
    }

    private static boolean isKeyedMulti(Type type) {
        return ReactiveMessagingDotNames.KEYED_MULTI.equals(type.name())
                && type.kind() == Type.Kind.PARAMETERIZED_TYPE
                && type.asParameterizedType().arguments().size() == 2;
    }

    private static boolean isList(Type type) {
        return DotNames.LIST.equals(type.name())
                && type.kind() == Type.Kind.PARAMETERIZED_TYPE
                && type.asParameterizedType().arguments().size() == 1;
    }

    private static boolean isPulsarBatchMessage(Type type) {
        return DotNames.PULSAR_BATCH_MESSAGE.equals(type.name())
                && type.kind() == Type.Kind.PARAMETERIZED_TYPE
                && type.asParameterizedType().arguments().size() == 1;
    }

    private static boolean isPulsarApiMessages(Type type) {
        return DotNames.PULSAR_API_MESSAGES.equals(type.name())
                && type.kind() == Type.Kind.PARAMETERIZED_TYPE
                && type.asParameterizedType().arguments().size() == 1;
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

    private String knownSchemaFor(Type type) {
        if (type == null) {
            return null;
        }
        DotName typeName = type.name();

        // statically known schema
        if (KNOWN_SCHEMA.containsKey(typeName)) {
            return KNOWN_SCHEMA.get(typeName);
        }

        // unknown
        return null;
    }

    private void objectMapperSchemaFor(String schemaId, Type type, SyntheticBeanBuilder syntheticBean) {
        if (syntheticBean != null) {
            syntheticBean.produceObjectMapperSchemaBean(schemaId, type);
        }
    }

    private String schemaFor(DefaultSchemaDiscoveryState discovery, Type type, SyntheticBeanBuilder syntheticBean) {
        String result = knownSchemaFor(type);
        if (result == null && type != null && syntheticBean != null && type.kind() == Type.Kind.CLASS) {
            String schemaId = syntheticBean.produceSchemaBean(discovery, type);
            if (schemaId != null) {
                log.infof("Generating Schema for type %s %s", type.name().toString(), schemaId);
            }
            result = schemaId;
        }
        return result;
    }

    // ---

    // @formatter:off
    private static final Map<DotName, String> KNOWN_SCHEMA = Map.ofEntries(
            // primitives
            Map.entry(DotName.createSimple("byte"),  SchemaType.INT8.name()),
            Map.entry(DotName.createSimple("short"),  SchemaType.INT16.name()),
            Map.entry(DotName.createSimple("int"),    SchemaType.INT32.name()),
            Map.entry(DotName.createSimple("long"),   SchemaType.INT64.name()),
            Map.entry(DotName.createSimple("float"),  SchemaType.FLOAT.name()),
            Map.entry(DotName.createSimple("double"), SchemaType.DOUBLE.name()),
            Map.entry(DotName.createSimple("boolean"), SchemaType.BOOLEAN.name()),
            // primitive wrappers
            Map.entry(DotName.createSimple(Byte.class.getName()),   SchemaType.INT8.name()),
            Map.entry(DotName.createSimple(Short.class.getName()),   SchemaType.INT16.name()),
            Map.entry(DotName.createSimple(Integer.class.getName()), SchemaType.INT32.name()),
            Map.entry(DotName.createSimple(Long.class.getName()),    SchemaType.INT64.name()),
            Map.entry(DotName.createSimple(Float.class.getName()),   SchemaType.FLOAT.name()),
            Map.entry(DotName.createSimple(Double.class.getName()),  SchemaType.DOUBLE.name()),
            Map.entry(DotName.createSimple(Boolean.class.getName()),  SchemaType.BOOLEAN.name()),
            // arrays
            Map.entry(DotName.createSimple("[B"), SchemaType.BYTES.name()),
            // other
            Map.entry(DotName.createSimple(String.class.getName()),    SchemaType.STRING.name()),
            Map.entry(DotName.createSimple(java.time.Instant.class.getName()),    SchemaType.INSTANT.name()),
            Map.entry(DotName.createSimple(java.sql.Timestamp.class.getName()),    SchemaType.TIMESTAMP.name()),
            Map.entry(DotName.createSimple(java.time.LocalDate.class.getName()),    SchemaType.LOCAL_DATE.name()),
            Map.entry(DotName.createSimple(java.time.LocalTime.class.getName()),    SchemaType.LOCAL_TIME.name()),
            Map.entry(DotName.createSimple(java.time.LocalDateTime.class.getName()),    SchemaType.LOCAL_DATE_TIME.name()),
            // Pulsar
            Map.entry(DotNames.PULSAR_GENERIC_RECORD,    SchemaType.AUTO_CONSUME.name())
    );
    // @formatter:on

}
