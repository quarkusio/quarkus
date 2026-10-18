package io.quarkus.elasticsearch.restclient.common.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;

class ApacheHttpClientProcessor {

    @BuildStep
    void runtimeInitializedClasses(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClasses) {
        // Apache HTTP Client 5:
        runtimeInitializedClasses.produce(new RuntimeInitializedClassBuildItem(
                "org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder"));
        runtimeInitializedClasses
                .produce(new RuntimeInitializedClassBuildItem("org.apache.hc.client5.http.ssl.ConscryptClientTlsStrategy"));
    }
}
