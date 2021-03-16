package io.quarkus.amazon.common.deployment;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.runtime.RuntimeValue;
import software.amazon.awssdk.http.SdkHttpClient;

public final class AmazonClientSyncTransportBuildItem extends MultiBuildItem {

    private final String awsClientName;
    private final DotName className;
    private final RuntimeValue<SdkHttpClient.Builder> clientBuilder;

    public AmazonClientSyncTransportBuildItem(String awsClientName,
            DotName className,
            RuntimeValue<SdkHttpClient.Builder> clientBuilder) {
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

    public RuntimeValue<SdkHttpClient.Builder> getClientBuilder() {
        return clientBuilder;
    }
}
