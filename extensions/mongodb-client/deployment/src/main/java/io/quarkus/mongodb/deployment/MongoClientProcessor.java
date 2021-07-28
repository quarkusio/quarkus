package io.quarkus.mongodb.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;
import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.inject.Singleton;

import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.pojo.PropertyCodecProvider;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import com.mongodb.client.MongoClient;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.event.CommandListener;
import com.mongodb.event.ConnectionPoolListener;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.BeanRegistrationPhaseBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.processor.BuildExtension;
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
import io.quarkus.deployment.builditem.SslNativeConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.metrics.MetricsCapabilityBuildItem;
import io.quarkus.mongodb.MongoClientName;
import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.quarkus.mongodb.runtime.MongoClientBeanUtil;
import io.quarkus.mongodb.runtime.MongoClientRecorder;
import io.quarkus.mongodb.runtime.MongoClientSupport;
import io.quarkus.mongodb.runtime.MongoClients;
import io.quarkus.mongodb.runtime.MongoServiceBindingConverter;
import io.quarkus.mongodb.runtime.MongodbConfig;
import io.quarkus.runtime.metrics.MetricsFactory;
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;

public class MongoClientProcessor {
    private static final String MONGODB_TRACING_COMMANDLISTENER_CLASSNAME = "io.quarkus.mongodb.tracing.MongoTracingCommandListener";
    private static final DotName MONGO_CLIENT_ANNOTATION = DotName.createSimple(MongoClientName.class.getName());

    private static final DotName MONGO_CLIENT = DotName.createSimple(MongoClient.class.getName());
    private static final DotName REACTIVE_MONGO_CLIENT = DotName.createSimple(ReactiveMongoClient.class.getName());

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
    CodecProviderBuildItem collectCodecProviders(CombinedIndexBuildItem indexBuildItem) {
        Collection<ClassInfo> codecProviderClasses = indexBuildItem.getIndex()
                .getAllKnownImplementors(DotName.createSimple(CodecProvider.class.getName()));
        List<String> names = codecProviderClasses.stream().map(ci -> ci.name().toString()).collect(Collectors.toList());
        return new CodecProviderBuildItem(names);
    }

    @BuildStep
    PropertyCodecProviderBuildItem collectPropertyCodecProviders(CombinedIndexBuildItem indexBuildItem) {
        Collection<ClassInfo> propertyCodecProviderClasses = indexBuildItem.getIndex()
                .getAllKnownImplementors(DotName.createSimple(PropertyCodecProvider.class.getName()));
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
    CommandListenerBuildItem collectCommandListeners(CombinedIndexBuildItem indexBuildItem,
            MongoClientBuildTimeConfig buildTimeConfig, Capabilities capabilities) {
        Collection<ClassInfo> commandListenerClasses = indexBuildItem.getIndex()
                .getAllKnownImplementors(DotName.createSimple(CommandListener.class.getName()));
        List<String> names = commandListenerClasses.stream()
                .map(ci -> ci.name().toString())
                .collect(Collectors.toList());
        if (buildTimeConfig.tracingEnabled && capabilities.isPresent(Capability.OPENTRACING)) {
            names.add(MONGODB_TRACING_COMMANDLISTENER_CLASSNAME);
        }
        return new CommandListenerBuildItem(names);
    }

    @BuildStep
    List<ReflectiveClassBuildItem> addExtensionPointsToNative(CodecProviderBuildItem codecProviders,
            PropertyCodecProviderBuildItem propertyCodecProviders, BsonDiscriminatorBuildItem bsonDiscriminators,
            CommandListenerBuildItem commandListeners) {
        List<String> reflectiveClassNames = new ArrayList<>();
        reflectiveClassNames.addAll(codecProviders.getCodecProviderClassNames());
        reflectiveClassNames.addAll(propertyCodecProviders.getPropertyCodecProviderClassNames());
        reflectiveClassNames.addAll(bsonDiscriminators.getBsonDiscriminatorClassNames());
        reflectiveClassNames.addAll(commandListeners.getCommandListenerClassNames());

        List<ReflectiveClassBuildItem> reflectiveClass = reflectiveClassNames.stream()
                .map(s -> new ReflectiveClassBuildItem(true, true, false, s))
                .collect(Collectors.toCollection(() -> new ArrayList<>()));
        // ChangeStreamDocument needs to be registered for reflection with its fields.
        reflectiveClass.add(new ReflectiveClassBuildItem(true, true, true, ChangeStreamDocument.class.getName()));
        return reflectiveClass;
    }

    @BuildStep
    public void mongoClientNames(CombinedIndexBuildItem indexBuildItem,
            BuildProducer<MongoClientNameBuildItem> mongoClientName) {
        Set<String> values = new HashSet<>();
        IndexView indexView = indexBuildItem.getIndex();
        addMongoClientNameValues(MONGO_CLIENT_ANNOTATION, indexView, values);
        for (String value : values) {
            mongoClientName.produce(new MongoClientNameBuildItem(value));
        }
    }

    private void addMongoClientNameValues(DotName annotationName, IndexView indexView, Set<String> values) {
        Collection<AnnotationInstance> mongoClientAnnotations = indexView.getAnnotations(annotationName);
        for (AnnotationInstance annotation : mongoClientAnnotations) {
            values.add(annotation.value().asString());
        }
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
    MongoConnectionPoolListenerBuildItem setupMetrics(
            MongoClientBuildTimeConfig buildTimeConfig,
            MongoClientRecorder recorder,
            Optional<MetricsCapabilityBuildItem> metricsCapability) {

        // Construction of MongoClient isn't compatible with the MetricsFactoryConsumer pattern.
        // Use a supplier to defer construction of the pool listener for the supported metrics system
        if (buildTimeConfig.metricsEnabled && metricsCapability.isPresent()) {
            if (metricsCapability.get().metricsSupported(MetricsFactory.MICROMETER)) {
                return new MongoConnectionPoolListenerBuildItem(recorder.createMicrometerConnectionPoolListener());
            } else {
                return new MongoConnectionPoolListenerBuildItem(recorder.createMPMetricsConnectionPoolListener());
            }
        }
        return null;
    }

    @BuildStep
    void additionalBeans(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        // add the @MongoClientName class otherwise it won't registered as a qualifier
        additionalBeans.produce(AdditionalBeanBuildItem.builder().addBeanClass(MongoClientName.class).build());
        // make MongoClients an unremoveable bean
        additionalBeans.produce(AdditionalBeanBuildItem.builder().addBeanClasses(MongoClients.class).setUnremovable().build());
    }

    @BuildStep
    void connectionNames(
            List<MongoClientNameBuildItem> mongoClientNames,
            BuildProducer<MongoConnectionNameBuildItem> mongoConnections) {
        mongoConnections.produce(new MongoConnectionNameBuildItem(MongoClientBeanUtil.DEFAULT_MONGOCLIENT_NAME));
        for (MongoClientNameBuildItem bi : mongoClientNames) {
            mongoConnections.produce(new MongoConnectionNameBuildItem(bi.getName()));
        }
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
            List<MongoConnectionPoolListenerBuildItem> connectionPoolListenerProvider,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItemBuildProducer) {

        List<Supplier<ConnectionPoolListener>> poolListenerList = new ArrayList<>(connectionPoolListenerProvider.size());
        for (MongoConnectionPoolListenerBuildItem item : connectionPoolListenerProvider) {
            poolListenerList.add(item.getConnectionPoolListener());
        }

        // create MongoClientSupport as a synthetic bean as it's used in AbstractMongoClientProducer
        syntheticBeanBuildItemBuildProducer.produce(SyntheticBeanBuildItem.configure(MongoClientSupport.class)
                .scope(Singleton.class)
                .supplier(recorder.mongoClientSupportSupplier(codecProvider.getCodecProviderClassNames(),
                        propertyCodecProvider.getPropertyCodecProviderClassNames(),
                        bsonDiscriminator.getBsonDiscriminatorClassNames(), commandListener.getCommandListenerClassNames(),
                        poolListenerList, sslNativeConfig.isExplicitlyDisabled()))
                .done());
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    void generateClientBeans(MongoClientRecorder recorder,
            BeanRegistrationPhaseBuildItem registrationPhase,
            List<MongoClientNameBuildItem> mongoClientNames,
            MongoClientBuildTimeConfig mongoClientBuildTimeConfig,
            MongodbConfig mongodbConfig,
            List<MongoUnremovableClientsBuildItem> mongoUnremovableClientsBuildItem,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItemBuildProducer) {

        boolean makeUnremovable = !mongoUnremovableClientsBuildItem.isEmpty();

        boolean createDefaultBlockingMongoClient = false;
        boolean createDefaultReactiveMongoClient = false;
        if (makeUnremovable || mongoClientBuildTimeConfig.forceDefaultClients) {
            // all clients are expected to exist in this case
            createDefaultBlockingMongoClient = true;
            createDefaultReactiveMongoClient = true;
        } else {
            // we only create the default client if they are actually used by injection points
            for (InjectionPointInfo injectionPoint : registrationPhase.getContext().get(BuildExtension.Key.INJECTION_POINTS)) {
                DotName injectionPointType = injectionPoint.getRequiredType().name();
                if (injectionPointType.equals(MONGO_CLIENT) && injectionPoint.hasDefaultedQualifier()) {
                    createDefaultBlockingMongoClient = true;
                } else if (injectionPointType.equals(REACTIVE_MONGO_CLIENT) && injectionPoint.hasDefaultedQualifier()) {
                    createDefaultReactiveMongoClient = true;
                }

                if (createDefaultBlockingMongoClient && createDefaultReactiveMongoClient) {
                    break;
                }
            }
        }

        if (createDefaultBlockingMongoClient) {
            syntheticBeanBuildItemBuildProducer.produce(createBlockingSyntheticBean(recorder, mongodbConfig,
                    makeUnremovable || mongoClientBuildTimeConfig.forceDefaultClients,
                    MongoClientBeanUtil.DEFAULT_MONGOCLIENT_NAME, false));
        }
        if (createDefaultReactiveMongoClient) {
            syntheticBeanBuildItemBuildProducer.produce(createReactiveSyntheticBean(recorder, mongodbConfig,
                    makeUnremovable || mongoClientBuildTimeConfig.forceDefaultClients,
                    MongoClientBeanUtil.DEFAULT_MONGOCLIENT_NAME, false));
        }

        for (MongoClientNameBuildItem mongoClientName : mongoClientNames) {
            // named blocking client
            syntheticBeanBuildItemBuildProducer
                    .produce(createBlockingSyntheticBean(recorder, mongodbConfig, makeUnremovable, mongoClientName.getName(),
                            mongoClientName.isAddQualifier()));
            // named reactive client
            syntheticBeanBuildItemBuildProducer
                    .produce(createReactiveSyntheticBean(recorder, mongodbConfig, makeUnremovable, mongoClientName.getName(),
                            mongoClientName.isAddQualifier()));
        }
    }

    private SyntheticBeanBuildItem createBlockingSyntheticBean(MongoClientRecorder recorder, MongodbConfig mongodbConfig,
            boolean makeUnremovable, String clientName, boolean addMongoClientQualifier) {

        SyntheticBeanBuildItem.ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem
                .configure(MongoClient.class)
                .scope(ApplicationScoped.class)
                // pass the runtime config into the recorder to ensure that the DataSource related beans
                // are created after runtime configuration has been setup
                .supplier(recorder.mongoClientSupplier(clientName, mongodbConfig))
                .setRuntimeInit();

        return applyCommonBeanConfig(makeUnremovable, clientName, addMongoClientQualifier, configurator, false);
    }

    private SyntheticBeanBuildItem createReactiveSyntheticBean(MongoClientRecorder recorder, MongodbConfig mongodbConfig,
            boolean makeUnremovable, String clientName, boolean addMongoClientQualifier) {

        SyntheticBeanBuildItem.ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem
                .configure(ReactiveMongoClient.class)
                .scope(ApplicationScoped.class)
                // pass the runtime config into the recorder to ensure that the DataSource related beans
                // are created after runtime configuration has been setup
                .supplier(recorder.reactiveMongoClientSupplier(clientName, mongodbConfig))
                .setRuntimeInit();

        return applyCommonBeanConfig(makeUnremovable, clientName, addMongoClientQualifier, configurator, true);
    }

    private SyntheticBeanBuildItem applyCommonBeanConfig(boolean makeUnremovable, String clientName,
            boolean addMongoClientQualifier, SyntheticBeanBuildItem.ExtendedBeanConfigurator configurator, boolean isReactive) {
        if (makeUnremovable) {
            configurator.unremovable();
        }

        if (MongoClientBeanUtil.isDefault(clientName)) {
            configurator.addQualifier(Default.class);
        } else {
            String namedQualifier = MongoClientBeanUtil.namedQualifier(clientName, isReactive);
            configurator.addQualifier().annotation(DotNames.NAMED).addValue("value", namedQualifier).done();
            if (addMongoClientQualifier) {
                configurator.addQualifier().annotation(MONGO_CLIENT_ANNOTATION).addValue("value", clientName).done();
            }
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
    List<MongoClientBuildItem> mongoClients(MongoClientRecorder recorder, List<MongoConnectionNameBuildItem> mongoConnections,
            // make sure all beans have been initialized
            @SuppressWarnings("unused") BeanContainerBuildItem beanContainer) {
        List<MongoClientBuildItem> result = new ArrayList<>(mongoConnections.size());
        for (MongoConnectionNameBuildItem mongoConnection : mongoConnections) {
            String name = mongoConnection.getName();
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
                buildTimeConfig.healthEnabled);
    }

    @BuildStep
    void registerServiceBinding(Capabilities capabilities, BuildProducer<ServiceProviderBuildItem> buildProducer) {
        if (capabilities.isPresent(Capability.KUBERNETES_SERVICE_BINDING)) {
            buildProducer.produce(
                    new ServiceProviderBuildItem(SERVICE_BINDING_INTERFACE_NAME,
                            MongoServiceBindingConverter.class.getName()));
        }
    }
}
