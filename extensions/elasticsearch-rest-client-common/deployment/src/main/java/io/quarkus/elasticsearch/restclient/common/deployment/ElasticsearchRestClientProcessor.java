package io.quarkus.elasticsearch.restclient.common.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;

class ElasticsearchRestClientProcessor {

    @BuildStep
    public void build(BuildProducer<ExtensionSslNativeSupportBuildItem> extensionSslNativeSupport) {
        // Indicates that this extension would like the SSL support to be enabled
        extensionSslNativeSupport.produce(new ExtensionSslNativeSupportBuildItem(Feature.ELASTICSEARCH_REST_CLIENT_COMMON));
    }

    @BuildStep
    void registerAnnotations(BuildProducer<AdditionalBeanBuildItem> additionalBeans,
                                          BuildProducer<BeanDefiningAnnotationBuildItem> beanDefiningAnnotations) {
        // add the @ElasticsearchClientName class otherwise it won't be registered as a qualifier
        additionalBeans.produce(AdditionalBeanBuildItem.builder()
                .addBeanClass(ElasticsearchClientProcessorUtil.ELASTICSEARCH_CLIENT_NAME_ANNOTATION.toString())
                .build());
    }
}
