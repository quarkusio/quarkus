package io.quarkus.amazon.lambda.resteasy.deployment;

import java.util.Optional;

import io.quarkus.amazon.lambda.resteasy.runtime.AmazonLambdaResteasyConfig;
import io.quarkus.amazon.lambda.resteasy.runtime.AmazonLambdaResteasyRecorder;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.resteasy.server.common.deployment.ResteasyInjectionReadyBuildItem;
import io.quarkus.resteasy.server.common.deployment.ResteasyServerConfigBuildItem;

public class AmazonLambdaResteasyProcessor {

    AmazonLambdaResteasyConfig config;

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void setup(AmazonLambdaResteasyRecorder recorder, Optional<ResteasyServerConfigBuildItem> resteasyServerConfig,
            ResteasyInjectionReadyBuildItem resteasyInjectionReady) {
        if (resteasyServerConfig.isPresent()) {
            recorder.initHandler(resteasyServerConfig.get().getInitParameters(), config);
        }
    }
}
