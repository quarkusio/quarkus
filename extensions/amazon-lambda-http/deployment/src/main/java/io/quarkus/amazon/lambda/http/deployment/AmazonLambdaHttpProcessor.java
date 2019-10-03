package io.quarkus.amazon.lambda.http.deployment;

import io.quarkus.amazon.lambda.deployment.AmazonLambdaBuildItem;
import io.quarkus.amazon.lambda.http.runtime.AwsHttpHandler;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.vertx.http.deployment.RequireVirtualHttpBuildItem;

public class AmazonLambdaHttpProcessor {

    @BuildStep
    RequireVirtualHttpBuildItem virtual(LaunchModeBuildItem launchMode) {
        return launchMode.getLaunchMode().isDevOrTest() ? null : RequireVirtualHttpBuildItem.MARKER;
    }

    @BuildStep
    AmazonLambdaBuildItem install(BuildProducer<AdditionalBeanBuildItem> consumer) {
        consumer.produce(AdditionalBeanBuildItem.unremovableOf(AwsHttpHandler.class));
        return new AmazonLambdaBuildItem(AwsHttpHandler.class.getName(), null);
    }
}
