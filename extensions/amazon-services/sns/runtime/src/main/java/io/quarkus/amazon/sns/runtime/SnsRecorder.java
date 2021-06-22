package io.quarkus.amazon.sns.runtime;

import io.quarkus.amazon.common.runtime.AwsConfig;
import io.quarkus.amazon.common.runtime.NettyHttpClientConfig;
import io.quarkus.amazon.common.runtime.SdkConfig;
import io.quarkus.amazon.common.runtime.SyncHttpClientConfig;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.http.SdkHttpClient.Builder;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.SnsAsyncClientBuilder;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.SnsClientBuilder;

@Recorder
public class SnsRecorder {
    public RuntimeValue<SyncHttpClientConfig> getSyncConfig(SnsConfig config) {
        return new RuntimeValue<>(config.syncClient);
    }

    public RuntimeValue<NettyHttpClientConfig> getAsyncConfig(SnsConfig config) {
        return new RuntimeValue<>(config.asyncClient);
    }

    public RuntimeValue<AwsConfig> getAwsConfig(SnsConfig config) {
        return new RuntimeValue<>(config.aws);
    }

    public RuntimeValue<SdkConfig> getSdkConfig(SnsConfig config) {
        return new RuntimeValue<>(config.sdk);
    }

    public RuntimeValue<AwsClientBuilder> createSyncBuilder(SnsConfig config, RuntimeValue<Builder> transport) {
        SnsClientBuilder builder = SnsClient.builder();

        if (transport != null) {
            builder.httpClientBuilder(transport.getValue());
        }
        return new RuntimeValue<>(builder);
    }

    public RuntimeValue<AwsClientBuilder> createAsyncBuilder(SnsConfig config,
            RuntimeValue<SdkAsyncHttpClient.Builder> transport) {

        SnsAsyncClientBuilder builder = SnsAsyncClient.builder();

        if (transport != null) {
            builder.httpClientBuilder(transport.getValue());
        }
        return new RuntimeValue<>(builder);
    }
}
