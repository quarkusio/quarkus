package io.quarkus.mongodb.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;
import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.inject.Default;
import javax.inject.Singleton;

import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import com.mongodb.client.MongoClient;
import com.mongodb.event.ConnectionPoolListener;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.annotations.Weak;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.SslNativeConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.mongodb.metrics.MongoMetricsConnectionPoolListener;
import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.quarkus.mongodb.runtime.MongoClientBeanUtil;
import io.quarkus.mongodb.runtime.MongoClientName;
import io.quarkus.mongodb.runtime.MongoClientRecorder;
import io.quarkus.mongodb.runtime.MongoClientSupport;
import io.quarkus.mongodb.runtime.MongoClients;
import io.quarkus.mongodb.runtime.MongodbConfig;
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;

public class MongoClientProcessor {
    private static final DotName MONGOCLIENT_ANNOTATION = DotName.createSimple(MongoClientName.class.getName());

    @BuildStep
    BeanDefiningAnnotationBuildItem registerConnectionBean() {
        return new BeanDefiningAnnotationBuildItem(MONGOCLIENT_ANNOTATION);
    }

    @BuildStep
    CodecProviderBuildItem collectCodecProviders(CombinedIndexBuildItem indexBuildItem) {
        Collection<ClassInfo> codecProviderClasses = indexBuildItem.getIndex()
                .getAllKnownImplementors(DotName.createSimple(CodecProvider.class.getName()));
        List<String> names = codecProviderClasses.stream().map(ci -> ci.name().toString()).collect(Collectors.toList());
        return new CodecProviderBuildItem(names);
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
    List<ReflectiveClassBuildItem> addCodecsAndDiscriminatorsToNative(CodecProviderBuildItem codecProviders,
            BsonDiscriminatorBuildItem bsonDiscriminators) {
        List<String> reflectiveClassNames = new ArrayList<>();
        reflectiveClassNames.addAll(codecProviders.getCodecProviderClassNames());
        reflectiveClassNames.addAll(bsonDiscriminators.getBsonDiscriminatorClassNames());

        return reflectiveClassNames.stream()
                .map(s -> new ReflectiveClassBuildItem(true, true, false, s))
                .collect(Collectors.toList());
    }

    @BuildStep
    public void mongoClientNames(ApplicationArchivesBuildItem applicationArchivesBuildItem,
            BuildProducer<MongoClientNameBuildItem> mongoClientName) {
        Set<String> values = new HashSet<>();
        IndexView indexView = applicationArchivesBuildItem.getRootArchive().getIndex();
        Collection<AnnotationInstance> mongoClientAnnotations = indexView.getAnnotations(MONGOCLIENT_ANNOTATION);
        for (AnnotationInstance annotation : mongoClientAnnotations) {
            values.add(annotation.value().asString());
        }
        for (String value : values) {
            mongoClientName.produce(new MongoClientNameBuildItem(value));
        }
    }

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FeatureBuildItem.MONGODB_CLIENT);
    }

    @BuildStep
    ExtensionSslNativeSupportBuildItem ssl() {
        return new ExtensionSslNativeSupportBuildItem(FeatureBuildItem.MONGODB_CLIENT);
    }

    @Record(STATIC_INIT)
    @BuildStep
    void build(
            List<MongoClientNameBuildItem> mongoClientNames,
            MongoClientRecorder recorder,
            SslNativeConfigBuildItem sslNativeConfig,
            CodecProviderBuildItem codecProvider,
            BsonDiscriminatorBuildItem bsonDiscriminator,
            List<MongoConnectionPoolListenerBuildItem> connectionPoolListenerProvider,
            BuildProducer<MongoConnectionNameBuildItem> mongoConnections,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItemBuildProducer,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans) {

        List<ConnectionPoolListener> poolListenerList = connectionPoolListenerProvider.stream()
                .map(MongoConnectionPoolListenerBuildItem::getConnectionPoolListener)
                .collect(Collectors.toList());

        // make MongoClients an unremoveable bean
        additionalBeans.produce(AdditionalBeanBuildItem.builder().addBeanClasses(MongoClients.class).setUnremovable().build());

        // create MongoClientSupport as a synthetic bean as it's used in AbstractMongoClientProducer
        syntheticBeanBuildItemBuildProducer.produce(SyntheticBeanBuildItem.configure(MongoClientSupport.class)
                .scope(Singleton.class)
                .supplier(recorder.mongoClientSupportSupplier(codecProvider.getCodecProviderClassNames(),
                        bsonDiscriminator.getBsonDiscriminatorClassNames(),
                        poolListenerList, sslNativeConfig.isExplicitlyDisabled()))
                .done());

        mongoConnections.produce(new MongoConnectionNameBuildItem(MongoClientBeanUtil.DEFAULT_MONGOCLIENT_NAME));
        for (MongoClientNameBuildItem bi : mongoClientNames) {
            mongoConnections.produce(new MongoConnectionNameBuildItem(bi.getName()));
        }
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    void generateClientBeans(MongoClientRecorder recorder,
            List<MongoClientNameBuildItem> mongoClientNames,
            MongodbConfig mongodbConfig,
            Optional<MongoUnremovableClientsBuildItem> mongoUnremovableClientsBuildItem,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItemBuildProducer) {

        boolean makeUnremovable = mongoUnremovableClientsBuildItem.isPresent();

        // default blocking client
        syntheticBeanBuildItemBuildProducer.produce(createBlockingSyntheticBean(recorder, mongodbConfig, makeUnremovable,
                MongoClientBeanUtil.DEFAULT_MONGOCLIENT_NAME));
        // default reactive client
        syntheticBeanBuildItemBuildProducer.produce(createReactiveSyntheticBean(recorder, mongodbConfig, makeUnremovable,
                MongoClientBeanUtil.DEFAULT_MONGOCLIENT_NAME));

        for (MongoClientNameBuildItem mongoClientName : mongoClientNames) {
            // named blocking client
            syntheticBeanBuildItemBuildProducer
                    .produce(createBlockingSyntheticBean(recorder, mongodbConfig, makeUnremovable, mongoClientName.getName()));
            // named reactive client
            syntheticBeanBuildItemBuildProducer
                    .produce(createReactiveSyntheticBean(recorder, mongodbConfig, makeUnremovable, mongoClientName.getName()));
        }
    }

    private SyntheticBeanBuildItem createBlockingSyntheticBean(MongoClientRecorder recorder, MongodbConfig mongodbConfig,
            boolean makeUnremovable, String clientName) {

        SyntheticBeanBuildItem.ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem
                .configure(MongoClient.class)
                .scope(Singleton.class)
                // pass the runtime config into the recorder to ensure that the DataSource related beans
                // are created after runtime configuration has been setup
                .supplier(recorder.mongoClientSupplier(clientName, mongodbConfig))
                .setRuntimeInit();

        return applyCommonBeanConfig(makeUnremovable, clientName, configurator);
    }

    private SyntheticBeanBuildItem createReactiveSyntheticBean(MongoClientRecorder recorder, MongodbConfig mongodbConfig,
            boolean makeUnremovable, String clientName) {

        SyntheticBeanBuildItem.ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem
                .configure(ReactiveMongoClient.class)
                .scope(Singleton.class)
                // pass the runtime config into the recorder to ensure that the DataSource related beans
                // are created after runtime configuration has been setup
                .supplier(recorder.reactiveMongoClientSupplier(clientName, mongodbConfig))
                .setRuntimeInit();

        return applyCommonBeanConfig(makeUnremovable, clientName, configurator);
    }

    private SyntheticBeanBuildItem applyCommonBeanConfig(boolean makeUnremovable, String clientName,
            SyntheticBeanBuildItem.ExtendedBeanConfigurator configurator) {
        if (makeUnremovable) {
            configurator.unremovable();
        }

        if (MongoClientBeanUtil.isDefault(clientName)) {
            configurator.addQualifier(Default.class);
        } else {
            String namedQualifier = MongoClientBeanUtil.namedQualifier(clientName, false);
            configurator.addQualifier().annotation(DotNames.NAMED).addValue("value", namedQualifier).done();
            configurator.addQualifier().annotation(MONGOCLIENT_ANNOTATION).addValue("value", clientName).done();
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
                buildTimeConfig.healthEnabled, "mongodb");
    }

    @BuildStep
    void setupMetrics(
            MongoClientBuildTimeConfig buildTimeConfig, Capabilities capabilities,
            BuildProducer<MongoConnectionPoolListenerBuildItem> producer) {

        if (buildTimeConfig.metricsEnabled && capabilities.isCapabilityPresent(Capabilities.METRICS)) {
            producer.produce(new MongoConnectionPoolListenerBuildItem(new MongoMetricsConnectionPoolListener()));
        }
    }
}
