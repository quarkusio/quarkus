package io.quarkus.elasticsearch.restclient.lowlevel.deployment;

import io.quarkus.arc.deployment.BeanRegistrationPhaseBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.processor.BuildExtension;
import io.quarkus.arc.processor.InjectionPointInfo;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.elasticsearch.restclient.common.deployment.ElasticsearchClientAnnotationTypeBuildItem;
import io.quarkus.elasticsearch.restclient.common.deployment.ElasticsearchClientProcessorUtil;
import io.quarkus.elasticsearch.restclient.common.deployment.ElasticsearchClientTypeBuildItem;
import org.elasticsearch.client.RestClient;
import org.jboss.jandex.DotName;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.elasticsearch.restclient.common.deployment.DevservicesElasticsearchBuildItem;
import io.quarkus.elasticsearch.restclient.lowlevel.ElasticsearchClientConfig;
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import java.util.List;
import java.util.Set;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;

class ElasticsearchLowLevelClientProcessor {

    private static final DotName REST_CLIENT = DotName.createSimple(RestClient.class.getName());
    private static final DotName ELASTICSEARCH_CLIENT_CONFIG_ANNOTATION = DotName
            .createSimple(ElasticsearchClientConfig.class.getName());

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.ELASTICSEARCH_REST_CLIENT);
    }

    @BuildStep
    public void collectLowLevelClientReferences(CombinedIndexBuildItem indexBuildItem,
            BeanRegistrationPhaseBuildItem registrationPhase,
            BuildProducer<ElasticsearchLowLevelClientReferenceBuildItem> references) {
        for (String name : ElasticsearchClientProcessorUtil.collectReferencedClientNames(indexBuildItem, registrationPhase,
                Set.of(REST_CLIENT),
                Set.of(ELASTICSEARCH_CLIENT_CONFIG_ANNOTATION))) {
            references.produce(new ElasticsearchLowLevelClientReferenceBuildItem(name));
        }
    }

    @Record(RUNTIME_INIT)
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
                // are created after runtime configuration has been set up
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
                // are created after runtime configuration has been set up
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


    @BuildStep
    void elasticsearchClientConfigSupport(BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<BeanDefiningAnnotationBuildItem> beanDefiningAnnotations) {
        // add the @ElasticsearchClientConfig class otherwise it won't be registered as a qualifier
        additionalBeans.produce(AdditionalBeanBuildItem.builder().addBeanClass(ElasticsearchClientConfig.class).build());

        beanDefiningAnnotations
                .produce(new BeanDefiningAnnotationBuildItem(ELASTICSEARCH_CLIENT_CONFIG_ANNOTATION,
                        DotNames.APPLICATION_SCOPED, false));
    }

    @BuildStep
    HealthBuildItem addHealthCheck(ElasticsearchBuildTimeConfig buildTimeConfig) {
        return new HealthBuildItem("io.quarkus.elasticsearch.restclient.lowlevel.runtime.health.ElasticsearchHealthCheck",
                buildTimeConfig.healthEnabled());
    }

    @BuildStep
    DevservicesElasticsearchBuildItem devServices() {
        return new DevservicesElasticsearchBuildItem("quarkus.elasticsearch.hosts");
    }

}
