package io.quarkus.amazon.common.deployment;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import javax.enterprise.context.ApplicationScoped;

import org.jboss.jandex.DotName;
import org.jboss.jandex.ParameterizedType;
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
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.processor.BuildExtension;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.arc.processor.InjectionPointInfo;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.runtime.RuntimeValue;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
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
            Type injectedType = getInjectedType(injectionPoint);

            if (syncClientName().equals(injectedType.name())) {
                syncClassName = Optional.of(syncClientName());
            }
            if (asyncClientName().equals(injectedType.name())) {
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

    protected void createClientBuilders(
            AmazonClientRecorder recorder,
            RuntimeValue<AwsConfig> awsConfigRuntime,
            RuntimeValue<SdkConfig> sdkConfigRuntime,
            SdkBuildTimeConfig sdkBuildConfig,
            List<AmazonClientSyncTransportBuildItem> amazonClientSyncTransports,
            List<AmazonClientAsyncTransportBuildItem> amazonClientAsyncTransports,
            Class<?> syncClientBuilderClass,
            Function<RuntimeValue<SdkHttpClient.Builder>, RuntimeValue<AwsClientBuilder>> syncClientBuilderFunction,
            Class<?> asyncClientBuilderClass,
            Function<RuntimeValue<SdkAsyncHttpClient.Builder>, RuntimeValue<AwsClientBuilder>> asyncClientBuilderFunction,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans) {
        String configName = configName();

        Optional<RuntimeValue<SdkHttpClient.Builder>> syncSdkHttpClientBuilder = amazonClientSyncTransports.stream()
                .filter(c -> configName.equals(c.getAwsClientName()))
                .map(c -> c.getClientBuilder())
                .findFirst();
        Optional<RuntimeValue<SdkAsyncHttpClient.Builder>> asyncSdkAsyncHttpClientBuilder = amazonClientAsyncTransports.stream()
                .filter(c -> configName.equals(c.getAwsClientName()))
                .map(c -> c.getClientBuilder())
                .findFirst();

        if (!syncSdkHttpClientBuilder.isPresent() && !asyncSdkAsyncHttpClientBuilder.isPresent()) {
            return;
        }

        RuntimeValue<AwsClientBuilder> syncClientBuilder = syncSdkHttpClientBuilder.isPresent()
                ? syncClientBuilderFunction.apply(syncSdkHttpClientBuilder.get())
                : null;
        RuntimeValue<AwsClientBuilder> asyncClientBuilder = asyncSdkAsyncHttpClientBuilder.isPresent()
                ? asyncClientBuilderFunction.apply(asyncSdkAsyncHttpClientBuilder.get())
                : null;

        if (syncClientBuilder != null) {
            syncClientBuilder = recorder.configure(syncClientBuilder, awsConfigRuntime, sdkConfigRuntime,
                    sdkBuildConfig, configName());
            syntheticBeans.produce(SyntheticBeanBuildItem.configure(syncClientBuilderClass)
                    .setRuntimeInit()
                    .scope(ApplicationScoped.class)
                    .runtimeValue(syncClientBuilder)
                    .done());
        }
        if (asyncClientBuilder != null) {
            asyncClientBuilder = recorder.configure(asyncClientBuilder, awsConfigRuntime, sdkConfigRuntime,
                    sdkBuildConfig, configName());
            syntheticBeans.produce(SyntheticBeanBuildItem.configure(asyncClientBuilderClass)
                    .setRuntimeInit()
                    .scope(ApplicationScoped.class)
                    .runtimeValue(asyncClientBuilder)
                    .done());
        }
    }

    private Type getInjectedType(InjectionPointInfo injectionPoint) {
        Type requiredType = injectionPoint.getRequiredType();
        Type injectedType = requiredType;

        if (DotNames.INSTANCE.equals(requiredType.name()) && requiredType instanceof ParameterizedType) {
            injectedType = requiredType.asParameterizedType().arguments().get(0);
        }

        return injectedType;
    }
}
