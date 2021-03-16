package io.quarkus.amazon.common.deployment;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.runtime.RuntimeValue;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;

public final class AmazonClientBuilderBuildItem extends MultiBuildItem {
    private final String awsClientName;
    private final RuntimeValue<? extends AwsClientBuilder> syncBuilder;
    private final RuntimeValue<? extends AwsClientBuilder> asyncBuilder;

    public AmazonClientBuilderBuildItem(String awsClientName, RuntimeValue<? extends AwsClientBuilder> syncBuilder,
            RuntimeValue<? extends AwsClientBuilder> asyncBuilder) {
        this.awsClientName = awsClientName;
        this.syncBuilder = syncBuilder;
        this.asyncBuilder = asyncBuilder;
    }

    public String getAwsClientName() {
        return awsClientName;
    }

    public RuntimeValue<? extends AwsClientBuilder> getSyncBuilder() {
        return syncBuilder;
    }

    public RuntimeValue<? extends AwsClientBuilder> getAsyncBuilder() {
        return asyncBuilder;
    }
}
