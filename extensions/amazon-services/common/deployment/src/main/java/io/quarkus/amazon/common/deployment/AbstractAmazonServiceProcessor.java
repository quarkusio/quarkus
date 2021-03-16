package io.quarkus.amazon.common.deployment;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;

import io.quarkus.amazon.common.runtime.AmazonClientApacheTransportRecorder;
import io.quarkus.amazon.common.runtime.AmazonClientNettyTransportRecorder;
import io.quarkus.amazon.common.runtime.AmazonClientRecorder;
import io.quarkus.amazon.common.runtime.AmazonClientUrlConnectionTransportRecorder;
import io.quarkus.amazon.common.runtime.AwsConfig;
import io.quarkus.amazon.common.runtime.NettyHttpClientConfig;
import io.quarkus.amazon.common.runtime.SdkBuildTimeConfig;
import io.quarkus.amazon.common.runtime.SdkConfig;
import io.quarkus.amazon.common.runtime.SyncHttpClientBuildTimeConfig;
import io.quarkus.amazon.common.runtime.SyncHttpClientConfig;
import io.quarkus.arc.deployment.BeanRegistrationPhaseBuildItem;
import io.quarkus.arc.processor.BuildExtension;
import io.quarkus.arc.processor.InjectionPointInfo;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.runtime.RuntimeValue;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;

abstract public class AbstractAmazonServiceProcessor {

    abstract protected Feature amazonServiceClientName();

    abstract protected String configName();

    abstract protected DotName syncClientName();

    abstract protected DotName asyncClientName();

    abstract protected String builtinInterceptorsPath();

    protected void setupExtension(BeanRegistrationPhaseBuildItem beanRegistrationPhase,
            BuildProducer<ExtensionSslNativeSupportBuildItem> extensionSslNativeSupport,
            BuildProducer<FeatureBuildItem> feature,
            BuildProducer<AmazonClientInterceptorsPathBuildItem> interceptors,
            BuildProducer<AmazonClientBuildItem> clientProducer,
            SdkBuildTimeConfig buildTimeSdkConfig,
            SyncHttpClientBuildTimeConfig buildTimeSyncConfig) {

        feature.produce(new FeatureBuildItem(amazonServiceClientName()));
        extensionSslNativeSupport.produce(new ExtensionSslNativeSupportBuildItem(amazonServiceClientName()));
        interceptors.produce(new AmazonClientInterceptorsPathBuildItem(builtinInterceptorsPath()));

        Optional<DotName> syncClassName = Optional.empty();
        Optional<DotName> asyncClassName = Optional.empty();

        //Discover all clients injections in order to determine if async or sync client is required
        for (InjectionPointInfo injectionPoint : beanRegistrationPhase.getContext().get(BuildExtension.Key.INJECTION_POINTS)) {
            Type requiredType = injectionPoint.getRequiredType();

            if (syncClientName().equals(requiredType.name())) {
                syncClassName = Optional.of(syncClientName());
            }
            if (asyncClientName().equals(requiredType.name())) {
                asyncClassName = Optional.of(asyncClientName());
            }
        }
        if (syncClassName.isPresent() || asyncClassName.isPresent()) {
            clientProducer.produce(new AmazonClientBuildItem(syncClassName, asyncClassName, configName(),
                    buildTimeSdkConfig, buildTimeSyncConfig));
        }
    }

    protected void createApacheSyncTransportBuilder(List<AmazonClientBuildItem> amazonClients,
            AmazonClientApacheTransportRecorder recorder,
            SyncHttpClientBuildTimeConfig buildSyncConfig,
            RuntimeValue<SyncHttpClientConfig> syncConfig,
            BuildProducer<AmazonClientSyncTransportBuildItem> clientSyncTransports) {

        Optional<AmazonClientBuildItem> matchingClientBuildItem = amazonClients.stream()
                .filter(c -> c.getAwsClientName().equals(configName()))
                .findAny();

        matchingClientBuildItem.ifPresent(client -> {
            if (!client.getSyncClassName().isPresent()) {
                return;
            }
            if (buildSyncConfig.type != SyncHttpClientBuildTimeConfig.SyncClientType.APACHE) {
                return;
            }

            clientSyncTransports.produce(
                    new AmazonClientSyncTransportBuildItem(
                            client.getAwsClientName(),
                            client.getSyncClassName().get(),
                            recorder.configureSync(configName(), syncConfig)));
        });
    }

    protected void createUrlConnectionSyncTransportBuilder(List<AmazonClientBuildItem> amazonClients,
            AmazonClientUrlConnectionTransportRecorder recorder,
            SyncHttpClientBuildTimeConfig buildSyncConfig,
            RuntimeValue<SyncHttpClientConfig> syncConfig,
            BuildProducer<AmazonClientSyncTransportBuildItem> clientSyncTransports) {

        Optional<AmazonClientBuildItem> matchingClientBuildItem = amazonClients.stream()
                .filter(c -> c.getAwsClientName().equals(configName()))
                .findAny();

        matchingClientBuildItem.ifPresent(client -> {
            if (!client.getSyncClassName().isPresent()) {
                return;
            }
            if (buildSyncConfig.type != SyncHttpClientBuildTimeConfig.SyncClientType.URL) {
                return;
            }

            clientSyncTransports.produce(
                    new AmazonClientSyncTransportBuildItem(
                            client.getAwsClientName(),
                            client.getSyncClassName().get(),
                            recorder.configureSync(configName(), syncConfig)));
        });
    }

    protected void createNettyAsyncTransportBuilder(List<AmazonClientBuildItem> amazonClients,
            AmazonClientNettyTransportRecorder recorder,
            RuntimeValue<NettyHttpClientConfig> asyncConfig,
            BuildProducer<AmazonClientAsyncTransportBuildItem> clientAsyncTransports) {

        Optional<AmazonClientBuildItem> matchingClientBuildItem = amazonClients.stream()
                .filter(c -> c.getAwsClientName().equals(configName()))
                .findAny();

        matchingClientBuildItem.ifPresent(client -> {
            if (!client.getAsyncClassName().isPresent()) {
                return;
            }

            clientAsyncTransports.produce(
                    new AmazonClientAsyncTransportBuildItem(
                            client.getAwsClientName(),
                            client.getAsyncClassName().get(),
                            recorder.configureAsync(configName(), asyncConfig)));
        });
    }

    protected void createClientBuilders(List<AmazonClientSyncTransportBuildItem> syncClientBuilders,
            List<AmazonClientAsyncTransportBuildItem> asyncClientBuilders,
            BuildProducer<AmazonClientBuilderBuildItem> builderProducer,
            Function<RuntimeValue<SdkHttpClient.Builder>, RuntimeValue<AwsClientBuilder>> syncFunc,
            Function<RuntimeValue<SdkAsyncHttpClient.Builder>, RuntimeValue<AwsClientBuilder>> asyncFunc) {
        String configName = configName();

        Optional<RuntimeValue<SdkHttpClient.Builder>> syncClientBuilder = syncClientBuilders.stream()
                .filter(c -> configName.equals(c.getAwsClientName()))
                .map(c -> c.getClientBuilder())
                .findFirst();
        Optional<RuntimeValue<SdkAsyncHttpClient.Builder>> asyncClientBuilder = asyncClientBuilders.stream()
                .filter(c -> configName.equals(c.getAwsClientName()))
                .map(c -> c.getClientBuilder())
                .findFirst();

        if (!syncClientBuilder.isPresent() && !asyncClientBuilder.isPresent()) {
            return;
        }

        builderProducer.produce(new AmazonClientBuilderBuildItem(configName,
                syncClientBuilder.isPresent() ? syncFunc.apply(syncClientBuilder.get()) : null,
                asyncClientBuilder.isPresent() ? asyncFunc.apply(asyncClientBuilder.get()) : null));
    }

    protected void buildClients(List<AmazonClientBuilderConfiguredBuildItem> configuredClients,
            Function<RuntimeValue<? extends AwsClientBuilder>, RuntimeValue<? extends SdkClient>> syncClient,
            Function<RuntimeValue<? extends AwsClientBuilder>, RuntimeValue<? extends SdkClient>> asyncClient) {

        for (AmazonClientBuilderConfiguredBuildItem client : configuredClients) {
            if (configName().equals(client.getAwsClientName())) {
                if (client.getSyncBuilder() != null) {
                    syncClient.apply(client.getSyncBuilder());
                }
                if (client.getAsyncBuilder() != null) {
                    asyncClient.apply(client.getAsyncBuilder());
                }
            }
        }
    }

    protected void initClientBuilders(List<AmazonClientBuilderBuildItem> clients, AmazonClientRecorder recorder,
            RuntimeValue<AwsConfig> awsConfigRuntime,
            RuntimeValue<SdkConfig> sdkConfigRuntime, SdkBuildTimeConfig sdkBuildConfig,
            BuildProducer<AmazonClientBuilderConfiguredBuildItem> producer) {
        Optional<AmazonClientBuilderBuildItem> matchingClientBuilderBuildItem = clients.stream()
                .filter(c -> c.getAwsClientName().equals(configName()))
                .findAny();

        matchingClientBuilderBuildItem.ifPresent(client -> {
            RuntimeValue<? extends AwsClientBuilder> syncBuilder = null;
            RuntimeValue<? extends AwsClientBuilder> asyncBuilder = null;

            if (client.getSyncBuilder() != null) {
                syncBuilder = recorder.configure(client.getSyncBuilder(), awsConfigRuntime, sdkConfigRuntime,
                        sdkBuildConfig, configName());
            }
            if (client.getAsyncBuilder() != null) {
                asyncBuilder = recorder.configure(client.getAsyncBuilder(), awsConfigRuntime, sdkConfigRuntime,
                        sdkBuildConfig, configName());
            }
            producer.produce(new AmazonClientBuilderConfiguredBuildItem(configName(), syncBuilder, asyncBuilder));
        });
    }
}
