package io.quarkus.amazon.sns.deployment;

import java.util.List;

import org.jboss.jandex.DotName;

import io.quarkus.amazon.common.deployment.AbstractAmazonServiceProcessor;
import io.quarkus.amazon.common.deployment.AmazonClientAsyncTransportBuildItem;
import io.quarkus.amazon.common.deployment.AmazonClientBuildItem;
import io.quarkus.amazon.common.deployment.AmazonClientInterceptorsPathBuildItem;
import io.quarkus.amazon.common.deployment.AmazonClientSyncTransportBuildItem;
import io.quarkus.amazon.common.deployment.AmazonHttpClients;
import io.quarkus.amazon.common.runtime.AmazonClientApacheTransportRecorder;
import io.quarkus.amazon.common.runtime.AmazonClientNettyTransportRecorder;
import io.quarkus.amazon.common.runtime.AmazonClientRecorder;
import io.quarkus.amazon.common.runtime.AmazonClientUrlConnectionTransportRecorder;
import io.quarkus.amazon.sns.runtime.SnsBuildTimeConfig;
import io.quarkus.amazon.sns.runtime.SnsClientProducer;
import io.quarkus.amazon.sns.runtime.SnsConfig;
import io.quarkus.amazon.sns.runtime.SnsRecorder;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanRegistrationPhaseBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.SnsAsyncClientBuilder;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.SnsClientBuilder;

public class SnsProcessor extends AbstractAmazonServiceProcessor {

    SnsBuildTimeConfig buildTimeConfig;

    @Override
    protected Feature amazonServiceClientName() {
        return Feature.AMAZON_SNS;
    }

    @Override
    protected String configName() {
        return "sns";
    }

    @Override
    protected DotName syncClientName() {
        return DotName.createSimple(SnsClient.class.getName());
    }

    @Override
    protected DotName asyncClientName() {
        return DotName.createSimple(SnsAsyncClient.class.getName());
    }

    @Override
    protected String builtinInterceptorsPath() {
        return "software/amazon/awssdk/services/sns/execution.interceptors";
    }

    @BuildStep
    AdditionalBeanBuildItem producer() {
        return AdditionalBeanBuildItem.unremovableOf(SnsClientProducer.class);
    }

    @BuildStep
    void setup(BeanRegistrationPhaseBuildItem beanRegistrationPhase,
            BuildProducer<ExtensionSslNativeSupportBuildItem> extensionSslNativeSupport,
            BuildProducer<FeatureBuildItem> feature,
            BuildProducer<AmazonClientInterceptorsPathBuildItem> interceptors,
            BuildProducer<AmazonClientBuildItem> clientProducer) {

        setupExtension(beanRegistrationPhase, extensionSslNativeSupport, feature, interceptors, clientProducer,
                buildTimeConfig.sdk,
                buildTimeConfig.syncClient);
    }

    @BuildStep(onlyIf = AmazonHttpClients.IsAmazonApacheHttpServicePresent.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    void setupApacheSyncTransport(List<AmazonClientBuildItem> amazonClients, SnsRecorder recorder,
            AmazonClientApacheTransportRecorder transportRecorder,
            SnsConfig runtimeConfig, BuildProducer<AmazonClientSyncTransportBuildItem> syncTransports) {

        createApacheSyncTransportBuilder(amazonClients,
                transportRecorder,
                buildTimeConfig.syncClient,
                recorder.getSyncConfig(runtimeConfig),
                syncTransports);
    }

    @BuildStep(onlyIf = AmazonHttpClients.IsAmazonUrlConnectionHttpServicePresent.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    void setupUrlConnectionSyncTransport(List<AmazonClientBuildItem> amazonClients, SnsRecorder recorder,
            AmazonClientUrlConnectionTransportRecorder transportRecorder,
            SnsConfig runtimeConfig, BuildProducer<AmazonClientSyncTransportBuildItem> syncTransports) {

        createUrlConnectionSyncTransportBuilder(amazonClients,
                transportRecorder,
                buildTimeConfig.syncClient,
                recorder.getSyncConfig(runtimeConfig),
                syncTransports);
    }

    @BuildStep(onlyIf = AmazonHttpClients.IsAmazonNettyHttpServicePresent.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    void setupNettyAsyncTransport(List<AmazonClientBuildItem> amazonClients, SnsRecorder recorder,
            AmazonClientNettyTransportRecorder transportRecorder,
            SnsConfig runtimeConfig, BuildProducer<AmazonClientAsyncTransportBuildItem> asyncTransports) {

        createNettyAsyncTransportBuilder(amazonClients,
                transportRecorder,
                recorder.getAsyncConfig(runtimeConfig),
                asyncTransports);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void createClientBuilders(SnsRecorder recorder,
            AmazonClientRecorder commonRecorder,
            SnsConfig runtimeConfig,
            List<AmazonClientSyncTransportBuildItem> syncTransports,
            List<AmazonClientAsyncTransportBuildItem> asyncTransports,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans) {

        createClientBuilders(commonRecorder,
                recorder.getAwsConfig(runtimeConfig),
                recorder.getSdkConfig(runtimeConfig),
                buildTimeConfig.sdk,
                syncTransports,
                asyncTransports,
                SnsClientBuilder.class,
                (syncTransport) -> recorder.createSyncBuilder(runtimeConfig, syncTransport),
                SnsAsyncClientBuilder.class,
                (asyncTransport) -> recorder.createAsyncBuilder(runtimeConfig, asyncTransport),
                syntheticBeans);
    }
}
