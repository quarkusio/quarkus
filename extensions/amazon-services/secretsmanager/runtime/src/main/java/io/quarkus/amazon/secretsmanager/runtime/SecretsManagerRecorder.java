package io.quarkus.amazon.secretsmanager.runtime;

import io.quarkus.amazon.common.runtime.AwsConfig;
import io.quarkus.amazon.common.runtime.NettyHttpClientConfig;
import io.quarkus.amazon.common.runtime.SdkConfig;
import io.quarkus.amazon.common.runtime.SyncHttpClientConfig;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerAsyncClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerAsyncClientBuilder;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClientBuilder;

@Recorder
public class SecretsManagerRecorder {

    public RuntimeValue<SyncHttpClientConfig> getSyncConfig(SecretsManagerConfig config) {
        return new RuntimeValue<>(config.syncClient);
    }

    public RuntimeValue<NettyHttpClientConfig> getAsyncConfig(SecretsManagerConfig config) {
        return new RuntimeValue<>(config.asyncClient);
    }

    public RuntimeValue<AwsConfig> getAwsConfig(SecretsManagerConfig config) {
        return new RuntimeValue<>(config.aws);
    }

    public RuntimeValue<SdkConfig> getSdkConfig(SecretsManagerConfig config) {
        return new RuntimeValue<>(config.sdk);
    }

    public RuntimeValue<AwsClientBuilder> createSyncBuilder(SecretsManagerConfig config,
            RuntimeValue<SdkHttpClient.Builder> transport) {
        SecretsManagerClientBuilder builder = SecretsManagerClient.builder();
        if (transport != null) {
            builder.httpClientBuilder(transport.getValue());
        }
        return new RuntimeValue<>(builder);
    }

    public RuntimeValue<AwsClientBuilder> createAsyncBuilder(SecretsManagerConfig config,
            RuntimeValue<SdkAsyncHttpClient.Builder> transport) {

        SecretsManagerAsyncClientBuilder builder = SecretsManagerAsyncClient.builder();
        if (transport != null) {
            builder.httpClientBuilder(transport.getValue());
        }
        return new RuntimeValue<>(builder);
    }

    public RuntimeValue<SecretsManagerClient> buildClient(RuntimeValue<? extends AwsClientBuilder> builder,
            BeanContainer beanContainer,
            ShutdownContext shutdown) {
        SecretsManagerClientProducer producer = beanContainer.instance(SecretsManagerClientProducer.class);
        producer.setSyncConfiguredBuilder((SecretsManagerClientBuilder) builder.getValue());
        shutdown.addShutdownTask(producer::destroy);
        return new RuntimeValue<>(producer.client());
    }

    public RuntimeValue<SecretsManagerAsyncClient> buildAsyncClient(RuntimeValue<? extends AwsClientBuilder> builder,
            BeanContainer beanContainer,
            ShutdownContext shutdown) {
        SecretsManagerClientProducer producer = beanContainer.instance(SecretsManagerClientProducer.class);
        producer.setAsyncConfiguredBuilder((SecretsManagerAsyncClientBuilder) builder.getValue());
        shutdown.addShutdownTask(producer::destroy);
        return new RuntimeValue<>(producer.asyncClient());
    }
}
