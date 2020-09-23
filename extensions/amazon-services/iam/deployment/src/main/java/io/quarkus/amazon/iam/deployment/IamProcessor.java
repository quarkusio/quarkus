package io.quarkus.amazon.iam.deployment;

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
import io.quarkus.amazon.iam.runtime.IamBuildTimeConfig;
import io.quarkus.amazon.iam.runtime.IamClientProducer;
import io.quarkus.amazon.iam.runtime.IamConfig;
import io.quarkus.amazon.iam.runtime.IamRecorder;
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
import software.amazon.awssdk.services.iam.IamAsyncClient;
import software.amazon.awssdk.services.iam.IamClient;

public class IamProcessor extends AbstractAmazonServiceProcessor {

    IamBuildTimeConfig buildTimeConfig;

    @Override
    protected Feature amazonServiceClientName() {
        return Feature.AMAZON_IAM;
    }

    @Override
    protected String configName() {
        return "iam";
    }

    @Override
    protected DotName syncClientName() {
        return DotName.createSimple(IamClient.class.getName());
    }

    @Override
    protected DotName asyncClientName() {
        return DotName.createSimple(IamAsyncClient.class.getName());
    }

    @Override
    protected String builtinInterceptorsPath() {
        return "software/amazon/awssdk/services/iam/execution.interceptors";
    }

    @BuildStep
    AdditionalBeanBuildItem producer() {
        return AdditionalBeanBuildItem.unremovableOf(IamClientProducer.class);
    }

    @BuildStep
    void setup(BeanRegistrationPhaseBuildItem beanRegistrationPhase,
            BuildProducer<ExtensionSslNativeSupportBuildItem> extensionSslNativeSupport,
            BuildProducer<FeatureBuildItem> feature,
            BuildProducer<AmazonClientInterceptorsPathBuildItem> interceptors,
            BuildProducer<AmazonClientBuildItem> clientProducer) {

        setupExtension(beanRegistrationPhase, extensionSslNativeSupport, feature, interceptors, clientProducer,
                buildTimeConfig.sdk, buildTimeConfig.syncClient);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void setupTransport(List<AmazonClientBuildItem> amazonClients, IamRecorder recorder,
            AmazonClientTransportRecorder transportRecorder,
            IamConfig runtimeConfig, BuildProducer<AmazonClientTransportsBuildItem> clientTransportBuildProducer) {

        createTransportBuilders(amazonClients,
                transportRecorder,
                buildTimeConfig.syncClient,
                recorder.getSyncConfig(runtimeConfig),
                recorder.getAsyncConfig(runtimeConfig),
                clientTransportBuildProducer);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void createClientBuilders(List<AmazonClientTransportsBuildItem> transportBuildItems, IamRecorder recorder,
            IamConfig runtimeConfig, BuildProducer<AmazonClientBuilderBuildItem> builderProducer) {

        createClientBuilders(transportBuildItems, builderProducer,
                (syncTransport) -> recorder.createSyncBuilder(runtimeConfig, syncTransport),
                (asyncTransport) -> recorder.createAsyncBuilder(runtimeConfig, asyncTransport));
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void configureClient(List<AmazonClientBuilderBuildItem> clients, IamRecorder recorder,
            AmazonClientRecorder commonRecorder,
            IamConfig runtimeConfig,
            BuildProducer<AmazonClientBuilderConfiguredBuildItem> producer) {

        initClientBuilders(clients, commonRecorder, recorder.getAwsConfig(runtimeConfig), recorder.getSdkConfig(runtimeConfig),
                buildTimeConfig.sdk, producer);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void buildClients(List<AmazonClientBuilderConfiguredBuildItem> configuredClients, IamRecorder recorder,
            BeanContainerBuildItem beanContainer,
            ShutdownContextBuildItem shutdown) {

        buildClients(configuredClients,
                (syncBuilder) -> recorder.buildClient(syncBuilder, beanContainer.getValue(), shutdown),
                (asyncBuilder) -> recorder.buildAsyncClient(asyncBuilder, beanContainer.getValue(), shutdown));
    }
}
