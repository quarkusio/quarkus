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
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;
import io.quarkus.vault.runtime.Base64StringDeserializer;
import io.quarkus.vault.runtime.Base64StringSerializer;
import io.quarkus.vault.runtime.VaultAuthManager;
import io.quarkus.vault.runtime.VaultConfigHolder;
import io.quarkus.vault.runtime.VaultCredentialsProvider;
import io.quarkus.vault.runtime.VaultDbManager;
import io.quarkus.vault.runtime.VaultKubernetesAuthManager;
import io.quarkus.vault.runtime.VaultKvManager;
import io.quarkus.vault.runtime.VaultPKIManager;
import io.quarkus.vault.runtime.VaultPKIManagerFactory;
import io.quarkus.vault.runtime.VaultRecorder;
import io.quarkus.vault.runtime.VaultSystemBackendManager;
import io.quarkus.vault.runtime.VaultTOTPManager;
import io.quarkus.vault.runtime.VaultTransitManager;
import io.quarkus.vault.runtime.client.VertxVaultClient;
import io.quarkus.vault.runtime.client.authmethod.VaultInternalAppRoleAuthMethod;
import io.quarkus.vault.runtime.client.authmethod.VaultInternalKubernetesAuthMethod;
import io.quarkus.vault.runtime.client.authmethod.VaultInternalTokenAuthMethod;
import io.quarkus.vault.runtime.client.authmethod.VaultInternalUserpassAuthMethod;
import io.quarkus.vault.runtime.client.backend.VaultInternalSystemBackend;
import io.quarkus.vault.runtime.client.dto.VaultModel;
import io.quarkus.vault.runtime.client.secretengine.VaultInternalDatabaseSecretEngine;
import io.quarkus.vault.runtime.client.secretengine.VaultInternalKvV1SecretEngine;
import io.quarkus.vault.runtime.client.secretengine.VaultInternalKvV2SecretEngine;
import io.quarkus.vault.runtime.client.secretengine.VaultInternalPKISecretEngine;
import io.quarkus.vault.runtime.client.secretengine.VaultInternalTOPTSecretEngine;
import io.quarkus.vault.runtime.client.secretengine.VaultInternalTransitSecretEngine;
import io.quarkus.vault.runtime.config.VaultBootstrapConfig;
import io.quarkus.vault.runtime.config.VaultBuildTimeConfig;
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
                .addBeanClass(VaultCredentialsProvider.class)
                .addBeanClass(VaultKvManager.class)
                .addBeanClass(VaultTransitManager.class)
                .addBeanClass(VaultTOTPManager.class)
                .addBeanClass(VaultSystemBackendManager.class)
                .addBeanClass(VaultKubernetesAuthManager.class)
                .addBeanClass(VaultAuthManager.class)
                .addBeanClass(VaultDbManager.class)
                .addBeanClass(VertxVaultClient.class)
                .addBeanClass(VaultConfigHolder.class)
                .addBeanClass(VaultPKIManager.class)
                .addBeanClass(VaultPKIManagerFactory.class)
                .addBeanClass(VaultInternalKvV1SecretEngine.class)
                .addBeanClass(VaultInternalKvV2SecretEngine.class)
                .addBeanClass(VaultInternalTransitSecretEngine.class)
                .addBeanClass(VaultInternalTOPTSecretEngine.class)
                .addBeanClass(VaultInternalSystemBackend.class)
                .addBeanClass(VaultInternalAppRoleAuthMethod.class)
                .addBeanClass(VaultInternalKubernetesAuthMethod.class)
                .addBeanClass(VaultInternalTokenAuthMethod.class)
                .addBeanClass(VaultInternalUserpassAuthMethod.class)
                .addBeanClass(VaultInternalDatabaseSecretEngine.class)
                .addBeanClass(VaultInternalPKISecretEngine.class)
                .build();
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    RunTimeConfigurationSourceValueBuildItem init(VaultRecorder recorder, VaultBootstrapConfig vaultBootstrapConfig) {
        return new RunTimeConfigurationSourceValueBuildItem(recorder.configure(vaultBootstrapConfig));
    }

    @BuildStep
    HealthBuildItem addHealthCheck(VaultBuildTimeConfig config) {
        return new HealthBuildItem(VaultHealthCheck.class.getName(), config.health.enabled);
    }
}
