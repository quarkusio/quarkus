package io.quarkus.mongodb.deployment;

import static io.quarkus.arc.processor.BuildExtension.Key.INJECTION_POINTS;
import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;
import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;
import static io.quarkus.mongodb.runtime.MongoConfig.DEFAULT_CLIENT_NAME;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Singleton;

import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.pojo.PropertyCodecProvider;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;
import org.bson.types.ObjectId;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import com.mongodb.client.MongoClient;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.client.model.changestream.UpdateDescription;
import com.mongodb.event.CommandListener;
import com.mongodb.reactivestreams.client.ReactiveContextProvider;
import com.mongodb.spi.dns.DnsClientProvider;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.BeanDiscoveryFinishedBuildItem;
import io.quarkus.arc.deployment.BeanRegistrationPhaseBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.arc.processor.InjectionPointInfo;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.annotations.Weak;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.ModuleEnableNativeAccessBuildItem;
import io.quarkus.deployment.builditem.SslNativeConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.metrics.MetricsCapabilityBuildItem;
import io.quarkus.mongodb.MongoClientName;
import io.quarkus.mongodb.deployment.spi.MongoClientsBuildItem;
import io.quarkus.mongodb.metrics.MicrometerCommandListener;
import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.quarkus.mongodb.runtime.MongoClientCustomizer;
import io.quarkus.mongodb.runtime.MongoClientRecorder;
import io.quarkus.mongodb.runtime.MongoClientSupport;
import io.quarkus.mongodb.runtime.MongoClients;
import io.quarkus.mongodb.runtime.MongoConfig;
import io.quarkus.mongodb.runtime.MongoReactiveContextProvider;
import io.quarkus.mongodb.runtime.MongoServiceBindingConverter;
import io.quarkus.mongodb.runtime.dns.MongoDnsClient;
import io.quarkus.mongodb.runtime.dns.MongoDnsClientProvider;
import io.quarkus.mongodb.tracing.MongoTracingCommandListener;
import io.quarkus.runtime.metrics.MetricsFactory;
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;
import io.quarkus.vertx.deployment.VertxBuildItem;

public class MongoClientProcessor {
    private static final DotName MONGO_CLIENT_ANNOTATION = DotName.createSimple(MongoClientName.class.getName());

    private static final DotName MONGO_CLIENT = DotName.createSimple(MongoClient.class.getName());
    private static final DotName REACTIVE_MONGO_CLIENT = DotName.createSimple(ReactiveMongoClient.class.getName());

    private static final DotName MONGO_CLIENT_CUSTOMIZER = DotName.createSimple(MongoClientCustomizer.class.getName());

    private static final String SERVICE_BINDING_INTERFACE_NAME = "io.quarkus.kubernetes.service.binding.runtime.ServiceBindingConverter";

    @BuildStep
    AdditionalIndexedClassesBuildItem includeBsonTypesToIndex() {
        return new AdditionalIndexedClassesBuildItem(
                "org.bson.types.BasicBSONList",
                "org.bson.types.Binary",
                "org.bson.types.BSONTimestamp",
                "org.bson.types.Code",
                "org.bson.types.CodeWithScope",
                "org.bson.types.CodeWScope",
                "org.bson.types.Decimal128",
                "org.bson.types.MaxKey",
                "org.bson.types.MinKey",
                "org.bson.types.ObjectId",
                "org.bson.types.StringRangeSet",
                "org.bson.types.Symbol");
    }

    @BuildStep
    AdditionalIndexedClassesBuildItem includeDnsTypesToIndex() {
        return new AdditionalIndexedClassesBuildItem(
                MongoDnsClientProvider.class.getName(),
                MongoDnsClient.class.getName());
    }

    @BuildStep
    AdditionalIndexedClassesBuildItem includeMongoCommandListener(MongoClientBuildTimeConfig buildTimeConfig) {
        if (buildTimeConfig.tracingEnabled()) {
            return new AdditionalIndexedClassesBuildItem(
                    MongoTracingCommandListener.class.getName(),
                    MongoReactiveContextProvider.class.getName());
        }
        return new AdditionalIndexedClassesBuildItem();
    }

    @BuildStep
    void includeMongoCommandMetricListener(
            BuildProducer<AdditionalIndexedClassesBuildItem> additionalIndexedClasses,
            MongoClientBuildTimeConfig buildTimeConfig,
            Optional<MetricsCapabilityBuildItem> metricsCapability) {
        if (!buildTimeConfig.metricsEnabled()) {
            return;
        }
        boolean withMicrometer = metricsCapability.map(cap -> cap.metricsSupported(MetricsFactory.MICROMETER))
                .orElse(false);
        if (withMicrometer) {
            additionalIndexedClasses.produce(new AdditionalIndexedClassesBuildItem(MicrometerCommandListener.class.getName()));
        }
    }

    @BuildStep
    public void registerDnsProvider(BuildProducer<NativeImageResourceBuildItem> nativeProducer) {
        nativeProducer.produce(new NativeImageResourceBuildItem("META-INF/services/" + DnsClientProvider.class.getName()));
    }

    @BuildStep
    CodecProviderBuildItem collectCodecProviders(CombinedIndexBuildItem indexBuildItem) {
        Collection<ClassInfo> codecProviderClasses = indexBuildItem.getIndex()
                .getAllKnownImplementations(DotName.createSimple(CodecProvider.class.getName()));
        List<String> names = codecProviderClasses.stream().map(ci -> ci.name().toString()).collect(Collectors.toList());
        return new CodecProviderBuildItem(names);
    }

    @BuildStep
    PropertyCodecProviderBuildItem collectPropertyCodecProviders(CombinedIndexBuildItem indexBuildItem) {
        Collection<ClassInfo> propertyCodecProviderClasses = indexBuildItem.getIndex()
                .getAllKnownImplementations(DotName.createSimple(PropertyCodecProvider.class.getName()));
        List<String> names = propertyCodecProviderClasses.stream().map(ci -> ci.name().toString()).collect(Collectors.toList());
        return new PropertyCodecProviderBuildItem(names);
    }

    @BuildStep
    BsonDiscriminatorBuildItem collectBsonDiscriminators(CombinedIndexBuildItem indexBuildItem) {
        List<String> names = new ArrayList<>();
        DotName bsonDiscriminatorName = DotName.createSimple(BsonDiscriminator.class.getName());
        for (AnnotationInstance annotationInstance : indexBuildItem.getIndex().getAnnotations(bsonDiscriminatorName)) {
            names.add(annotationInstance.target().asClass().name().toString());
        }
        return new BsonDiscriminatorBuildItem(names);
    }

    @BuildStep
    CommandListenerBuildItem collectCommandListeners(CombinedIndexBuildItem indexBuildItem) {
        Collection<ClassInfo> commandListenerClasses = indexBuildItem.getIndex()
                .getAllKnownImplementations(DotName.createSimple(CommandListener.class.getName()));
        List<String> names = commandListenerClasses.stream()
                .map(ci -> ci.name().toString())
                .collect(Collectors.toList());
        return new CommandListenerBuildItem(names);
    }

    @BuildStep
    ContextProviderBuildItem collectContextProviders(CombinedIndexBuildItem indexBuildItem) {
        Collection<ClassInfo> contextProviders = indexBuildItem.getIndex()
                .getAllKnownImplementations(DotName.createSimple(ReactiveContextProvider.class.getName()));
        List<String> names = contextProviders.stream()
                .map(ci -> ci.name().toString())
                .collect(Collectors.toList());
        return new ContextProviderBuildItem(names);
    }

    @BuildStep
    List<ReflectiveClassBuildItem> addExtensionPointsToNative(CodecProviderBuildItem codecProviders,
            PropertyCodecProviderBuildItem propertyCodecProviders, BsonDiscriminatorBuildItem bsonDiscriminators,
            CommandListenerBuildItem commandListeners,
            ContextProviderBuildItem contextProviders) {
        List<String> reflectiveClassNames = new ArrayList<>();
        reflectiveClassNames.addAll(codecProviders.getCodecProviderClassNames());
        reflectiveClassNames.addAll(propertyCodecProviders.getPropertyCodecProviderClassNames());
        reflectiveClassNames.addAll(bsonDiscriminators.getBsonDiscriminatorClassNames());
        reflectiveClassNames.addAll(commandListeners.getCommandListenerClassNames());
        reflectiveClassNames.addAll(contextProviders.getContextProviderClassNames());

        List<ReflectiveClassBuildItem> reflectiveClass = reflectiveClassNames.stream()
                .map(s -> ReflectiveClassBuildItem.builder(s)
                        .reason(getClass().getName())
                        .methods().build())
                .collect(Collectors.toCollection(ArrayList::new));
        // ChangeStreamDocument needs to be registered for reflection with its fields.
        reflectiveClass.add(ReflectiveClassBuildItem.builder(ChangeStreamDocument.class)
                .reason(getClass().getName())
                .methods().fields().build());
        reflectiveClass.add(ReflectiveClassBuildItem.builder(UpdateDescription.class)
                .reason(getClass().getName())
                .methods().build());
        // ObjectId is often used on identifier, so we also register it
        reflectiveClass.add(ReflectiveClassBuildItem.builder(ObjectId.class)
                .reason(getClass().getName())
                .methods().fields().build());
        return reflectiveClass;
    }

    @BuildStep
    public void mongoClients(
            CombinedIndexBuildItem indexBuildItem,
            BeanRegistrationPhaseBuildItem registrationPhase,
            MongoClientBuildTimeConfig mongoClientBuildTimeConfig,
            BuildProducer<io.quarkus.mongodb.deployment.spi.MongoClientBuildItem> mongoClient) {

        IndexView indexView = indexBuildItem.getIndex();
        Collection<AnnotationInstance> mongoClientAnnotations = indexView.getAnnotations(MONGO_CLIENT_ANNOTATION);
        // Unique names
        Set<String> mongoClients = new HashSet<>();
        for (AnnotationInstance annotation : mongoClientAnnotations) {
            mongoClients.add(annotation.value().asString());
        }
        for (String name : mongoClients) {
            mongoClient.produce(io.quarkus.mongodb.deployment.spi.MongoClientBuildItem.of(name));
        }

        boolean defaultUnremoveable = false;
        if (mongoClientBuildTimeConfig.forceDefaultClients()) {
            defaultUnremoveable = true;
        } else {
            for (InjectionPointInfo injectionPoint : registrationPhase.getContext().get(INJECTION_POINTS)) {
                DotName injectionPointType = injectionPoint.getRequiredType().name();
                if ((injectionPointType.equals(MONGO_CLIENT) || injectionPointType.equals(REACTIVE_MONGO_CLIENT))
                        && injectionPoint.hasDefaultedQualifier()) {
                    defaultUnremoveable = true;
                }
            }
        }
        if (defaultUnremoveable) {
            mongoClient.produce(io.quarkus.mongodb.deployment.spi.MongoClientBuildItem.defaultClientUnremovable());
        } else {
            mongoClient.produce(io.quarkus.mongodb.deployment.spi.MongoClientBuildItem.defaultClient());
        }
    }

    @BuildStep
    @Deprecated(forRemoval = true, since = "3.33")
    void deprecatedMongoClientNames(
            List<MongoClientNameBuildItem> deprecatedMongoClientNames,
            BuildProducer<io.quarkus.mongodb.deployment.spi.MongoClientBuildItem> mongoClientName) {
        for (MongoClientNameBuildItem deprecatedMongoClientName : deprecatedMongoClientNames) {
            mongoClientName.produce(
                    io.quarkus.mongodb.deployment.spi.MongoClientBuildItem.ofUnremovable(deprecatedMongoClientName.getName()));
        }
    }

    @BuildStep
    MongoClientsBuildItem mongoClients(List<io.quarkus.mongodb.deployment.spi.MongoClientBuildItem> mongoClients) {
        Map<String, io.quarkus.mongodb.deployment.spi.MongoClientBuildItem> names = new HashMap<>();
        for (io.quarkus.mongodb.deployment.spi.MongoClientBuildItem mongoClient : mongoClients) {
            io.quarkus.mongodb.deployment.spi.MongoClientBuildItem existent = names.put(mongoClient.getName(), mongoClient);
            if (existent == null) {
                names.put(mongoClient.getName(), mongoClient);
            } else if (existent.isUnremovable() && !mongoClient.isUnremovable()) {
                names.put(mongoClient.getName(), existent);
            }
        }
        return MongoClientsBuildItem.of(names.values().stream().toList());
    }

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.MONGODB_CLIENT);
    }

    @BuildStep
    ExtensionSslNativeSupportBuildItem ssl() {
        return new ExtensionSslNativeSupportBuildItem(Feature.MONGODB_CLIENT);
    }

    @BuildStep
    @Record(STATIC_INIT)
    io.quarkus.mongodb.deployment.spi.MongoConnectionPoolListenerBuildItem setupMetrics(
            MongoClientBuildTimeConfig buildTimeConfig,
            MongoClientRecorder recorder,
            Optional<MetricsCapabilityBuildItem> metricsCapability) {

        // Construction of MongoClient isn't compatible with the MetricsFactoryConsumer pattern.
        // Use a supplier to defer construction of the pool listener for the supported metrics system
        if (buildTimeConfig.metricsEnabled() && metricsCapability.isPresent()) {
            if (metricsCapability.get().metricsSupported(MetricsFactory.MICROMETER)) {
                return io.quarkus.mongodb.deployment.spi.MongoConnectionPoolListenerBuildItem.of(
                        recorder.createMicrometerConnectionPoolListener());
            }
        }
        return null;
    }

    @BuildStep
    void additionalBeans(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        // add the @MongoClientName class otherwise it won't be registered as a qualifier
        additionalBeans.produce(AdditionalBeanBuildItem.builder().addBeanClass(MongoClientName.class).build());
        // make MongoClients an unremovable bean
        additionalBeans.produce(AdditionalBeanBuildItem.builder().addBeanClasses(MongoClients.class).setUnremovable().build());
    }

    @Record(STATIC_INIT)
    @BuildStep
    void build(
            MongoClientRecorder recorder,
            SslNativeConfigBuildItem sslNativeConfig,
            CodecProviderBuildItem codecProvider,
            PropertyCodecProviderBuildItem propertyCodecProvider,
            BsonDiscriminatorBuildItem bsonDiscriminator,
            CommandListenerBuildItem commandListener,
            ContextProviderBuildItem contextProvider,
            List<io.quarkus.mongodb.deployment.spi.MongoConnectionPoolListenerBuildItem> connectionPoolListeners,
            List<MongoConnectionPoolListenerBuildItem> connectionPoolListenerProvider,
            BuildProducer<AdditionalBeanBuildItem> additionalBeanBuildItemProducer,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItemBuildProducer) {

        List<String> codecProviderClassNames = codecProvider.getCodecProviderClassNames();
        AdditionalBeanBuildItem.Builder additionalBeansBuilder = AdditionalBeanBuildItem.builder();
        additionalBeansBuilder.setDefaultScope(DotNames.SINGLETON);
        for (String name : codecProviderClassNames) {
            additionalBeansBuilder.addBeanClass(name);
        }
        for (String name : propertyCodecProvider.getPropertyCodecProviderClassNames()) {
            additionalBeansBuilder.addBeanClass(name);
        }
        for (String name : commandListener.getCommandListenerClassNames()) {
            additionalBeansBuilder.addBeanClass(name);
        }
        for (String name : contextProvider.getContextProviderClassNames()) {
            additionalBeansBuilder.addBeanClass(name);
        }
        additionalBeanBuildItemProducer.produce(additionalBeansBuilder.build());

        // create MongoClientSupport as a synthetic bean as it's used in AbstractMongoClientProducer
        syntheticBeanBuildItemBuildProducer.produce(SyntheticBeanBuildItem.configure(MongoClientSupport.class)
                .scope(Singleton.class)
                .supplier(recorder.mongoClientSupportSupplier(
                        bsonDiscriminator.getBsonDiscriminatorClassNames(),
                        connectionPoolListeners.stream()
                                .map(io.quarkus.mongodb.deployment.spi.MongoConnectionPoolListenerBuildItem::getConnectionPoolListener)
                                .toList(),
                        connectionPoolListenerProvider.stream()
                                .map(MongoConnectionPoolListenerBuildItem::getConnectionPoolListener).toList(),
                        sslNativeConfig.isExplicitlyDisabled()))
                .done());
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    void generateClientBeans(
            MongoClientRecorder recorder,
            io.quarkus.mongodb.deployment.spi.MongoClientsBuildItem mongoClients,
            List<MongoUnremovableClientsBuildItem> unremovableClients,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItemBuildProducer,
            VertxBuildItem vertxBuildItem) {

        for (io.quarkus.mongodb.deployment.spi.MongoClientBuildItem mongoClient : mongoClients.getMongoClients()) {
            boolean unremovable = mongoClient.isUnremovable() || !unremovableClients.isEmpty();
            // previous implementation skipped the synthetic bean entire if it wasn't unremovable
            if (mongoClient.isDefault() && !unremovable) {
                continue;
            }
            // but for named clients, it would always register them with the unremovable flag
            syntheticBeanBuildItemBuildProducer
                    .produce(createBlockingSyntheticBean(recorder, unremovable, mongoClient.getName()));
            syntheticBeanBuildItemBuildProducer
                    .produce(createReactiveSyntheticBean(recorder, unremovable, mongoClient.getName()));
        }

        recorder.performInitialization(vertxBuildItem.getVertx());
    }

    private SyntheticBeanBuildItem createBlockingSyntheticBean(MongoClientRecorder recorder,
            boolean unremovable, String clientName) {

        SyntheticBeanBuildItem.ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem
                .configure(MongoClient.class)
                .scope(ApplicationScoped.class)
                .supplier(recorder.mongoClientSupplier(clientName))
                .checkActive(recorder.checkActive(clientName))
                .startup()
                .setRuntimeInit();

        return applyCommonBeanConfig(unremovable, clientName, configurator);
    }

    private SyntheticBeanBuildItem createReactiveSyntheticBean(MongoClientRecorder recorder, boolean unremovable,
            String clientName) {

        SyntheticBeanBuildItem.ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem
                .configure(ReactiveMongoClient.class)
                .scope(ApplicationScoped.class)
                .supplier(recorder.reactiveMongoClientSupplier(clientName))
                .checkActive(recorder.checkActive(clientName))
                .startup()
                .setRuntimeInit();

        return applyCommonBeanConfig(unremovable, clientName, configurator);
    }

    private SyntheticBeanBuildItem applyCommonBeanConfig(boolean unremovable, String clientName,
            SyntheticBeanBuildItem.ExtendedBeanConfigurator configurator) {
        if (!unremovable) {
            configurator.unremovable();
        }

        if (MongoConfig.isDefaultClient(clientName)) {
            configurator.addQualifier(Default.class);
        } else {
            configurator.addQualifier().annotation(DotNames.NAMED).addValue("value", clientName).done();
            configurator.addQualifier().annotation(MONGO_CLIENT_ANNOTATION).addValue("value", clientName).done();
        }
        return configurator.done();
    }

    /**
     * We only create the bytecode that returns Mongo clients when MongoClientBuildItem is used
     * This is an optimization in order to avoid having to make all mongo client beans unremovable
     * by default.
     * When the build consumes MongoClientBuildItem, then we need to make the all clients unremovable
     * by default, because they are not referenced by CDI injection points
     */
    @BuildStep
    @Record(value = RUNTIME_INIT, optional = true)
    List<MongoClientBuildItem> mongoClients(
            MongoClientRecorder recorder,
            MongoClientsBuildItem mongoClients,
            // make sure all beans have been initialized
            @SuppressWarnings("unused") BeanContainerBuildItem beanContainer) {
        List<MongoClientBuildItem> result = new ArrayList<>(mongoClients.getMongoClients().size());
        for (io.quarkus.mongodb.deployment.spi.MongoClientBuildItem mongoClient : mongoClients.getMongoClients()) {
            String name = mongoClient.getName();
            result.add(new MongoClientBuildItem(recorder.getClient(name), recorder.getReactiveClient(name), name));
        }
        return result;
    }

    /**
     * When {@link MongoClientBuildItem} is actually consumed by the build, then we need to make all the
     * Mongo client beans unremovable, because they can be potentially used by the consumers
     */
    @BuildStep
    @Weak
    MongoUnremovableClientsBuildItem unremovable(@SuppressWarnings("unused") BuildProducer<MongoClientBuildItem> producer) {
        return new MongoUnremovableClientsBuildItem();
    }

    @BuildStep
    HealthBuildItem addHealthCheck(MongoClientBuildTimeConfig buildTimeConfig) {
        return new HealthBuildItem("io.quarkus.mongodb.health.MongoHealthCheck",
                buildTimeConfig.healthEnabled());
    }

    @BuildStep
    void registerServiceBinding(Capabilities capabilities, BuildProducer<ServiceProviderBuildItem> buildProducer) {
        if (capabilities.isPresent(Capability.KUBERNETES_SERVICE_BINDING)) {
            buildProducer.produce(
                    new ServiceProviderBuildItem(SERVICE_BINDING_INTERFACE_NAME,
                            MongoServiceBindingConverter.class.getName()));
        }
    }

    @BuildStep
    void runtimeInitializedClasses(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClasses) {
        runtimeInitializedClasses.produce(new RuntimeInitializedClassBuildItem(ObjectId.class.getName()));
        runtimeInitializedClasses.produce(new RuntimeInitializedClassBuildItem("com.mongodb.internal.dns.DefaultDnsResolver"));
    }

    /**
     * Ensure we have at most one customizer per Mongo client.
     *
     * @param beans the beans
     * @param validation the producer used to report issues
     */
    @BuildStep
    void validateMongoConfigCustomizers(BeanDiscoveryFinishedBuildItem beans,
            BuildProducer<ValidationPhaseBuildItem.ValidationErrorBuildItem> validation) {
        HashMap<String, List<String>> customizers = new HashMap<>();

        for (BeanInfo bean : beans.getBeans()) {
            if (bean.hasType(MONGO_CLIENT_CUSTOMIZER)) {
                var name = bean.getQualifier(MONGO_CLIENT_ANNOTATION);
                if (name.isPresent()) {
                    String clientName = name.get().value().asString();
                    customizers.computeIfAbsent(clientName, k -> new ArrayList<>()).add(bean.getBeanClass().toString());
                } else {
                    customizers.computeIfAbsent(DEFAULT_CLIENT_NAME, k -> new ArrayList<>())
                            .add(bean.getBeanClass().toString());
                }
            }
        }

        for (Map.Entry<String, List<String>> entry : customizers.entrySet()) {
            if (entry.getValue().size() > 1) {
                validation.produce(new ValidationPhaseBuildItem.ValidationErrorBuildItem(
                        new IllegalStateException("Multiple Mongo client customizers found for client " + entry.getKey() + ": "
                                + String.join(", ", entry.getValue()))));
            }
        }
    }

    @BuildStep
    ModuleEnableNativeAccessBuildItem mongoCryptEnableNativeAccess() {
        // mongodb-crypt uses JNI to load libmongocrypt native library for client-side encryption
        return new ModuleEnableNativeAccessBuildItem("com.mongodb.crypt.capi");
    }
}
