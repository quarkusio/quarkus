package io.quarkus.vault;

import java.util.OptionalInt;

import org.jboss.jandex.DotName;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationSourceBuildItem;
import io.quarkus.deployment.builditem.SslNativeConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.vault.runtime.Base64StringDeserializer;
import io.quarkus.vault.runtime.Base64StringSerializer;
import io.quarkus.vault.runtime.VaultRecorder;
import io.quarkus.vault.runtime.VaultServiceProducer;
import io.quarkus.vault.runtime.client.dto.VaultModel;
import io.quarkus.vault.runtime.config.VaultConfigSource;
import io.quarkus.vault.runtime.config.VaultRuntimeConfig;

public class VaultProcessor {

    private static final Logger log = Logger.getLogger(VaultProcessor.class);

    @BuildStep
    void build(
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            BuildProducer<FeatureBuildItem> feature,
            CombinedIndexBuildItem combinedIndexBuildItem,
            SslNativeConfigBuildItem sslNativeConfig,
            BuildProducer<ExtensionSslNativeSupportBuildItem> sslNativeSupport) {

        feature.produce(new FeatureBuildItem(FeatureBuildItem.VAULT));

        final String[] modelClasses = combinedIndexBuildItem.getIndex()
                .getAllKnownImplementors(DotName.createSimple(VaultModel.class.getName()))
                .stream()
                .map(c -> c.name().toString())
                .toArray(String[]::new);
        reflectiveClasses.produce(ReflectiveClassBuildItem.weakClass(modelClasses));
        reflectiveClasses.produce(
                new ReflectiveClassBuildItem(false, false, Base64StringDeserializer.class, Base64StringSerializer.class));

        sslNativeSupport.produce(new ExtensionSslNativeSupportBuildItem(FeatureBuildItem.VAULT));
    }

    @BuildStep
    void setUpConfigFile(BuildProducer<RunTimeConfigurationSourceBuildItem> configSourceConsumer) {
        configSourceConsumer.produce(new RunTimeConfigurationSourceBuildItem(
                VaultConfigSource.class.getName(), OptionalInt.of(150)));
    }

    @BuildStep
    AdditionalBeanBuildItem registerAdditionalBeans() {
        return new AdditionalBeanBuildItem.Builder()
                .setUnremovable()
                .addBeanClass(VaultServiceProducer.class)
                .addBeanClass(CredentialsProvider.class)
                .addBeanClass(VaultKVSecretEngine.class)
                .build();
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    void configure(VaultRecorder recorder, VaultRuntimeConfig serverConfig) {
        recorder.configureRuntimeProperties(serverConfig);
    }

}
