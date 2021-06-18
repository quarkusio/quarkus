package io.quarkus.amazon.kms.runtime;

import io.quarkus.amazon.common.runtime.AwsConfig;
import io.quarkus.amazon.common.runtime.NettyHttpClientConfig;
import io.quarkus.amazon.common.runtime.SdkConfig;
import io.quarkus.amazon.common.runtime.SyncHttpClientConfig;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.services.kms.KmsAsyncClient;
import software.amazon.awssdk.services.kms.KmsAsyncClientBuilder;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.KmsClientBuilder;

@Recorder
public class KmsRecorder {

    public RuntimeValue<SyncHttpClientConfig> getSyncConfig(KmsConfig config) {
        return new RuntimeValue<>(config.syncClient);
    }

    public RuntimeValue<NettyHttpClientConfig> getAsyncConfig(KmsConfig config) {
        return new RuntimeValue<>(config.asyncClient);
    }

    public RuntimeValue<AwsConfig> getAwsConfig(KmsConfig config) {
        return new RuntimeValue<>(config.aws);
    }

    public RuntimeValue<SdkConfig> getSdkConfig(KmsConfig config) {
        return new RuntimeValue<>(config.sdk);
    }

    public RuntimeValue<AwsClientBuilder> createSyncBuilder(KmsConfig config, RuntimeValue<SdkHttpClient.Builder> transport) {
        KmsClientBuilder builder = KmsClient.builder();
        if (transport != null) {
            builder.httpClientBuilder(transport.getValue());
        }
        return new RuntimeValue<>(builder);
    }

    public RuntimeValue<AwsClientBuilder> createAsyncBuilder(KmsConfig config,
            RuntimeValue<SdkAsyncHttpClient.Builder> transport) {

        KmsAsyncClientBuilder builder = KmsAsyncClient.builder();
        if (transport != null) {
            builder.httpClientBuilder(transport.getValue());
        }
        return new RuntimeValue<>(builder);
    }
}
