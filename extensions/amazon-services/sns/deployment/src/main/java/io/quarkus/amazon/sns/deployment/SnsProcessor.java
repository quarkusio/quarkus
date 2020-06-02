package io.quarkus.amazon.sns.deployment;

import java.util.List;

import org.jboss.jandex.DotName;

import io.quarkus.amazon.common.deployment.AbstractAmazonServiceProcessor;
import io.quarkus.amazon.common.deployment.AmazonClientBuildItem;
import io.quarkus.amazon.common.deployment.AmazonClientBuilderBuildItem;
import io.quarkus.amazon.common.deployment.AmazonClientBuilderConfiguredBuildItem;
import io.quarkus.amazon.common.deployment.AmazonClientInterceptorsPathBuildItem;
import io.quarkus.amazon.common.deployment.AmazonClientTransportsBuildItem;
import io.quarkus.amazon.common.runtime.AmazonClientRecorder;
import io.quarkus.amazon.common.runtime.AmazonClientTransportRecorder;
import io.quarkus.amazon.sns.runtime.SnsBuildTimeConfig;
import io.quarkus.amazon.sns.runtime.SnsClientProducer;
import io.quarkus.amazon.sns.runtime.SnsConfig;
import io.quarkus.amazon.sns.runtime.SnsRecorder;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.BeanRegistrationPhaseBuildItem;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.SnsClient;

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

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void setupTransport(List<AmazonClientBuildItem> amazonClients, SnsRecorder recorder,
            AmazonClientTransportRecorder transportRecorder,
            SnsConfig runtimeConfig, BuildProducer<AmazonClientTransportsBuildItem> clientTransportBuildProducer) {

        createTransportBuilders(amazonClients,
                transportRecorder,
                buildTimeConfig.syncClient,
                recorder.getSyncConfig(runtimeConfig),
                recorder.getAsyncConfig(runtimeConfig),
                clientTransportBuildProducer);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void createClientBuilders(List<AmazonClientTransportsBuildItem> transportBuildItems, SnsRecorder recorder,
            SnsConfig runtimeConfig, BuildProducer<AmazonClientBuilderBuildItem> builderProducer) {

        createClientBuilders(transportBuildItems, builderProducer,
                (syncTransport) -> recorder.createSyncBuilder(runtimeConfig, syncTransport),
                (asyncTransport) -> recorder.createAsyncBuilder(runtimeConfig, asyncTransport));
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void configureClient(List<AmazonClientBuilderBuildItem> clients, SnsRecorder recorder,
            AmazonClientRecorder commonRecorder,
            SnsConfig runtimeConfig,
            BuildProducer<AmazonClientBuilderConfiguredBuildItem> producer) {

        initClientBuilders(clients, commonRecorder, recorder.getAwsConfig(runtimeConfig), recorder.getSdkConfig(runtimeConfig),
                buildTimeConfig.sdk, producer);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void buildClients(List<AmazonClientBuilderConfiguredBuildItem> configuredClients, SnsRecorder recorder,
            BeanContainerBuildItem beanContainer,
            ShutdownContextBuildItem shutdown) {

        buildClients(configuredClients,
                (syncBuilder) -> recorder.buildClient(syncBuilder, beanContainer.getValue(), shutdown),
                (asyncBuilder) -> recorder.buildAsyncClient(asyncBuilder, beanContainer.getValue(), shutdown));
    }
}
