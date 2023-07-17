package io.quarkus.security.webauthn.deployment;

import java.util.function.BooleanSupplier;

import jakarta.inject.Singleton;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.security.webauthn.WebAuthnAuthenticationMechanism;
import io.quarkus.security.webauthn.WebAuthnAuthenticatorStorage;
import io.quarkus.security.webauthn.WebAuthnBuildTimeConfig;
import io.quarkus.security.webauthn.WebAuthnIdentityProvider;
import io.quarkus.security.webauthn.WebAuthnRecorder;
import io.quarkus.security.webauthn.WebAuthnSecurity;
import io.quarkus.security.webauthn.WebAuthnTrustedIdentityProvider;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.deployment.VertxWebRouterBuildItem;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;
import io.vertx.ext.auth.webauthn.impl.attestation.Attestation;

@BuildSteps(onlyIf = QuarkusSecurityWebAuthnProcessor.IsEnabled.class)
class QuarkusSecurityWebAuthnProcessor {

    @BuildStep
    public void myBeans(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        AdditionalBeanBuildItem.Builder builder = AdditionalBeanBuildItem.builder().setUnremovable();

        builder.addBeanClass(WebAuthnSecurity.class)
                .addBeanClass(WebAuthnAuthenticatorStorage.class)
                .addBeanClass(WebAuthnIdentityProvider.class)
                .addBeanClass(WebAuthnTrustedIdentityProvider.class);
        additionalBeans.produce(builder.build());
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    public void setup(
            WebAuthnRecorder recorder,
            VertxWebRouterBuildItem vertxWebRouterBuildItem,
            BeanContainerBuildItem beanContainerBuildItem,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem) {
        recorder.setupRoutes(beanContainerBuildItem.getValue(), vertxWebRouterBuildItem.getHttpRouter(),
                nonApplicationRootPathBuildItem.getNonApplicationRootPath());
    }

    @BuildStep
    public ServiceProviderBuildItem serviceLoader() {
        return ServiceProviderBuildItem.allProvidersFromClassPath(Attestation.class.getName());
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    SyntheticBeanBuildItem initWebAuthnAuth(
            WebAuthnRecorder recorder) {
        return SyntheticBeanBuildItem.configure(WebAuthnAuthenticationMechanism.class)
                .types(HttpAuthenticationMechanism.class)
                .setRuntimeInit()
                .scope(Singleton.class)
                .supplier(recorder.setupWebAuthnAuthenticationMechanism()).done();
    }

    public static class IsEnabled implements BooleanSupplier {
        WebAuthnBuildTimeConfig config;

        public boolean getAsBoolean() {
            return config.enabled;
        }
    }

}
