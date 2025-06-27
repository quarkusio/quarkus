package io.quarkus.smallrye.reactivemessaging.kafka.deployment;

import static io.quarkus.smallrye.reactivemessaging.kafka.HibernateOrmStateStore.HIBERNATE_ORM_STATE_STORE;
import static io.quarkus.smallrye.reactivemessaging.kafka.HibernateReactiveStateStore.HIBERNATE_REACTIVE_STATE_STORE;
import static io.quarkus.smallrye.reactivemessaging.kafka.RedisStateStore.REDIS_STATE_STORE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.processor.KotlinUtils;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.deployment.builditem.RuntimeConfigSetupCompleteBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.logging.LogCleanupFilterBuildItem;
import io.quarkus.hibernate.orm.deployment.spi.AdditionalJpaModelBuildItem;
import io.quarkus.smallrye.reactivemessaging.deployment.ReactiveMessagingDotNames;
import io.quarkus.smallrye.reactivemessaging.deployment.items.ConnectorManagedChannelBuildItem;
import io.quarkus.smallrye.reactivemessaging.kafka.DatabindProcessingStateCodec;
import io.quarkus.smallrye.reactivemessaging.kafka.HibernateOrmStateStore;
import io.quarkus.smallrye.reactivemessaging.kafka.HibernateReactiveStateStore;
import io.quarkus.smallrye.reactivemessaging.kafka.KafkaConfigCustomizer;
import io.quarkus.smallrye.reactivemessaging.kafka.RedisStateStore;
import io.smallrye.mutiny.tuples.Functions.TriConsumer;
import io.smallrye.reactive.messaging.kafka.KafkaConnector;
import io.smallrye.reactive.messaging.kafka.commit.ProcessingState;

public class SmallRyeReactiveMessagingKafkaProcessor {

    private static final Logger LOGGER = Logger.getLogger("io.quarkus.smallrye-reactive-messaging-kafka.deployment.processor");

    public static final String CHECKPOINT_STATE_STORE_MESSAGE = "Quarkus detected the use of `%s` for the" +
            " Kafka checkpoint commit strategy but the extension has not been added. Consider adding '%s'.";

    private static final String CHECKPOINT_ENTITY_NAME = "io.quarkus.smallrye.reactivemessaging.kafka.CheckpointEntity";
    private static final String CHECKPOINT_ENTITY_ID_NAME = "io.quarkus.smallrye.reactivemessaging.kafka.CheckpointEntityId";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.MESSAGING_KAFKA);
    }

    @BuildStep
    public void build(BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<AdditionalBeanBuildItem> additionalBean) {
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(ProcessingState.class)
                .reason(getClass().getName())
                .methods().fields().build());
        additionalBean.produce(AdditionalBeanBuildItem.unremovableOf(KafkaConfigCustomizer.class));
    }

    @BuildStep
    public void ignoreDuplicateJmxRegistrationInDevAndTestModes(LaunchModeBuildItem launchMode,
            BuildProducer<LogCleanupFilterBuildItem> log) {
        if (launchMode.getLaunchMode().isDevOrTest()) {
            log.produce(new LogCleanupFilterBuildItem(
                    "org.apache.kafka.common.utils.AppInfoParser",
                    "Error registering AppInfo mbean"));
        }
    }

    static boolean hasStateStoreConfig(String stateStoreName, Config config) {
        Optional<String> connectorStrategy = getConnectorProperty("checkpoint.state-store", config);
        if (connectorStrategy.isPresent() && connectorStrategy.get().equals(stateStoreName)) {
            return true;
        }
        List<String> stateStores = getChannelProperties("checkpoint.state-store", config);
        return stateStores.contains(stateStoreName);
    }

    static boolean hasDLQConfig(String channelName, Config config) {
        String propertyKey = getChannelPropertyKey(channelName, "failure-strategy", true);
        Optional<String> channelFailureStrategy = config.getOptionalValue(propertyKey, String.class);
        Optional<String> failureStrategy = channelFailureStrategy.or(() -> getConnectorProperty("failure-strategy", config));

        return failureStrategy.isPresent()
                && (failureStrategy.get().equals("dead-letter-queue")
                        || failureStrategy.get().equals("delayed-retry-topic"));
    }

    private static Optional<String> getConnectorProperty(String keySuffix, Config config) {
        return config.getOptionalValue("mp.messaging.connector." + KafkaConnector.CONNECTOR_NAME + "." + keySuffix,
                String.class);
    }

    private static List<String> getChannelProperties(String keySuffix, Config config) {
        List<String> values = new ArrayList<>();
        for (String propertyName : config.getPropertyNames()) {
            if (propertyName.startsWith("mp.messaging.incoming.") && propertyName.endsWith("." + keySuffix)) {
                values.add(config.getValue(propertyName, String.class));
            }
        }
        return values;
    }

    static String channelPropertyFormat = "mp.messaging.%s.%s.%s";

    static String getChannelPropertyKey(String channelName, String propertyName, boolean incoming) {
        return String.format(channelPropertyFormat, incoming ? "incoming" : "outgoing",
                channelName.contains(".") ? "\"" + channelName + "\"" : channelName, propertyName);
    }

    @BuildStep
    public void checkpointRedis(BuildProducer<AdditionalBeanBuildItem> additionalBean,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            Capabilities capabilities) {
        if (hasStateStoreConfig(REDIS_STATE_STORE, ConfigProvider.getConfig())) {
            Optional<String> checkpointStateType = getConnectorProperty("checkpoint.state-type", ConfigProvider.getConfig());
            checkpointStateType.ifPresent(
                    s -> reflectiveClass.produce(ReflectiveClassBuildItem.builder(s)
                            .reason(getClass().getName())
                            .methods().fields().build()));
            if (capabilities.isPresent(Capability.REDIS_CLIENT)) {
                additionalBean.produce(new AdditionalBeanBuildItem(RedisStateStore.Factory.class));
                additionalBean.produce(new AdditionalBeanBuildItem(DatabindProcessingStateCodec.Factory.class));
            } else {
                LOGGER.warnf(CHECKPOINT_STATE_STORE_MESSAGE, REDIS_STATE_STORE, "quarkus-redis-client");
            }
        }
    }

    @BuildStep
    public void checkpointHibernateReactive(BuildProducer<AdditionalBeanBuildItem> additionalBean, Capabilities capabilities) {
        if (hasStateStoreConfig(HIBERNATE_REACTIVE_STATE_STORE, ConfigProvider.getConfig())) {
            if (capabilities.isPresent(Capability.HIBERNATE_REACTIVE)) {
                additionalBean.produce(new AdditionalBeanBuildItem(HibernateReactiveStateStore.Factory.class));
            } else {
                LOGGER.warnf(CHECKPOINT_STATE_STORE_MESSAGE, HIBERNATE_REACTIVE_STATE_STORE, "quarkus-hibernate-reactive");
            }
        }
    }

    @BuildStep
    public void checkpointHibernateOrm(BuildProducer<AdditionalBeanBuildItem> additionalBean, Capabilities capabilities) {
        if (hasStateStoreConfig(HIBERNATE_ORM_STATE_STORE, ConfigProvider.getConfig())) {
            if (capabilities.isPresent(Capability.HIBERNATE_ORM)) {
                additionalBean.produce(new AdditionalBeanBuildItem(HibernateOrmStateStore.Factory.class));
            } else {
                LOGGER.warnf(CHECKPOINT_STATE_STORE_MESSAGE, HIBERNATE_ORM_STATE_STORE, "quarkus-hibernate-orm");
            }
        }
    }

    @BuildStep
    public void additionalJpaModel(BuildProducer<AdditionalJpaModelBuildItem> additionalJpaModel) {
        additionalJpaModel.produce(new AdditionalJpaModelBuildItem(CHECKPOINT_ENTITY_NAME));
        additionalJpaModel.produce(new AdditionalJpaModelBuildItem(CHECKPOINT_ENTITY_ID_NAME));
    }

    /**
     * Handles the serializer/deserializer detection and whether the graceful shutdown should be used in dev mode.
     */
    @BuildStep
    public void defaultChannelConfiguration(
            LaunchModeBuildItem launchMode,
            ReactiveMessagingKafkaBuildTimeConfig buildTimeConfig,
            CombinedIndexBuildItem combinedIndex,
            List<ConnectorManagedChannelBuildItem> channelsManagedByConnectors,
            BuildProducer<RunTimeConfigurationDefaultBuildItem> defaultConfigProducer,
            BuildProducer<GeneratedClassBuildItem> generatedClass,
            BuildProducer<ReflectiveClassBuildItem> reflection) {

        DefaultSerdeDiscoveryState discoveryState = new DefaultSerdeDiscoveryState(combinedIndex.getIndex());
        if (buildTimeConfig.serializerAutodetectionEnabled()) {
            discoverDefaultSerdeConfig(discoveryState, channelsManagedByConnectors, defaultConfigProducer,
                    buildTimeConfig.serializerGenerationEnabled() ? generatedClass : null, reflection);
        }

        if (launchMode.getLaunchMode().isDevOrTest()) {
            if (!buildTimeConfig.enableGracefulShutdownInDevAndTestMode()) {
                List<AnnotationInstance> incomings = discoveryState.findRepeatableAnnotationsOnMethods(DotNames.INCOMING);
                List<AnnotationInstance> outgoings = discoveryState.findRepeatableAnnotationsOnMethods(DotNames.OUTGOING);
                List<AnnotationInstance> channels = discoveryState.findAnnotationsOnInjectionPoints(DotNames.CHANNEL);
                List<AnnotationInstance> annotations = new ArrayList<>();
                annotations.addAll(incomings);
                annotations.addAll(outgoings);
                annotations.addAll(channels);
                for (AnnotationInstance annotation : annotations) {
                    String channelName = annotation.value().asString();
                    if (!discoveryState.isKafkaConnector(channelsManagedByConnectors, true, channelName)) {
                        continue;
                    }
                    String key = getChannelPropertyKey(channelName, "graceful-shutdown", true);
                    discoveryState.ifNotYetConfigured(key, () -> {
                        defaultConfigProducer.produce(new RunTimeConfigurationDefaultBuildItem(key, "false"));
                    });
                }
            }
        }
    }

    // visible for testing
    void discoverDefaultSerdeConfig(DefaultSerdeDiscoveryState discovery,
            List<ConnectorManagedChannelBuildItem> channelsManagedByConnectors,
            BuildProducer<RunTimeConfigurationDefaultBuildItem> config,
            BuildProducer<GeneratedClassBuildItem> generatedClass,
            BuildProducer<ReflectiveClassBuildItem> reflection) {
        Map<String, Result> alreadyGeneratedSerializers = new HashMap<>();
        Map<String, Result> alreadyGeneratedDeserializers = new HashMap<>();
        for (AnnotationInstance annotation : discovery.findRepeatableAnnotationsOnMethods(DotNames.INCOMING)) {
            String channelName = annotation.value().asString();
            if (!discovery.isKafkaConnector(channelsManagedByConnectors, true, channelName)) {
                continue;
            }

            MethodInfo method = annotation.target().asMethod();

            Type incomingType = getIncomingTypeFromMethod(method);

            processIncomingType(discovery, config, incomingType, channelName, generatedClass, reflection,
                    alreadyGeneratedDeserializers, alreadyGeneratedSerializers);
        }

        for (AnnotationInstance annotation : discovery.findRepeatableAnnotationsOnMethods(DotNames.OUTGOING)) {
            String channelName = annotation.value().asString();
            if (!discovery.isKafkaConnector(channelsManagedByConnectors, false, channelName)) {
                continue;
            }

            MethodInfo method = annotation.target().asMethod();

            Type outgoingType = getOutgoingTypeFromMethod(method);
            processOutgoingType(discovery, outgoingType, (keySerializer, valueSerializer) -> {
                produceRuntimeConfigurationDefaultBuildItem(discovery, config,
                        getChannelPropertyKey(channelName, "key.serializer", false), keySerializer);
                produceRuntimeConfigurationDefaultBuildItem(discovery, config,
                        getChannelPropertyKey(channelName, "value.serializer", false), valueSerializer);

                handleAdditionalProperties(channelName, false, discovery, config, keySerializer, valueSerializer);
            }, generatedClass, reflection, alreadyGeneratedSerializers);
        }

        for (AnnotationInstance annotation : discovery.findAnnotationsOnInjectionPoints(DotNames.CHANNEL)) {
            String channelName = annotation.value().asString();
            if (!discovery.isKafkaConnector(channelsManagedByConnectors, false, channelName)
                    && !discovery.isKafkaConnector(channelsManagedByConnectors, true, channelName)) {
                continue;
            }

            Type injectionPointType = getInjectionPointType(annotation);
            if (injectionPointType == null) {
                continue;
            }

            Type incomingType = getIncomingTypeFromChannelInjectionPoint(injectionPointType);

            processIncomingType(discovery, config, incomingType, channelName, generatedClass, reflection,
                    alreadyGeneratedDeserializers, alreadyGeneratedSerializers);

            processKafkaTransactions(discovery, config, channelName, injectionPointType);

            if (isKafkaRequestReplyEmitter(injectionPointType)) {
                Type requestType = injectionPointType.asParameterizedType().arguments().get(0);
                Type replyType = injectionPointType.asParameterizedType().arguments().get(1);
                processOutgoingType(discovery, requestType, (keySerializer, valueSerializer) -> {
                    produceRuntimeConfigurationDefaultBuildItem(discovery, config,
                            getChannelPropertyKey(channelName, "key.serializer", false), keySerializer);
                    produceRuntimeConfigurationDefaultBuildItem(discovery, config,
                            getChannelPropertyKey(channelName, "value.serializer", false), valueSerializer);
                }, generatedClass, reflection, alreadyGeneratedSerializers);
                extractKeyValueType(replyType, (key, value, isBatchType) -> {
                    Result keyDeserializer = deserializerFor(discovery, key, true, channelName, generatedClass, reflection,
                            alreadyGeneratedDeserializers, alreadyGeneratedSerializers);
                    Result valueDeserializer = deserializerFor(discovery, value, false, channelName, generatedClass, reflection,
                            alreadyGeneratedDeserializers, alreadyGeneratedSerializers);

                    produceRuntimeConfigurationDefaultBuildItem(discovery, config,
                            getChannelPropertyKey(channelName, "reply.key.deserializer", false), keyDeserializer);
                    produceRuntimeConfigurationDefaultBuildItem(discovery, config,
                            getChannelPropertyKey(channelName, "reply.value.deserializer", false), valueDeserializer);
                    handleAdditionalProperties(channelName, false, discovery, config, keyDeserializer, valueDeserializer);
                });
            } else {
                Type outgoingType = getOutgoingTypeFromChannelInjectionPoint(injectionPointType);
                processOutgoingType(discovery, outgoingType, (keySerializer, valueSerializer) -> {
                    produceRuntimeConfigurationDefaultBuildItem(discovery, config,
                            getChannelPropertyKey(channelName, "key.serializer", false), keySerializer);
                    produceRuntimeConfigurationDefaultBuildItem(discovery, config,
                            getChannelPropertyKey(channelName, "value.serializer", false), valueSerializer);

                    handleAdditionalProperties(channelName, false, discovery, config, keySerializer, valueSerializer);
                }, generatedClass, reflection, alreadyGeneratedSerializers);
            }
        }
    }

    private void processKafkaTransactions(DefaultSerdeDiscoveryState discovery,
            BuildProducer<RunTimeConfigurationDefaultBuildItem> config, String channelName, Type injectionPointType) {
        if (injectionPointType != null && isKafkaTransactionsEmitter(injectionPointType)) {
            String transactionalIdKey = getChannelPropertyKey(channelName, "transactional.id", false);
            String enableIdempotenceKey = getChannelPropertyKey(channelName, "enable.idempotence", false);
            String acksKey = getChannelPropertyKey(channelName, "acks", false);
            LOGGER.infof("Transactional producer detected for channel '%s', setting following default config values: "
                    + "'" + transactionalIdKey + "=${quarkus.application.name}-${channelName}', "
                    + "'" + enableIdempotenceKey + "=true', "
                    + "'" + acksKey + "=all'", channelName);
            produceRuntimeConfigurationDefaultBuildItem(discovery, config, transactionalIdKey,
                    "${quarkus.application.name}-" + channelName);
            produceRuntimeConfigurationDefaultBuildItem(discovery, config, enableIdempotenceKey, "true");
            produceRuntimeConfigurationDefaultBuildItem(discovery, config, acksKey, "all");
        }
    }

    private void processIncomingType(DefaultSerdeDiscoveryState discovery,
            BuildProducer<RunTimeConfigurationDefaultBuildItem> config, Type incomingType, String channelName,
            BuildProducer<GeneratedClassBuildItem> generatedClass, BuildProducer<ReflectiveClassBuildItem> reflection,
            Map<String, Result> alreadyGeneratedDeserializers, Map<String, Result> alreadyGeneratedSerializers) {
        extractKeyValueType(incomingType, (key, value, isBatchType) -> {
            Result keyDeserializer = deserializerFor(discovery, key, true, channelName, generatedClass, reflection,
                    alreadyGeneratedDeserializers, alreadyGeneratedSerializers);
            Result valueDeserializer = deserializerFor(discovery, value, false, channelName, generatedClass, reflection,
                    alreadyGeneratedDeserializers, alreadyGeneratedSerializers);

            produceRuntimeConfigurationDefaultBuildItem(discovery, config,
                    getChannelPropertyKey(channelName, "key.deserializer", true), keyDeserializer);
            produceRuntimeConfigurationDefaultBuildItem(discovery, config,
                    getChannelPropertyKey(channelName, "value.deserializer", true), valueDeserializer);
            if (Boolean.TRUE.equals(isBatchType)) {
                produceRuntimeConfigurationDefaultBuildItem(discovery, config,
                        getChannelPropertyKey(channelName, "batch", true), "true");
            }

            handleAdditionalProperties(channelName, true, discovery, config, keyDeserializer, valueDeserializer);
        });
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

    private void handleAdditionalProperties(String channelName, boolean incoming, DefaultSerdeDiscoveryState discovery,
            BuildProducer<RunTimeConfigurationDefaultBuildItem> config, Result... results) {
        for (Result result : results) {
            if (result == null) {
                continue;
            }

            result.additionalProperties.forEach((key, value) -> {
                String configKey = getChannelPropertyKey(channelName, key, incoming);
                produceRuntimeConfigurationDefaultBuildItem(discovery, config, configKey, value);
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

        if (discovery.shouldNotConfigure(key)) {
            return;
        }

        discovery.ifNotYetConfigured(key, () -> {
            config.produce(new RunTimeConfigurationDefaultBuildItem(key, value));
        });
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
                    || (isFlowPublisher(returnType) && parametersCount == 1)
                    || (isPublisherBuilder(returnType) && parametersCount == 1)
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
            if (incomingType != null &&
                    (isPublisher(incomingType)
                            || isFlowPublisher(incomingType)
                            || isPublisherBuilder(incomingType)
                            || isMulti(incomingType))) {
                incomingType = incomingType.asParameterizedType().arguments().get(0);
            }
        }
        return incomingType;
    }

    private Type getIncomingTypeFromChannelInjectionPoint(Type injectionPointType) {
        if (injectionPointType == null) {
            return null;
        }

        if (isPublisher(injectionPointType)
                || isPublisherBuilder(injectionPointType)
                || isFlowPublisher(injectionPointType)
                || isMulti(injectionPointType)) {
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
                || (isFlowPublisher(returnType) && parametersCount == 0)
                || (isPublisherBuilder(returnType) && parametersCount == 0)
                || (isMulti(returnType) && parametersCount == 0)
                || (isMultiSplitter(returnType) && parametersCount == 0)
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
                    || (isFlowPublisher(returnType) && parametersCount == 1)
                    || (isPublisherBuilder(returnType) && parametersCount == 1)
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
                    && (isPublisher(outgoingType)
                            || isFlowPublisher(outgoingType)
                            || isPublisherBuilder(outgoingType)
                            || isMulti(outgoingType))) {
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

        if (isEmitter(injectionPointType) || isMutinyEmitter(injectionPointType) || isContextualEmitter(injectionPointType)
                || isKafkaTransactionsEmitter(injectionPointType)) {
            return injectionPointType.asParameterizedType().arguments().get(0);
        } else {
            return null;
        }
    }

    private void processOutgoingType(DefaultSerdeDiscoveryState discovery, Type outgoingType,
            BiConsumer<Result, Result> serializerAcceptor, BuildProducer<GeneratedClassBuildItem> generatedClass,
            BuildProducer<ReflectiveClassBuildItem> reflection, Map<String, Result> alreadyGeneratedSerializer) {
        extractKeyValueType(outgoingType, (key, value, isBatch) -> {
            Result keySerializer = serializerFor(discovery, key, generatedClass, reflection,
                    alreadyGeneratedSerializer);
            Result valueSerializer = serializerFor(discovery, value, generatedClass, reflection,
                    alreadyGeneratedSerializer);
            serializerAcceptor.accept(keySerializer, valueSerializer);
        });
    }

    private void extractKeyValueType(Type type, TriConsumer<Type, Type, Boolean> keyValueTypeAcceptor) {
        if (type == null) {
            return;
        }

        if (isTargeted(type)) {
            return;
        }

        if (isGenericPayload(type)) {
            extractKeyValueType(type.asParameterizedType().arguments().get(0), keyValueTypeAcceptor);
            return;
        }

        if (isMessage(type)) {
            List<Type> typeArguments = type.asParameterizedType().arguments();
            Type messageTypeParameter = typeArguments.get(0);
            if (isList(messageTypeParameter)) {
                List<Type> messageListTypeArguments = messageTypeParameter.asParameterizedType().arguments();
                keyValueTypeAcceptor.accept(null, messageListTypeArguments.get(0), true);
            } else {
                keyValueTypeAcceptor.accept(null, messageTypeParameter, false);
            }
        } else if (isList(type)) {
            List<Type> typeArguments = type.asParameterizedType().arguments();
            keyValueTypeAcceptor.accept(null, typeArguments.get(0), true);
        } else if (isKafkaRecord(type) || isRecord(type) || isProducerRecord(type) || isConsumerRecord(type)
                || isKeyedMulti(type)) {
            List<Type> typeArguments = type.asParameterizedType().arguments();
            keyValueTypeAcceptor.accept(typeArguments.get(0), typeArguments.get(1), false);
        } else if (isConsumerRecords(type) || isKafkaBatchRecord(type)) {
            List<Type> typeArguments = type.asParameterizedType().arguments();
            keyValueTypeAcceptor.accept(typeArguments.get(0), typeArguments.get(1), true);
        } else if (isRawMessage(type)) {
            keyValueTypeAcceptor.accept(null, type, false);
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

    private static boolean isFlowPublisher(Type type) {
        // raw type Flow.Publisher is wrong, must be Flow.Publisher<Something>
        return DotNames.FLOW_PUBLISHER.equals(type.name())
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
        // raw type ContextualEmitter is wrong, must be ContextualEmitter<Something>
        return DotNames.CONTEXTUAL_EMITTER.equals(type.name())
                && type.kind() == Type.Kind.PARAMETERIZED_TYPE
                && type.asParameterizedType().arguments().size() == 1;
    }

    private static boolean isKafkaTransactionsEmitter(Type type) {
        // raw type KafkaTransactions is wrong, must be KafkaTransactions<Something>
        return DotNames.KAFKA_TRANSACTIONS_EMITTER.equals(type.name())
                && type.kind() == Type.Kind.PARAMETERIZED_TYPE
                && type.asParameterizedType().arguments().size() == 1;
    }

    private static boolean isKafkaRequestReplyEmitter(Type type) {
        // raw type KafkaRequestReply is wrong, must be KafkaRequestReply<Request, Reply>
        return DotNames.KAFKA_REQUEST_REPLY_EMITTER.equals(type.name())
                && type.kind() == Type.Kind.PARAMETERIZED_TYPE
                && type.asParameterizedType().arguments().size() == 2;
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

    private static boolean isKeyedMulti(Type type) {
        return ReactiveMessagingDotNames.KEYED_MULTI.equals(type.name())
                && type.kind() == Type.Kind.PARAMETERIZED_TYPE
                && type.asParameterizedType().arguments().size() == 2;
    }

    private static boolean isProducerRecord(Type type) {
        // raw type ProducerRecord is wrong, must be ProducerRecord<Something, SomethingElse>
        return DotNames.PRODUCER_RECORD.equals(type.name())
                && type.kind() == Type.Kind.PARAMETERIZED_TYPE
                && type.asParameterizedType().arguments().size() == 2;
    }

    private static boolean isList(Type type) {
        return DotNames.LIST.equals(type.name())
                && type.kind() == Type.Kind.PARAMETERIZED_TYPE
                && type.asParameterizedType().arguments().size() == 1;
    }

    private static boolean isKafkaBatchRecord(Type type) {
        return DotNames.KAFKA_BATCH_RECORD.equals(type.name())
                && type.kind() == Type.Kind.PARAMETERIZED_TYPE
                && type.asParameterizedType().arguments().size() == 2;
    }

    private static boolean isConsumerRecords(Type type) {
        return DotNames.CONSUMER_RECORDS.equals(type.name())
                && type.kind() == Type.Kind.PARAMETERIZED_TYPE
                && type.asParameterizedType().arguments().size() == 2;
    }

    private static boolean isTargeted(Type type) {
        return DotNames.TARGETED.equals(type.name())
                || DotNames.TARGETED_MESSAGES.equals(type.name());
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
            Map.entry(DotName.createSimple(io.vertx.core.buffer.Buffer.class.getName()),   io.quarkus.kafka.client.serialization.BufferDeserializer.class.getName()),
            Map.entry(DotName.createSimple(io.vertx.core.json.JsonObject.class.getName()), io.quarkus.kafka.client.serialization.JsonObjectDeserializer.class.getName()),
            Map.entry(DotName.createSimple(io.vertx.core.json.JsonArray.class.getName()),  io.quarkus.kafka.client.serialization.JsonArrayDeserializer.class.getName())
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
            Map.entry(DotName.createSimple(io.vertx.core.buffer.Buffer.class.getName()),   io.quarkus.kafka.client.serialization.BufferSerializer.class.getName()),
            Map.entry(DotName.createSimple(io.vertx.core.json.JsonObject.class.getName()), io.quarkus.kafka.client.serialization.JsonObjectSerializer.class.getName()),
            Map.entry(DotName.createSimple(io.vertx.core.json.JsonArray.class.getName()),  io.quarkus.kafka.client.serialization.JsonArraySerializer.class.getName())
    );
    // @formatter:on

    private Result deserializerFor(DefaultSerdeDiscoveryState discovery,
            Type type,
            boolean key,
            String channelName,
            BuildProducer<GeneratedClassBuildItem> generatedClass,
            BuildProducer<ReflectiveClassBuildItem> reflection,
            Map<String, Result> alreadyGeneratedDeserializers,
            Map<String, Result> alreadyGeneratedSerializers) {
        Result result = serializerDeserializerFor(discovery, type, false);
        if (result != null && !result.exists) {
            // avoid returning Result.nonexistent() to callers, they expect a non-null Result to always be known
            return null;
        }
        // if result is null, generate a jackson serializer, generatedClass is null if the generation is disabled.
        // also, only generate the serializer/deserializer for classes and only generate once
        if (result == null && type != null && generatedClass != null && type.kind() == Type.Kind.CLASS) {
            // Check if already generated
            result = alreadyGeneratedDeserializers.get(type.name().toString());
            if (result == null) {
                String clazz = JacksonSerdeGenerator.generateDeserializer(generatedClass, type);
                LOGGER.infof("Generating Jackson deserializer for type %s", type.name().toString());
                // Deserializers are access by reflection.
                reflection.produce(
                        ReflectiveClassBuildItem.builder(clazz)
                                .reason(getClass().getName())
                                .methods().build());
                alreadyGeneratedDeserializers.put(type.name().toString(), result);
                // if the channel has a DLQ config generate a serializer as well
                if (hasDLQConfig(channelName, discovery.getConfig())) {
                    Result serializer = serializerFor(discovery, type, generatedClass, reflection, alreadyGeneratedSerializers);
                    if (serializer != null) {
                        result = Result.of(clazz)
                                .with(key, "dead-letter-queue.key.serializer", serializer.value)
                                .with(!key, "dead-letter-queue.value.serializer", serializer.value);
                    }
                } else {
                    result = Result.of(clazz);
                }
            }
        }
        return result;
    }

    private Result serializerFor(DefaultSerdeDiscoveryState discovery, Type type,
            BuildProducer<GeneratedClassBuildItem> generatedClass,
            BuildProducer<ReflectiveClassBuildItem> reflection,
            Map<String, Result> alreadyGeneratedSerializers) {
        Result result = serializerDeserializerFor(discovery, type, true);
        if (result != null && !result.exists) {
            // avoid returning Result.nonexistent() to callers, they expect a non-null Result to always be known
            return null;
        }
        // if result is null, generate a jackson deserializer, generatedClass is null if the generation is disabled.
        // also, only generate the serializer/deserializer for classes and only generate once
        if (result == null && type != null && generatedClass != null && type.kind() == Type.Kind.CLASS) {
            // Check if already generated
            result = alreadyGeneratedSerializers.get(type.name().toString());
            if (result == null) {
                String clazz = JacksonSerdeGenerator.generateSerializer(generatedClass, type);
                LOGGER.infof("Generating Jackson serializer for type %s", type.name().toString());
                // Serializers are access by reflection.
                reflection.produce(
                        ReflectiveClassBuildItem.builder(clazz)
                                .reason(getClass().getName())
                                .methods().build());
                result = Result.of(clazz);
                alreadyGeneratedSerializers.put(type.name().toString(), result);
            }
        }

        return result;
    }

    private Result serializerDeserializerFor(DefaultSerdeDiscoveryState discovery, Type type, boolean serializer) {
        if (type == null) {
            return null;
        }
        DotName typeName = type.name();

        // Serializer/deserializer implementations
        ClassInfo implementation = discovery.getImplementorOfWithTypeArgument(
                serializer ? DotNames.KAFKA_SERIALIZER : DotNames.KAFKA_DESERIALIZER, typeName);
        if (implementation != null) {
            return Result.of(implementation.name().toString());
        }

        // statically known serializer/deserializer
        Map<DotName, String> map = serializer ? KNOWN_SERIALIZERS : KNOWN_DESERIALIZERS;
        if (map.containsKey(typeName)) {
            return Result.of(map.get(typeName));
        }

        // Avro generated class or GenericRecord (serializer/deserializer provided by Confluent or Apicurio)
        boolean isAvroGenerated = discovery.isAvroGenerated(typeName);
        if (isAvroGenerated || DotNames.AVRO_GENERIC_RECORD.equals(typeName)) {
            int avroLibraries = 0;
            avroLibraries += discovery.hasConfluent() ? 1 : 0;
            avroLibraries += discovery.hasApicurio1() ? 1 : 0;
            avroLibraries += discovery.hasApicurio2Avro() ? 1 : 0;
            if (avroLibraries > 1) {
                LOGGER.debugf("Skipping Avro serde autodetection for %s, because multiple Avro serde libraries are present",
                        typeName);
                return Result.nonexistent();
            }

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
            } else if (discovery.hasApicurio2Avro()) {
                return serializer
                        ? Result.of("io.apicurio.registry.serde.avro.AvroKafkaSerializer")
                        : Result.of("io.apicurio.registry.serde.avro.AvroKafkaDeserializer")
                                .with(isAvroGenerated, "apicurio.registry.use-specific-avro-reader", "true");
            } else {
                // we know it is an Avro type, no point in serializing it as JSON
                return Result.nonexistent();
            }
        }

        //TODO autodiscovery of json serdes

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
        reflectiveClass.produce(
                ReflectiveClassBuildItem.builder(type.name().toString())
                        .reason(getClass().getName())
                        .methods().fields().build());
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
            extractKeyValueType(type, (key, value, isBatch) -> {
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
        String configKey = getChannelPropertyKey(channelName, (isKey ? "key" : "value") + "." +
                (serializer ? "serializer" : "deserializer"), !serializer);
        ConfigValue configValue = config.getConfigValue(configKey);
        if (configValue.getValue() != null) {
            DotName serdeName = DotName.createSimple(configValue.getValue());
            return serializer ? isSubclassOfJsonSerializer(index, serdeName) : isSubclassOfJsonDeserializer(index, serdeName);
        }
        return false;
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
