package io.quarkus.amazon.sqs.runtime;

import io.quarkus.amazon.common.runtime.AwsConfig;
import io.quarkus.amazon.common.runtime.NettyHttpClientConfig;
import io.quarkus.amazon.common.runtime.SdkConfig;
import io.quarkus.amazon.common.runtime.SyncHttpClientConfig;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.http.SdkHttpClient.Builder;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClientBuilder;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;

@Recorder
public class SqsRecorder {
    public RuntimeValue<SyncHttpClientConfig> getSyncConfig(SqsConfig config) {
        return new RuntimeValue<>(config.syncClient);
    }

    public RuntimeValue<NettyHttpClientConfig> getAsyncConfig(SqsConfig config) {
        return new RuntimeValue<>(config.asyncClient);
    }

    public RuntimeValue<AwsConfig> getAwsConfig(SqsConfig config) {
        return new RuntimeValue<>(config.aws);
    }

    public RuntimeValue<SdkConfig> getSdkConfig(SqsConfig config) {
        return new RuntimeValue<>(config.sdk);
    }

    public RuntimeValue<AwsClientBuilder> createSyncBuilder(SqsConfig config, RuntimeValue<Builder> transport) {
        SqsClientBuilder builder = SqsClient.builder();

        if (transport != null) {
            builder.httpClientBuilder(transport.getValue());
        }
        return new RuntimeValue<>(builder);
    }

    public RuntimeValue<AwsClientBuilder> createAsyncBuilder(SqsConfig config,
            RuntimeValue<SdkAsyncHttpClient.Builder> transport) {

        SqsAsyncClientBuilder builder = SqsAsyncClient.builder();

        if (transport != null) {
            builder.httpClientBuilder(transport.getValue());
        }
        return new RuntimeValue<>(builder);
    }

    public RuntimeValue<SqsClient> buildClient(RuntimeValue<? extends AwsClientBuilder> builder,
            BeanContainer beanContainer,
            ShutdownContext shutdown) {
        SqsClientProducer producer = beanContainer.instance(SqsClientProducer.class);
        producer.setSyncConfiguredBuilder((SqsClientBuilder) builder.getValue());
        shutdown.addShutdownTask(producer::destroy);
        return new RuntimeValue<>(producer.client());
    }

    public RuntimeValue<SqsAsyncClient> buildAsyncClient(RuntimeValue<? extends AwsClientBuilder> builder,
            BeanContainer beanContainer,
            ShutdownContext shutdown) {
        SqsClientProducer producer = beanContainer.instance(SqsClientProducer.class);
        producer.setAsyncConfiguredBuilder((SqsAsyncClientBuilder) builder.getValue());
        shutdown.addShutdownTask(producer::destroy);
        return new RuntimeValue<>(producer.asyncClient());
    }
}
