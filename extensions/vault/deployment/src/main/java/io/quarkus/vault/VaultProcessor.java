package io.quarkus.vault;

import org.jboss.jandex.DotName;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationSourceValueBuildItem;
import io.quarkus.deployment.builditem.SslNativeConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.runtime.TlsConfig;
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;
import io.quarkus.vault.runtime.Base64StringDeserializer;
import io.quarkus.vault.runtime.Base64StringSerializer;
import io.quarkus.vault.runtime.VaultRecorder;
import io.quarkus.vault.runtime.VaultServiceProducer;
import io.quarkus.vault.runtime.client.dto.VaultModel;
import io.quarkus.vault.runtime.config.VaultBuildTimeConfig;
import io.quarkus.vault.runtime.config.VaultRuntimeConfig;
import io.quarkus.vault.runtime.health.VaultHealthCheck;

public class VaultProcessor {

    @BuildStep
    void addDependencies(BuildProducer<IndexDependencyBuildItem> indexDependency) {
        indexDependency.produce(new IndexDependencyBuildItem("io.quarkus", "quarkus-vault-model"));
    }

    @BuildStep
    void build(
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            BuildProducer<FeatureBuildItem> feature,
            CombinedIndexBuildItem combinedIndexBuildItem,
            SslNativeConfigBuildItem sslNativeConfig,
            BuildProducer<ExtensionSslNativeSupportBuildItem> sslNativeSupport) {

        feature.produce(new FeatureBuildItem(Feature.VAULT));

        final String[] modelClasses = combinedIndexBuildItem.getIndex()
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
    AdditionalBeanBuildItem registerAdditionalBeans() {
        return new AdditionalBeanBuildItem.Builder()
                .setUnremovable()
                .addBeanClass(VaultServiceProducer.class)
                .addBeanClass(VaultKVSecretEngine.class)
                .build();
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    public RunTimeConfigurationSourceValueBuildItem configure(VaultRecorder recorder, VaultBuildTimeConfig buildTimeConfig,
            VaultRuntimeConfig serverConfig, TlsConfig tlsConfig) {
        return new RunTimeConfigurationSourceValueBuildItem(
                recorder.configureRuntimeProperties(buildTimeConfig, serverConfig, tlsConfig));
    }

    @BuildStep
    HealthBuildItem addHealthCheck(VaultBuildTimeConfig config) {
        return new HealthBuildItem(VaultHealthCheck.class.getName(), config.health.enabled);
    }

}
