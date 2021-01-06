package io.quarkus.oidc.client.deployment;

import java.util.Arrays;
import java.util.List;
import java.util.function.BooleanSupplier;

import javax.inject.Singleton;

import io.quarkus.arc.BeanDestroyer;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.EnableAllSecurityServicesBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.oidc.client.OidcClient;
import io.quarkus.oidc.client.OidcClients;
import io.quarkus.oidc.client.runtime.OidcClientBuildTimeConfig;
import io.quarkus.oidc.client.runtime.OidcClientRecorder;
import io.quarkus.oidc.client.runtime.OidcClientsConfig;
import io.quarkus.oidc.client.runtime.TokensHelper;
import io.quarkus.oidc.client.runtime.TokensProducer;
import io.quarkus.runtime.TlsConfig;
import io.quarkus.vertx.core.deployment.CoreVertxBuildItem;

public class OidcClientBuildStep {

    @BuildStep(onlyIf = IsEnabled.class)
    FeatureBuildItem featureBuildItem() {
        return new FeatureBuildItem(Feature.OIDC_CLIENT);
    }

    @BuildStep(onlyIf = IsEnabled.class)
    EnableAllSecurityServicesBuildItem security() {
        return new EnableAllSecurityServicesBuildItem();
    }

    @BuildStep(onlyIf = IsEnabled.class)
    void registerProvider(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(TokensProducer.class));
    }

    @BuildStep(onlyIf = IsEnabled.class)
    void runtimeInitializeTokenHelper(BuildProducer<RuntimeInitializedClassBuildItem> runtime) {
        runtime.produce(new RuntimeInitializedClassBuildItem(TokensHelper.class.getName()));
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep(onlyIf = IsEnabled.class)
    public List<SyntheticBeanBuildItem> setup(
            OidcClientsConfig oidcConfig,
            TlsConfig tlsConfig,
            OidcClientRecorder recorder,
            CoreVertxBuildItem vertxBuildItem) {

        OidcClients clients = recorder.setup(oidcConfig, tlsConfig, vertxBuildItem.getVertx());

        SyntheticBeanBuildItem oidcClientBuildItem = SyntheticBeanBuildItem.configure(OidcClient.class).unremovable()
                .types(OidcClient.class)
                .supplier(recorder.createOidcClientBean(clients))
                .scope(Singleton.class)
                .setRuntimeInit()
                .destroyer(BeanDestroyer.CloseableDestroyer.class)
                .done();
        SyntheticBeanBuildItem oidcClientsBuildItem = SyntheticBeanBuildItem.configure(OidcClients.class).unremovable()
                .types(OidcClients.class)
                .supplier(recorder.createOidcClientsBean(clients))
                .scope(Singleton.class)
                .setRuntimeInit()
                .destroyer(BeanDestroyer.CloseableDestroyer.class)
                .done();
        return Arrays.asList(oidcClientBuildItem, oidcClientsBuildItem);
    }

    public static class IsEnabled implements BooleanSupplier {
        OidcClientBuildTimeConfig config;

        public boolean getAsBoolean() {
            return config.enabled;
        }
    }
}
