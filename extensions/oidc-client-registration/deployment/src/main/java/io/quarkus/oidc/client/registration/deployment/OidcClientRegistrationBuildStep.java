package io.quarkus.oidc.client.registration.deployment;

import java.io.IOException;
import java.util.Optional;
import java.util.function.BooleanSupplier;

import jakarta.inject.Singleton;

import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.core.Phase;
import io.quarkus.core.deployment.service.ServiceRegistrar;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.oidc.client.registration.OidcClientRegistration;
import io.quarkus.oidc.client.registration.OidcClientRegistrations;
import io.quarkus.oidc.client.registration.runtime.DefaultOidcClientRegistrationActiveCheck;
import io.quarkus.oidc.client.registration.runtime.OidcClientRegistrationRecorder;
import io.quarkus.oidc.client.registration.runtime.OidcClientRegistrationsConfig;
import io.quarkus.proxy.ProxyConfigurationRegistry;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.vertx.core.Vertx;

@BuildSteps(onlyIf = OidcClientRegistrationBuildStep.IsEnabled.class)
public class OidcClientRegistrationBuildStep {

    @BuildStep
    ExtensionSslNativeSupportBuildItem enableSslInNative() {
        return new ExtensionSslNativeSupportBuildItem(Feature.OIDC_CLIENT_REGISTRATION);
    }

    @BuildStep
    void setup(ServiceRegistrar serviceRegistrar, BuildProducer<SyntheticBeanBuildItem> syntheticBean) {
        serviceRegistrar
                .forService(OidcClientRegistrations.class)
                .atPhase(Phase.DATA)
                .require(ProxyConfigurationRegistry.class)
                .require(TlsConfigurationRegistry.class)
                .require(Vertx.class)
                .require(OidcClientRegistrationsConfig.class)
                .onStart((ctx, proxyConfigurationRegistry, tlsConfigurationRegistry, vertx, runtimeConfig) -> {
                    OidcClientRegistrations oidcClientRegistrations = OidcClientRegistrationRecorder.setup(vertx,
                            tlsConfigurationRegistry, proxyConfigurationRegistry, runtimeConfig);
                    ctx.onStop(() -> {
                        try {
                            oidcClientRegistrations.close();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                    return oidcClientRegistrations;
                });

        serviceRegistrar
                .forService(OidcClientRegistration.class)
                .atPhase(Phase.DATA)
                .require(OidcClientRegistrations.class)
                .optional()
                .onStart((ctx, oidcClientRegistrations) -> Optional
                        .ofNullable(oidcClientRegistrations.getClientRegistration()));

        // this bean is inactive when the default client registration is not configured
        syntheticBean.produce(SyntheticBeanBuildItem.configure(OidcClientRegistration.class).unremovable()
                .types(OidcClientRegistration.class)
                .serviceValue(OidcClientRegistration.class)
                .checkActive(DefaultOidcClientRegistrationActiveCheck.class)
                .scope(Singleton.class)
                .setRuntimeInit()
                .done());

        syntheticBean.produce(SyntheticBeanBuildItem.configure(OidcClientRegistrations.class).unremovable()
                .types(OidcClientRegistrations.class)
                .serviceValue(OidcClientRegistrations.class)
                .scope(Singleton.class)
                .setRuntimeInit()
                .done());
    }

    public static class IsEnabled implements BooleanSupplier {
        OidcClientRegistrationBuildTimeConfig config;

        public boolean getAsBoolean() {
            return config.enabled();
        }
    }
}
