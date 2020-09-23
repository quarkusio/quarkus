package io.quarkus.vault;

import java.io.File;
import java.util.OptionalInt;

import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.jboss.jandex.JarIndexer;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationSourceBuildItem;
import io.quarkus.deployment.builditem.SslNativeConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;
import io.quarkus.vault.runtime.Base64StringDeserializer;
import io.quarkus.vault.runtime.Base64StringSerializer;
import io.quarkus.vault.runtime.VaultRecorder;
import io.quarkus.vault.runtime.VaultServiceProducer;
import io.quarkus.vault.runtime.client.dto.VaultModel;
import io.quarkus.vault.runtime.config.VaultBuildTimeConfig;
import io.quarkus.vault.runtime.config.VaultConfigSource;
import io.quarkus.vault.runtime.config.VaultRuntimeConfig;

public class VaultProcessor {

    @BuildStep
    void build(
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            BuildProducer<FeatureBuildItem> feature,
            SslNativeConfigBuildItem sslNativeConfig,
            BuildProducer<ExtensionSslNativeSupportBuildItem> sslNativeSupport) throws Exception {

        feature.produce(new FeatureBuildItem(Feature.VAULT));

        // Manually index the runtime module because we need to find all model classes in it
        File runtimeJar = new File(VaultModel.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        Indexer indexer = new Indexer();
        JarIndexer.createJarIndex(runtimeJar, indexer, false, false, false);
        Index runtimeModuleIndex = indexer.complete();

        final String[] modelClasses = runtimeModuleIndex
                .getAllKnownImplementors(DotName.createSimple(VaultModel.class.getName()))
                .stream()
                .map(c -> c.name().toString())
                .toArray(String[]::new);
        reflectiveClasses.produce(ReflectiveClassBuildItem.weakClass(modelClasses));
        reflectiveClasses.produce(
                new ReflectiveClassBuildItem(false, false, Base64StringDeserializer.class, Base64StringSerializer.class));

        sslNativeSupport.produce(new ExtensionSslNativeSupportBuildItem(Feature.VAULT));
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
                .addBeanClass(VaultKVSecretEngine.class)
                .build();
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    void configure(VaultRecorder recorder, VaultBuildTimeConfig buildTimeConfig, VaultRuntimeConfig serverConfig) {
        recorder.configureRuntimeProperties(buildTimeConfig, serverConfig);
    }

    @BuildStep
    HealthBuildItem addHealthCheck(VaultBuildTimeConfig config) {
        return new HealthBuildItem("io.quarkus.vault.runtime.health.VaultHealthCheck",
                config.health.enabled);
    }

}
