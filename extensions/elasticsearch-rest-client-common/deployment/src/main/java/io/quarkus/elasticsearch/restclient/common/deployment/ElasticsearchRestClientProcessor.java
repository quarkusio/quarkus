package io.quarkus.elasticsearch.restclient.common.deployment;

import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

class ElasticsearchRestClientProcessor {

    @BuildStep
    public void build(BuildProducer<ExtensionSslNativeSupportBuildItem> extensionSslNativeSupport) {
        // Indicates that this extension would like the SSL support to be enabled
        extensionSslNativeSupport.produce(new ExtensionSslNativeSupportBuildItem(Feature.ELASTICSEARCH_REST_CLIENT_COMMON));
    }

    @BuildStep
    public ReflectiveClassBuildItem registerForReflection() {
        return new ReflectiveClassBuildItem(true, true,
                "org.apache.logging.log4j.message.ReusableMessageFactory",
                "org.apache.logging.log4j.message.DefaultFlowMessageFactory");
    }
}
