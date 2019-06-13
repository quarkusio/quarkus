package io.quarkus.elasticsearch.restclient.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;

class ElasticsearchRestClientProcessor {

    @BuildStep
    public void build(BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ExtensionSslNativeSupportBuildItem> extensionSslNativeSupport) throws Exception {
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false,
                org.apache.commons.logging.impl.LogFactoryImpl.class.getName(),
                org.apache.commons.logging.impl.Jdk14Logger.class.getName()));

        // Indicates that this extension would like the SSL support to be enabled
        extensionSslNativeSupport.produce(new ExtensionSslNativeSupportBuildItem(FeatureBuildItem.ELASTICSEARCH_REST_CLIENT));
    }
}
