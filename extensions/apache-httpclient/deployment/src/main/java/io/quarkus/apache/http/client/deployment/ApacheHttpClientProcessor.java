package io.quarkus.apache.http.client.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;

class ApacheHttpClientProcessor {

    @BuildStep
    void runtimeInitializedClasses(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClasses) {
        runtimeInitializedClasses.produce(new RuntimeInitializedClassBuildItem("org.apache.http.impl.auth.NTLMEngineImpl"));
        runtimeInitializedClasses
                .produce(new RuntimeInitializedClassBuildItem("org.apache.http.conn.ssl.SSLConnectionSocketFactory"));
    }
}
