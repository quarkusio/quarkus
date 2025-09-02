package io.quarkus.oidc.client.registration.deployment;

import java.util.function.BooleanSupplier;

import jakarta.inject.Singleton;

import io.quarkus.arc.BeanDestroyer;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.oidc.client.registration.OidcClientRegistration;
import io.quarkus.oidc.client.registration.OidcClientRegistrations;
import io.quarkus.oidc.client.registration.runtime.OidcClientRegistrationRecorder;
import io.quarkus.tls.deployment.spi.TlsRegistryBuildItem;
import io.quarkus.vertx.core.deployment.CoreVertxBuildItem;

@BuildSteps(onlyIf = OidcClientRegistrationBuildStep.IsEnabled.class)
public class OidcClientRegistrationBuildStep {

    @BuildStep
    ExtensionSslNativeSupportBuildItem enableSslInNative() {
        return new ExtensionSslNativeSupportBuildItem(Feature.OIDC_CLIENT_REGISTRATION);
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    public void setup(
            OidcClientRegistrationRecorder recorder,
            CoreVertxBuildItem vertxBuildItem,
            TlsRegistryBuildItem tlsRegistry,
            BuildProducer<SyntheticBeanBuildItem> syntheticBean) {

        OidcClientRegistrations oidcClientRegistrations = recorder.setup(vertxBuildItem.getVertx(),
                tlsRegistry.registry());

        syntheticBean.produce(SyntheticBeanBuildItem.configure(OidcClientRegistration.class).unremovable()
                .types(OidcClientRegistration.class)
                .supplier(recorder.createOidcClientRegistrationBean(oidcClientRegistrations))
                .scope(Singleton.class)
                .setRuntimeInit()
                .destroyer(BeanDestroyer.CloseableDestroyer.class)
                .done());

        syntheticBean.produce(SyntheticBeanBuildItem.configure(OidcClientRegistrations.class).unremovable()
                .types(OidcClientRegistrations.class)
                .supplier(recorder.createOidcClientRegistrationsBean(oidcClientRegistrations))
                .scope(Singleton.class)
                .setRuntimeInit()
                .destroyer(BeanDestroyer.CloseableDestroyer.class)
                .done());
    }

    public static class IsEnabled implements BooleanSupplier {
        OidcClientRegistrationBuildTimeConfig config;

        public boolean getAsBoolean() {
            return config.enabled();
        }
    }
}
