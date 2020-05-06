package io.quarkus.amazon.common.deployment;

import java.util.Optional;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.runtime.RuntimeValue;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpClient.Builder;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;

public final class AmazonClientTransportsBuildItem extends MultiBuildItem {
    private final Optional<DotName> syncClassName;
    private final Optional<DotName> asyncClassName;
    private final RuntimeValue<SdkHttpClient.Builder> syncTransport;
    private final RuntimeValue<SdkAsyncHttpClient.Builder> asyncTransport;

    private final String awsClientName;

    public AmazonClientTransportsBuildItem(Optional<DotName> syncClassName, Optional<DotName> asyncClassName,
            RuntimeValue<Builder> syncTransport,
            RuntimeValue<SdkAsyncHttpClient.Builder> asyncTransport,
            String awsClientName) {
        this.syncClassName = syncClassName;
        this.asyncClassName = asyncClassName;
        this.syncTransport = syncTransport;
        this.asyncTransport = asyncTransport;
        this.awsClientName = awsClientName;
    }

    public Optional<DotName> getSyncClassName() {
        return syncClassName;
    }

    public Optional<DotName> getAsyncClassName() {
        return asyncClassName;
    }

    public RuntimeValue<Builder> getSyncTransport() {
        return syncTransport;
    }

    public RuntimeValue<SdkAsyncHttpClient.Builder> getAsyncTransport() {
        return asyncTransport;
    }

    public String getAwsClientName() {
        return awsClientName;
    }
}
