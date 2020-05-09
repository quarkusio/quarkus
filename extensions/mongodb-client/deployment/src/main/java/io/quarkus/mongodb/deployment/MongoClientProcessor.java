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
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import com.mongodb.client.MongoClient;
import com.mongodb.event.ConnectionPoolListener;

import io.quarkus.arc.Unremovable;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.annotations.Weak;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.SslNativeConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.deployment.util.HashUtil;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.mongodb.metrics.MongoMetricsConnectionPoolListener;
import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.quarkus.mongodb.runtime.AbstractMongoClientProducer;
import io.quarkus.mongodb.runtime.MongoClientConfig;
import io.quarkus.mongodb.runtime.MongoClientName;
import io.quarkus.mongodb.runtime.MongoClientRecorder;
import io.quarkus.mongodb.runtime.MongoClientSupport;
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;

public class MongoClientProcessor {
    private static DotName MONGOCLIENT_ANNOTATION = DotName.createSimple(MongoClientName.class.getName());
    private static final DotName UNREMOVABLE_BEAN = DotName.createSimple(AbstractMongoClientProducer.class.getName());

    @BuildStep
    BeanDefiningAnnotationBuildItem registerConnectionBean() {
        return new BeanDefiningAnnotationBuildItem(MONGOCLIENT_ANNOTATION);
    }

    @BuildStep
    UnremovableBeanBuildItem markBeansAsUnremovable() {
        return new UnremovableBeanBuildItem(new UnremovableBeanBuildItem.BeanTypeExclusion(UNREMOVABLE_BEAN));
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

    /**
     * Create a producer bean managing the lifecycle of the MongoClient.
     * <p>
     * The generated class will look like
     * 
     * <pre>
     * public class myclass extends AbstractMongoClientProducer {
     *     &#64;Singleton
     *     &#64;Produces
     *     &#64;Default
     *     public MongoClient createDefaultMongoClient() {
     *         MongoClientConfig cfg = getDefaultMongoClientConfig();
     *         return createMongoClient(cfg);
     *     }
     * 
     *     &#64;Singleton
     *     &#64;Produces
     *     &#64;Default
     *     public ReactiveMongoClient createDefaultReactiveMongoClient() {
     *         MongoClientConfig cfg = getDefaultMongoClientConfig();
     *         return createReactiveMongoClient(cfg);
     *     }
     * 
     *     // for each named mongoclient configuration
     *     // example:
     *     // quarkus.mongodb.cluster1.connection-string = mongodb://mongo1:27017,mongo2:27017
     *     &#64;Singleton
     *     &#64;Produces
     *     &#64;Named("cluster1")
     *     &#64;MongoClientName("cluster1")
     *     public MongoClient createNamedMongoClient_123456() {
     *         MongoClientConfig cfg = getMongoClientConfig("cluster1");
     *         return createMongoClient(cfg);
     *     }
     * 
     *     &#64;Singleton
     *     &#64;Produces
     *     &#64;Named("cluster1")
     *     &#64;MongoClientName("cluster1")
     *     public ReactiveMongoClient createNamedReactiveMongoClient_123456() {
     *         MongoClientConfig cfg = getMongoClientConfig("cluster1");
     *         return createReactiveMongoClient(cfg);
     *     }
     * }
     * </pre>
     */
    private void createMongoClientProducerBean(List<MongoClientNameBuildItem> mongoClientNames,
            BuildProducer<GeneratedBeanBuildItem> generatedBean,
            String mongoClientProducerClassName, boolean makeUnremovable) {

        ClassOutput classOutput = new GeneratedBeanGizmoAdaptor(generatedBean);

        try (ClassCreator classCreator = ClassCreator.builder().classOutput(classOutput)
                .className(mongoClientProducerClassName)
                .superClass(AbstractMongoClientProducer.class)
                .build()) {
            classCreator.addAnnotation(Singleton.class);

            try (MethodCreator defaultMongoClientMethodCreator = classCreator.getMethodCreator("createDefaultMongoClient",
                    MongoClient.class)) {
                defaultMongoClientMethodCreator.addAnnotation(Singleton.class);
                defaultMongoClientMethodCreator.addAnnotation(Produces.class);
                defaultMongoClientMethodCreator.addAnnotation(Default.class);
                if (makeUnremovable) {
                    defaultMongoClientMethodCreator.addAnnotation(Unremovable.class);
                }

                ResultHandle mongoClientConfig = defaultMongoClientMethodCreator.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(AbstractMongoClientProducer.class, "getDefaultMongoClientConfig",
                                MongoClientConfig.class),
                        defaultMongoClientMethodCreator.getThis());
                ResultHandle defaultMongoClientNameRH = defaultMongoClientMethodCreator
                        .load(MongoClientRecorder.DEFAULT_MONGOCLIENT_NAME);

                defaultMongoClientMethodCreator.returnValue(
                        defaultMongoClientMethodCreator.invokeVirtualMethod(
                                MethodDescriptor.ofMethod(AbstractMongoClientProducer.class, "createMongoClient",
                                        MongoClient.class,
                                        MongoClientConfig.class, String.class),
                                defaultMongoClientMethodCreator.getThis(),
                                mongoClientConfig, defaultMongoClientNameRH));
            }

            // Default Mutiny reactive client
            try (MethodCreator defaultReactiveMongoClientMethodCreator = classCreator.getMethodCreator(
                    "createDefaultReactiveMongoClient",
                    ReactiveMongoClient.class)) {
                defaultReactiveMongoClientMethodCreator.addAnnotation(Singleton.class);
                defaultReactiveMongoClientMethodCreator.addAnnotation(Produces.class);
                defaultReactiveMongoClientMethodCreator.addAnnotation(Default.class);
                if (makeUnremovable) {
                    defaultReactiveMongoClientMethodCreator.addAnnotation(Unremovable.class);
                }

                ResultHandle mongoReactiveClientConfig = defaultReactiveMongoClientMethodCreator.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(AbstractMongoClientProducer.class, "getDefaultMongoClientConfig",
                                MongoClientConfig.class),
                        defaultReactiveMongoClientMethodCreator.getThis());

                ResultHandle defaultReactiveMongoClientNameRH = defaultReactiveMongoClientMethodCreator
                        .load(MongoClientRecorder.DEFAULT_MONGOCLIENT_NAME);
                defaultReactiveMongoClientMethodCreator.returnValue(
                        defaultReactiveMongoClientMethodCreator.invokeVirtualMethod(
                                MethodDescriptor.ofMethod(AbstractMongoClientProducer.class, "createReactiveMongoClient",
                                        ReactiveMongoClient.class,
                                        MongoClientConfig.class, String.class),
                                defaultReactiveMongoClientMethodCreator.getThis(),
                                mongoReactiveClientConfig, defaultReactiveMongoClientNameRH));
            }

            for (MongoClientNameBuildItem bi : mongoClientNames) {
                String namedMongoClientName = bi.getName();
                try (MethodCreator namedMongoClientMethodCreator = classCreator.getMethodCreator(
                        "createNamedMongoClient_" + HashUtil.sha1(namedMongoClientName),
                        MongoClient.class)) {
                    namedMongoClientMethodCreator.addAnnotation(Singleton.class);
                    namedMongoClientMethodCreator.addAnnotation(Produces.class);
                    namedMongoClientMethodCreator.addAnnotation(AnnotationInstance.create(DotNames.NAMED, null,
                            new AnnotationValue[] { AnnotationValue.createStringValue("value", namedMongoClientName) }));
                    namedMongoClientMethodCreator
                            .addAnnotation(AnnotationInstance.create(MONGOCLIENT_ANNOTATION, null,
                                    new AnnotationValue[] {
                                            AnnotationValue.createStringValue("value", namedMongoClientName) }));
                    if (makeUnremovable) {
                        namedMongoClientMethodCreator.addAnnotation(Unremovable.class);
                    }

                    ResultHandle namedMongoClientNameRH = namedMongoClientMethodCreator.load(namedMongoClientName);

                    ResultHandle namedMongoClientConfig = namedMongoClientMethodCreator.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(AbstractMongoClientProducer.class, "getMongoClientConfig",
                                    MongoClientConfig.class, String.class),
                            namedMongoClientMethodCreator.getThis(), namedMongoClientNameRH);

                    namedMongoClientMethodCreator.returnValue(
                            namedMongoClientMethodCreator.invokeVirtualMethod(
                                    MethodDescriptor.ofMethod(AbstractMongoClientProducer.class, "createMongoClient",
                                            MongoClient.class,
                                            MongoClientConfig.class, String.class),
                                    namedMongoClientMethodCreator.getThis(),
                                    namedMongoClientConfig, namedMongoClientNameRH));
                }

                // Mutiny reactive clients
                try (MethodCreator namedReactiveMongoClientMethodCreator = classCreator.getMethodCreator(
                        "createNamedReactiveMongoClient_" + HashUtil.sha1(namedMongoClientName),
                        ReactiveMongoClient.class)) {
                    namedReactiveMongoClientMethodCreator.addAnnotation(Singleton.class);
                    namedReactiveMongoClientMethodCreator.addAnnotation(Produces.class);
                    namedReactiveMongoClientMethodCreator.addAnnotation(AnnotationInstance.create(DotNames.NAMED, null,
                            new AnnotationValue[] {
                                    AnnotationValue.createStringValue("value",
                                            namedMongoClientName + MongoClientRecorder.REACTIVE_CLIENT_NAME_SUFFIX) }));
                    namedReactiveMongoClientMethodCreator
                            .addAnnotation(AnnotationInstance.create(MONGOCLIENT_ANNOTATION, null,
                                    new AnnotationValue[] {
                                            AnnotationValue.createStringValue("value", namedMongoClientName) }));
                    if (makeUnremovable) {
                        namedReactiveMongoClientMethodCreator.addAnnotation(Unremovable.class);
                    }

                    ResultHandle namedReactiveMongoClientNameRH = namedReactiveMongoClientMethodCreator
                            .load(namedMongoClientName);

                    ResultHandle namedReactiveMongoClientConfig = namedReactiveMongoClientMethodCreator.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(AbstractMongoClientProducer.class, "getMongoClientConfig",
                                    MongoClientConfig.class, String.class),
                            namedReactiveMongoClientMethodCreator.getThis(), namedReactiveMongoClientNameRH);

                    namedReactiveMongoClientMethodCreator.returnValue(
                            namedReactiveMongoClientMethodCreator.invokeVirtualMethod(
                                    MethodDescriptor.ofMethod(AbstractMongoClientProducer.class, "createReactiveMongoClient",
                                            ReactiveMongoClient.class,
                                            MongoClientConfig.class, String.class),
                                    namedReactiveMongoClientMethodCreator.getThis(),
                                    namedReactiveMongoClientConfig, namedReactiveMongoClientNameRH));
                }
            }
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
            RecorderContext recorderContext,
            MongoClientRecorder recorder,
            Optional<MongoUnremovableClientsBuildItem> mongoUnremovableClientsBuildItem,
            SslNativeConfigBuildItem sslNativeConfig,
            CodecProviderBuildItem codecProvider,
            BsonDiscriminatorBuildItem bsonDiscriminator,
            List<MongoConnectionPoolListenerBuildItem> connectionPoolListenerProvider,
            BuildProducer<GeneratedBeanBuildItem> generatedBean,
            BuildProducer<MongoConnectionNameBuildItem> mongoConnections,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItemBuildProducer) {

        List<ConnectionPoolListener> poolListenerList = connectionPoolListenerProvider.stream()
                .map(MongoConnectionPoolListenerBuildItem::getConnectionPoolListener)
                .collect(Collectors.toList());

        // create MongoClientSupport as a synthetic bean as it's used in AbstractMongoClientProducer
        syntheticBeanBuildItemBuildProducer.produce(SyntheticBeanBuildItem.configure(MongoClientSupport.class)
                .scope(Singleton.class)
                .supplier(recorder.mongoClientSupportSupplier(codecProvider.getCodecProviderClassNames(),
                        bsonDiscriminator.getBsonDiscriminatorClassNames(),
                        poolListenerList, sslNativeConfig.isExplicitlyDisabled()))
                .done());

        String mongoClientProducerClassName = getMongoClientProducerClassName();
        createMongoClientProducerBean(mongoClientNames, generatedBean, mongoClientProducerClassName,
                mongoUnremovableClientsBuildItem.isPresent());

        mongoConnections.produce(new MongoConnectionNameBuildItem(MongoClientRecorder.DEFAULT_MONGOCLIENT_NAME));
        for (MongoClientNameBuildItem bi : mongoClientNames) {
            mongoConnections.produce(new MongoConnectionNameBuildItem(bi.getName()));
        }
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
    List<MongoClientBuildItem> mongoClients(MongoClientRecorder recorder, List<MongoConnectionNameBuildItem> mongoConnections) {
        List<MongoClientBuildItem> result = new ArrayList<>(mongoConnections.size());
        for (MongoConnectionNameBuildItem mongoConnection : mongoConnections) {
            String name = mongoConnection.getName();
            result.add(new MongoClientBuildItem(recorder.getClient(name), recorder.getReactiveClient(name), name));
        }
        return result;
    }

    /**
     * When MongoClientBuildItem is actually consumed by the build, then we need to make all the mongo beans unremovable
     * because they can be potentially used by the consumers
     */
    @BuildStep
    @Weak
    MongoUnremovableClientsBuildItem unremovable(@SuppressWarnings("unused") BuildProducer<MongoClientBuildItem> producer) {
        return new MongoUnremovableClientsBuildItem();
    }

    private String getMongoClientProducerClassName() {
        return AbstractMongoClientProducer.class.getPackage().getName() + "."
                + "MongoClientProducer";
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
