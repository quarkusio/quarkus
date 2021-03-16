package io.quarkus.amazon.common.deployment;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.runtime.RuntimeValue;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;

public final class AmazonClientAsyncTransportBuildItem extends MultiBuildItem {

    private final String awsClientName;
    private final DotName className;
    private final RuntimeValue<SdkAsyncHttpClient.Builder> clientBuilder;

    public AmazonClientAsyncTransportBuildItem(String awsClientName,
            DotName className,
            RuntimeValue<SdkAsyncHttpClient.Builder> clientBuilder) {
        this.awsClientName = awsClientName;
        this.className = className;
        this.clientBuilder = clientBuilder;
    }

    public String getAwsClientName() {
        return awsClientName;
    }

    public DotName getClassName() {
        return className;
    }

    public RuntimeValue<SdkAsyncHttpClient.Builder> getClientBuilder() {
        return clientBuilder;
    }
}
