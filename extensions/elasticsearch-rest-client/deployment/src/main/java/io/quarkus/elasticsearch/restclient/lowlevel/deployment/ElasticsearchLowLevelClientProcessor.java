package io.quarkus.elasticsearch.restclient.lowlevel.deployment;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Singleton;

import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.sniff.Sniffer;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;

import io.quarkus.arc.ActiveResult;
import io.quarkus.arc.BeanDestroyer;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.arc.deployment.BeanRegistrationPhaseBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.elasticsearch.restclient.common.deployment.DevservicesElasticsearchBuildItem;
import io.quarkus.elasticsearch.restclient.common.deployment.ElasticsearchClientProcessorUtil;
import io.quarkus.elasticsearch.restclient.common.runtime.ElasticsearchClientBeanUtil;
import io.quarkus.elasticsearch.restclient.lowlevel.ElasticsearchClientConfig;
import io.quarkus.elasticsearch.restclient.lowlevel.runtime.ElasticsearchLowLevelClientRecorder;
import io.quarkus.elasticsearch.restclient.lowlevel.runtime.health.ElasticsearchHealthCheckCondition;
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;
import io.smallrye.common.annotation.Identifier;

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

    @BuildStep
    void elasticsearchClientConfigSupport(BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<BeanDefiningAnnotationBuildItem> beanDefiningAnnotations) {
        // add the @ElasticsearchClientConfig class otherwise it won't be registered as a qualifier
        additionalBeans.produce(AdditionalBeanBuildItem.builder().addBeanClass(ElasticsearchClientConfig.class).build());

        beanDefiningAnnotations
                .produce(new BeanDefiningAnnotationBuildItem(ELASTICSEARCH_CLIENT_CONFIG_ANNOTATION,
                        DotNames.APPLICATION_SCOPED, false));
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    void generateElasticsearchBeans(
            ElasticsearchLowLevelClientRecorder recorder,
            ElasticsearchBuildTimeConfig config,
            List<ElasticsearchLowLevelClientReferenceBuildItem> elasticsearchLowLevelClientReferenceBuildItems,
            BuildProducer<SyntheticBeanBuildItem> producer,
            Capabilities capabilities) {
        boolean healthChecksPossible = capabilities.isPresent(Capability.SMALLRYE_HEALTH);

        for (ElasticsearchLowLevelClientReferenceBuildItem buildItem : elasticsearchLowLevelClientReferenceBuildItems) {
            String clientName = buildItem.getName();
            produceRestClientBean(clientName, recorder, producer);
            produceRestClientSnifferBean(clientName, recorder, producer);
            ElasticsearchLowLevelClientBuildTimeConfig clientConfig = config.clients().get(clientName);
            if (healthChecksPossible && (clientConfig == null || clientConfig.healthEnabled())) {
                produceHealthCheckBean(clientName, recorder, producer);
            }
        }
    }

    @BuildStep
    public void devServices(
            List<ElasticsearchLowLevelClientReferenceBuildItem> clients,
            BuildProducer<DevservicesElasticsearchBuildItem> producer) {
        for (ElasticsearchLowLevelClientReferenceBuildItem client : clients) {
            producer.produce(new DevservicesElasticsearchBuildItem(client.getName(), hostConfigProperty(client.getName())));
        }
    }

    private String hostConfigProperty(String clientName) {
        if (ElasticsearchClientBeanUtil.isDefault(clientName)) {
            return "quarkus.elasticsearch.hosts";
        } else {
            return "quarkus.elasticsearch.\"%s\".hosts".formatted(clientName);
        }
    }

    @BuildStep
    HealthBuildItem addHealthCheck(ElasticsearchBuildTimeConfig buildTimeConfig, Capabilities capabilities) {
        boolean healthChecksPossible = capabilities.isPresent(Capability.SMALLRYE_HEALTH);
        boolean atLeastOneHealthCheckEnabled = false;
        for (ElasticsearchLowLevelClientBuildTimeConfig config : buildTimeConfig.clients().values()) {
            if (config.healthEnabled()) {
                atLeastOneHealthCheckEnabled = true;
            }
        }
        if (healthChecksPossible) {
            return new HealthBuildItem("io.quarkus.elasticsearch.restclient.lowlevel.runtime.health.ElasticsearchHealthCheck",
                    atLeastOneHealthCheckEnabled);
        }
        return null;
    }

    private void produceRestClientBean(String clientName, ElasticsearchLowLevelClientRecorder recorder,
            BuildProducer<SyntheticBeanBuildItem> producer) {
        producer.produce(createSyntheticBean(
                clientName,
                RestClient.class,
                Singleton.class,
                ElasticsearchClientBeanUtil.isDefault(clientName),
                recorder.checkActiveRestClientSupplier(clientName))
                .createWith(recorder.restClientSupplier(clientName))
                .destroyer(BeanDestroyer.AutoCloseableDestroyer.class)
                .done());
    }

    private void produceRestClientSnifferBean(String clientName, ElasticsearchLowLevelClientRecorder recorder,
            BuildProducer<SyntheticBeanBuildItem> producer) {
        producer.produce(createSyntheticBean(
                clientName,
                Sniffer.class,
                Singleton.class,
                ElasticsearchClientBeanUtil.isDefault(clientName),
                recorder.checkActiveSnifferSupplier(clientName))
                .createWith(recorder.restClientSnifferSupplier(clientName))
                .addInjectionPoint(ClassType.create(DotName.createSimple(RestClient.class)), qualifier(clientName))
                .destroyer(BeanDestroyer.AutoCloseableDestroyer.class)
                .done());
    }

    private void produceHealthCheckBean(String clientName, ElasticsearchLowLevelClientRecorder recorder,
            BuildProducer<SyntheticBeanBuildItem> producer) {
        producer.produce(createSyntheticBean(
                clientName,
                ElasticsearchHealthCheckCondition.class,
                ApplicationScoped.class,
                ElasticsearchClientBeanUtil.isDefault(clientName),
                recorder.checkActiveHealthCheckSupplier(clientName))
                .createWith(recorder.restClientHealthCheckConditionSupplier(clientName))
                .addInjectionPoint(ClassType.create(DotName.createSimple(RestClient.class)), qualifier(clientName))
                // .addQualifier(DotName.createSimple("org.eclipse.microprofile.health.Readiness"))
                .done());
    }

    private static <T> SyntheticBeanBuildItem.ExtendedBeanConfigurator createSyntheticBean(String clientName,
            Class<T> type,
            Class<? extends Annotation> scope,
            boolean defaultBean,
            Supplier<ActiveResult> checkActiveSupplier) {
        SyntheticBeanBuildItem.ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem
                .configure(type)
                .scope(scope)
                .unremovable()
                .setRuntimeInit()
                .checkActive(checkActiveSupplier)
                .startup();

        if (defaultBean) {
            configurator.defaultBean();
            configurator.addQualifier(Default.class);
        }

        configurator.addQualifier().annotation(DotNames.IDENTIFIER).addValue("value", clientName).done();

        return configurator;
    }

    public static AnnotationInstance qualifier(String clientName) {
        if (clientName == null || ElasticsearchClientBeanUtil.isDefault(clientName)) {
            return AnnotationInstance.builder(Default.class).build();
        } else {
            return AnnotationInstance.builder(Identifier.class).value(clientName).build();
        }
    }

}
