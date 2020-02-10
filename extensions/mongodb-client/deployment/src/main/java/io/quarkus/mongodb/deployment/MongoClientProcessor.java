package io.quarkus.mongodb.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;
import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;

import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import com.mongodb.client.MongoClient;

import io.quarkus.arc.Unremovable;
import io.quarkus.arc.deployment.BeanContainerListenerBuildItem;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
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
import io.quarkus.mongodb.ReactiveMongoClient;
import io.quarkus.mongodb.runtime.AbstractMongoClientProducer;
import io.quarkus.mongodb.runtime.MongoClientConfig;
import io.quarkus.mongodb.runtime.MongoClientName;
import io.quarkus.mongodb.runtime.MongoClientRecorder;
import io.quarkus.mongodb.runtime.MongodbConfig;
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

    @Record(RUNTIME_INIT)
    @BuildStep
    void configureRuntimeProperties(MongoClientRecorder recorder,
            CodecProviderBuildItem codecProvider,
            BsonDiscriminatorBuildItem bsonDiscriminator,
            MongodbConfig config) {
        recorder.configureRuntimeProperties(codecProvider.getCodecProviderClassNames(),
                bsonDiscriminator.getBsonDisciminatorClassNames(), config);
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
        reflectiveClassNames.addAll(bsonDiscriminators.getBsonDisciminatorClassNames());

        return reflectiveClassNames.stream()
                .map(s -> new ReflectiveClassBuildItem(true, true, false, s))
                .collect(Collectors.toList());
    }

    /**
     * Create a producer bean managing the lifecycle of the MongoClient.
     * <p>
     * The generated class will look like
     * 
     * <pre>
     * public class myclass extends AbstractMongoClientProducer {
     *     &#64;ApplicationScoped
     *     &#64;Produces
     *     &#64;Default
     *     public MongoClient createDefaultMongoClient() {
     *         MongoClientConfig cfg = getDefaultMongoClientConfig();
     *         return createMongoClient(cfg);
     *     }
     * 
     *     &#64;ApplicationScoped
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
     *     &#64;ApplicationScoped
     *     &#64;Produces
     *     &#64;Named("cluster1")
     *     &#64;MongoClientName("cluster1")
     *     public MongoClient createNamedMongoClient_123456() {
     *         MongoClientConfig cfg = getMongoClientConfig("cluster1");
     *         return createMongoClient(cfg);
     *     }
     * 
     *     &#64;ApplicationScoped
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
    private void createMongoClientProducerBean(ApplicationArchivesBuildItem applicationArchivesBuildItem,
            BuildProducer<GeneratedBeanBuildItem> generatedBean,
            String mongoClientProducerClassName) {

        Set<String> mongoClientNames = new HashSet<>();
        IndexView indexView = applicationArchivesBuildItem.getRootArchive().getIndex();
        Collection<AnnotationInstance> mongoClientAnnotations = indexView.getAnnotations(MONGOCLIENT_ANNOTATION);
        for (AnnotationInstance annotation : mongoClientAnnotations) {
            String mongoClientName = annotation.value().asString();
            mongoClientNames.add(mongoClientName);
        }
        ClassOutput classOutput = new GeneratedBeanGizmoAdaptor(generatedBean);

        try (ClassCreator classCreator = ClassCreator.builder().classOutput(classOutput)
                .className(mongoClientProducerClassName)
                .superClass(AbstractMongoClientProducer.class)
                .build()) {
            classCreator.addAnnotation(ApplicationScoped.class);
            classCreator.addAnnotation(Unremovable.class);

            try (MethodCreator defaultMongoClientMethodCreator = classCreator.getMethodCreator("createDefaultMongoClient",
                    MongoClient.class)) {
                defaultMongoClientMethodCreator.addAnnotation(ApplicationScoped.class);
                defaultMongoClientMethodCreator.addAnnotation(Produces.class);
                defaultMongoClientMethodCreator.addAnnotation(Default.class);

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
            try (MethodCreator defaultReactiveMongoClientMethodCreator = classCreator.getMethodCreator(
                    "createDefaultReactiveMongoClient",
                    ReactiveMongoClient.class)) {
                defaultReactiveMongoClientMethodCreator.addAnnotation(ApplicationScoped.class);
                defaultReactiveMongoClientMethodCreator.addAnnotation(Produces.class);
                defaultReactiveMongoClientMethodCreator.addAnnotation(Default.class);

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
            for (String namedMongoClientName : mongoClientNames) {

                try (MethodCreator namedMongoClientMethodCreator = classCreator.getMethodCreator(
                        "createNamedMongoClient_" + HashUtil.sha1(namedMongoClientName),
                        MongoClient.class)) {
                    namedMongoClientMethodCreator.addAnnotation(ApplicationScoped.class);
                    namedMongoClientMethodCreator.addAnnotation(Produces.class);
                    namedMongoClientMethodCreator.addAnnotation(AnnotationInstance.create(DotNames.NAMED, null,
                            new AnnotationValue[] { AnnotationValue.createStringValue("value", namedMongoClientName) }));
                    namedMongoClientMethodCreator
                            .addAnnotation(AnnotationInstance.create(MONGOCLIENT_ANNOTATION, null,
                                    new AnnotationValue[] {
                                            AnnotationValue.createStringValue("value", namedMongoClientName) }));

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
                try (MethodCreator namedReactiveMongoClientMethodCreator = classCreator.getMethodCreator(
                        "createNamedReactiveMongoClient_" + HashUtil.sha1(namedMongoClientName),
                        ReactiveMongoClient.class)) {
                    namedReactiveMongoClientMethodCreator.addAnnotation(ApplicationScoped.class);
                    namedReactiveMongoClientMethodCreator.addAnnotation(Produces.class);
                    namedReactiveMongoClientMethodCreator.addAnnotation(AnnotationInstance.create(DotNames.NAMED, null,
                            new AnnotationValue[] {
                                    AnnotationValue.createStringValue("value", namedMongoClientName + "reactive") }));
                    namedReactiveMongoClientMethodCreator
                            .addAnnotation(AnnotationInstance.create(MONGOCLIENT_ANNOTATION, null,
                                    new AnnotationValue[] {
                                            AnnotationValue.createStringValue("value", namedMongoClientName) }));

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

    @SuppressWarnings("unchecked")
    @Record(STATIC_INIT)
    @BuildStep
    BeanContainerListenerBuildItem build(
            ApplicationArchivesBuildItem applicationArchivesBuildItem,
            RecorderContext recorderContext,
            MongoClientRecorder recorder,
            BuildProducer<FeatureBuildItem> feature,
            SslNativeConfigBuildItem sslNativeConfig, BuildProducer<ExtensionSslNativeSupportBuildItem> sslNativeSupport,
            BuildProducer<GeneratedBeanBuildItem> generatedBean) throws Exception {

        feature.produce(new FeatureBuildItem(FeatureBuildItem.MONGODB_CLIENT));
        sslNativeSupport.produce(new ExtensionSslNativeSupportBuildItem(FeatureBuildItem.MONGODB_CLIENT));

        String mongoClientProducerClassName = getMongoClientProducerClassName();
        createMongoClientProducerBean(applicationArchivesBuildItem, generatedBean, mongoClientProducerClassName);

        return new BeanContainerListenerBuildItem(recorder.addMongoClient(
                (Class<? extends AbstractMongoClientProducer>) recorderContext.classProxy(mongoClientProducerClassName),
                sslNativeConfig.isExplicitlyDisabled()));
    }

    @Record(RUNTIME_INIT)
    @BuildStep
    void build(MongoClientRecorder recorder, BuildProducer<MongoClientBuildItem> mongoClients, MongodbConfig config) {
        if (config.mongoClientConfigs != null && !config.mongoClientConfigs.isEmpty()) {
            for (Map.Entry<String, MongoClientConfig> namedDataSourceEntry : config.mongoClientConfigs.entrySet()) {
                String name = namedDataSourceEntry.getKey();
                mongoClients
                        .produce(new MongoClientBuildItem(recorder.getClient(name), recorder.getReactiveClient(name), name));
            }
        }
        if (config.defaultMongoClientConfig != null
                && (config.defaultMongoClientConfig.connectionString.isPresent()
                        || !config.defaultMongoClientConfig.hosts.isEmpty())) {
            mongoClients.produce(new MongoClientBuildItem(recorder.getClient(MongoClientRecorder.DEFAULT_MONGOCLIENT_NAME),
                    recorder.getReactiveClient(MongoClientRecorder.DEFAULT_MONGOCLIENT_NAME),
                    MongoClientRecorder.DEFAULT_MONGOCLIENT_NAME));
        }
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
}
