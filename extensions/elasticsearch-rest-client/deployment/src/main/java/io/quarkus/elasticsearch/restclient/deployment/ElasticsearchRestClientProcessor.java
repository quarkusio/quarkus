package io.quarkus.elasticsearch.restclient.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;

class ElasticsearchRestClientProcessor {

    @BuildStep
    public void build(BuildProducer<ExtensionSslNativeSupportBuildItem> extensionSslNativeSupport) {
        // Indicates that this extension would like the SSL support to be enabled
        extensionSslNativeSupport.produce(new ExtensionSslNativeSupportBuildItem(FeatureBuildItem.ELASTICSEARCH_REST_CLIENT));
    }
}
