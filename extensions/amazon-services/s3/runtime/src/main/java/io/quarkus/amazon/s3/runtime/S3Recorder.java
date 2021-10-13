package io.quarkus.amazon.s3.runtime;

import io.quarkus.amazon.common.runtime.AwsConfig;
import io.quarkus.amazon.common.runtime.NettyHttpClientConfig;
import io.quarkus.amazon.common.runtime.SdkConfig;
import io.quarkus.amazon.common.runtime.SyncHttpClientConfig;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.http.SdkHttpClient.Builder;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;
import software.amazon.awssdk.services.s3.S3BaseClientBuilder;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;

@Recorder
public class S3Recorder {

    final S3Config config;

    public S3Recorder(S3Config config) {
        this.config = config;
    }

    public RuntimeValue<SyncHttpClientConfig> getSyncConfig() {
        return new RuntimeValue<>(config.syncClient);
    }

    public RuntimeValue<NettyHttpClientConfig> getAsyncConfig() {
        return new RuntimeValue<>(config.asyncClient);
    }

    public RuntimeValue<AwsConfig> getAwsConfig() {
        return new RuntimeValue<>(config.aws);
    }

    public RuntimeValue<SdkConfig> getSdkConfig() {
        return new RuntimeValue<>(config.sdk);
    }

    public RuntimeValue<AwsClientBuilder> createSyncBuilder(RuntimeValue<Builder> transport) {
        S3ClientBuilder builder = S3Client.builder();
        configureS3Client(builder);

        if (transport != null) {
            builder.httpClientBuilder(transport.getValue());
        }
        return new RuntimeValue<>(builder);
    }

    public RuntimeValue<AwsClientBuilder> createAsyncBuilder(RuntimeValue<SdkAsyncHttpClient.Builder> transport) {

        S3AsyncClientBuilder builder = S3AsyncClient.builder();
        configureS3Client(builder);

        if (transport != null) {
            builder.httpClientBuilder(transport.getValue());
        }
        return new RuntimeValue<>(builder);
    }

    private void configureS3Client(S3BaseClientBuilder builder) {
        S3Configuration.Builder s3ConfigBuilder = S3Configuration.builder()
                .accelerateModeEnabled(config.accelerateMode)
                .checksumValidationEnabled(config.checksumValidation)
                .chunkedEncodingEnabled(config.chunkedEncoding)
                .dualstackEnabled(config.dualstack)
                .pathStyleAccessEnabled(config.pathStyleAccess)
                .useArnRegionEnabled(config.useArnRegionEnabled);

        if (config.profileName.isPresent()) {
            s3ConfigBuilder.profileName(config.profileName.get());
        }
        builder.serviceConfiguration(s3ConfigBuilder.build());
    }
}
