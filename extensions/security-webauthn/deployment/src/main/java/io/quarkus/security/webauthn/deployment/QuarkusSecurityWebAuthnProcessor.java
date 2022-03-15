package io.quarkus.security.webauthn.deployment;

import java.util.function.BooleanSupplier;

import javax.inject.Singleton;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.security.webauthn.WebAuthnAuthenticationMechanism;
import io.quarkus.security.webauthn.WebAuthnAuthenticatorStorage;
import io.quarkus.security.webauthn.WebAuthnBuildTimeConfig;
import io.quarkus.security.webauthn.WebAuthnIdentityProvider;
import io.quarkus.security.webauthn.WebAuthnRecorder;
import io.quarkus.security.webauthn.WebAuthnSecurity;
import io.quarkus.security.webauthn.WebAuthnTrustedIdentityProvider;
import io.quarkus.vertx.http.deployment.VertxWebRouterBuildItem;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;

class QuarkusSecurityWebAuthnProcessor {

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.SECURITY_WEBAUTHN);
    }

    @BuildStep(onlyIf = IsEnabled.class)
    public void myBeans(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        AdditionalBeanBuildItem.Builder builder = AdditionalBeanBuildItem.builder().setUnremovable();

        builder.addBeanClass(WebAuthnSecurity.class)
                .addBeanClass(WebAuthnAuthenticatorStorage.class)
                .addBeanClass(WebAuthnIdentityProvider.class)
                .addBeanClass(WebAuthnTrustedIdentityProvider.class);
        additionalBeans.produce(builder.build());
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep(onlyIf = IsEnabled.class)
    public void setup(
            WebAuthnRecorder recorder,
            VertxWebRouterBuildItem vertxWebRouterBuildItem,
            BeanContainerBuildItem beanContainerBuildItem) {
        recorder.setupRoutes(beanContainerBuildItem.getValue(), vertxWebRouterBuildItem.getHttpRouter());
    }

    @BuildStep(onlyIf = IsEnabled.class)
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
