package io.quarkus.elasticsearch.javaclient.deployment;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import jakarta.enterprise.inject.Default;
import jakarta.inject.Singleton;

import org.elasticsearch.client.RestClient;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;

import com.fasterxml.jackson.databind.ObjectMapper;

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.transport.ElasticsearchTransport;
import io.quarkus.arc.ActiveResult;
import io.quarkus.arc.BeanDestroyer;
import io.quarkus.arc.deployment.BeanRegistrationPhaseBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.NativeImageFeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.elasticsearch.javaclient.runtime.ElasticsearchJavaClientRecorder;
import io.quarkus.elasticsearch.restclient.common.deployment.ElasticsearchClientProcessorUtil;
import io.quarkus.elasticsearch.restclient.common.runtime.ElasticsearchClientBeanUtil;
import io.quarkus.elasticsearch.restclient.lowlevel.deployment.ConfiguredElasticsearchLowLevelClientBuildItem;
import io.quarkus.elasticsearch.restclient.lowlevel.deployment.ElasticsearchBuildTimeConfig;
import io.quarkus.elasticsearch.restclient.lowlevel.deployment.ElasticsearchLowLevelClientReferenceBuildItem;
import io.smallrye.common.annotation.Identifier;

class ElasticsearchJavaClientProcessor {

    private static final DotName ELASTICSEARCH_CLIENT = DotName.createSimple(ElasticsearchClient.class.getName());

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.ELASTICSEARCH_JAVA_CLIENT);
    }

    @BuildStep
    ServiceProviderBuildItem serviceProvider() {
        return new ServiceProviderBuildItem("jakarta.json.spi.JsonProvider",
                "co.elastic.clients.json.jackson.JacksonJsonProvider");
    }

    @BuildStep
    ReflectiveClassBuildItem jsonProvider() {
        return ReflectiveClassBuildItem.builder("org.eclipse.parsson.JsonProviderImpl").build();
    }

    @BuildStep
    NativeImageFeatureBuildItem enableElasticsearchJavaClientFeature() {
        return new NativeImageFeatureBuildItem(
                "io.quarkus.elasticsearch.javaclient.runtime.graalvm.ElasticsearchJavaClientFeature");
    }

    @BuildStep
    public void collectJavaClientReferences(
            CombinedIndexBuildItem indexBuildItem,
            BeanRegistrationPhaseBuildItem registrationPhase,
            List<ElasticsearchLowLevelClientReferenceBuildItem> elasticsearchLowLevelClientReferenceBuildItems,
            BuildProducer<ElasticsearchJavaClientReferenceBuildItem> references) {
        Set<String> clientNames = new HashSet<>();
        for (String name : ElasticsearchClientProcessorUtil.collectReferencedClientNames(indexBuildItem, registrationPhase,
                Set.of(ELASTICSEARCH_CLIENT),
                Set.of())) {
            references.produce(new ElasticsearchJavaClientReferenceBuildItem(name));
            clientNames.add(name);
        }
        // Because we may have not discovered all the clients with ^ we just create for any lower level client we have:
        for (ElasticsearchLowLevelClientReferenceBuildItem item : elasticsearchLowLevelClientReferenceBuildItems) {
            if (clientNames.add(item.getName())) {
                references.produce(new ElasticsearchJavaClientReferenceBuildItem(item.getName()));
            }
        }
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    void generateElasticsearchClientBeans(
            ElasticsearchJavaClientRecorder recorder,
            ConfiguredElasticsearchLowLevelClientBuildItem lowLevelClientBuildItem,
            ElasticsearchBuildTimeConfig config,
            List<ElasticsearchJavaClientReferenceBuildItem> elasticsearchLowLevelClientReferenceBuildItems,
            BuildProducer<SyntheticBeanBuildItem> producer) {
        for (ElasticsearchJavaClientReferenceBuildItem buildItem : elasticsearchLowLevelClientReferenceBuildItems) {
            String clientName = buildItem.getName();
            if (!lowLevelClientBuildItem.getNames().contains(clientName)) {
                throw new IllegalStateException("Unable to locate the low-level client [" + clientName
                        + "]. Impossible to create the transport for higher level Elasticsearch clients. " +
                        "Make sure that the low-level client is correctly configured.");
            }
            produceTransportBean(clientName, recorder, producer);
            produceBlockingClientBean(clientName, recorder, producer);
            produceAsyncClientBean(clientName, recorder, producer);
        }
    }

    private void produceTransportBean(String clientName, ElasticsearchJavaClientRecorder recorder,
            BuildProducer<SyntheticBeanBuildItem> producer) {
        producer.produce(createSyntheticBean(
                clientName,
                ElasticsearchTransport.class,
                Singleton.class,
                ElasticsearchClientBeanUtil.isDefault(clientName),
                recorder.checkActiveElasticsearchTransportSupplier(clientName))
                .addInjectionPoint(ClassType.create(DotName.createSimple(RestClient.class)), qualifier(clientName))
                .addInjectionPoint(ClassType.create(DotName.createSimple(ObjectMapper.class)))
                .createWith(recorder.elasticsearchTransportSupplier(clientName))
                .destroyer(BeanDestroyer.AutoCloseableDestroyer.class)
                .done());
    }

    private void produceBlockingClientBean(String clientName, ElasticsearchJavaClientRecorder recorder,
            BuildProducer<SyntheticBeanBuildItem> producer) {
        producer.produce(createSyntheticBean(
                clientName,
                ElasticsearchClient.class,
                Singleton.class,
                ElasticsearchClientBeanUtil.isDefault(clientName),
                recorder.checkActiveElasticsearchTransportSupplier(clientName))
                .createWith(recorder.blockingClientSupplier(clientName))
                .addInjectionPoint(ClassType.create(DotName.createSimple(ElasticsearchTransport.class)), qualifier(clientName))
                .destroyer(BeanDestroyer.AutoCloseableDestroyer.class)
                .done());
    }

    private void produceAsyncClientBean(String clientName, ElasticsearchJavaClientRecorder recorder,
            BuildProducer<SyntheticBeanBuildItem> producer) {
        producer.produce(createSyntheticBean(
                clientName,
                ElasticsearchAsyncClient.class,
                Singleton.class,
                ElasticsearchClientBeanUtil.isDefault(clientName),
                recorder.checkActiveElasticsearchTransportSupplier(clientName))
                .createWith(recorder.asyncClientSupplier(clientName))
                .addInjectionPoint(ClassType.create(DotName.createSimple(ElasticsearchTransport.class)), qualifier(clientName))
                .destroyer(BeanDestroyer.AutoCloseableDestroyer.class)
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

    private static AnnotationInstance qualifier(String clientName) {
        if (clientName == null || ElasticsearchClientBeanUtil.isDefault(clientName)) {
            return AnnotationInstance.builder(Default.class).build();
        } else {
            return AnnotationInstance.builder(Identifier.class).value(clientName).build();
        }
    }

}
