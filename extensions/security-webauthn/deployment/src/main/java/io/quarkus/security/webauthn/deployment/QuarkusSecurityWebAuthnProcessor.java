package io.quarkus.security.webauthn.deployment;

import java.util.List;
import java.util.function.BooleanSupplier;

import jakarta.inject.Singleton;

import org.jboss.jandex.DotName;

import com.webauthn4j.data.AuthenticationRequest;
import com.webauthn4j.data.AuthenticatorAssertionResponse;
import com.webauthn4j.data.AuthenticatorAttestationResponse;
import com.webauthn4j.data.PublicKeyCredential;
import com.webauthn4j.data.PublicKeyCredentialCreationOptions;
import com.webauthn4j.data.PublicKeyCredentialParameters;
import com.webauthn4j.data.PublicKeyCredentialRequestOptions;
import com.webauthn4j.data.PublicKeyCredentialRpEntity;
import com.webauthn4j.data.PublicKeyCredentialType;
import com.webauthn4j.data.PublicKeyCredentialUserEntity;
import com.webauthn4j.data.RegistrationRequest;
import com.webauthn4j.data.attestation.AttestationObject;
import com.webauthn4j.data.client.CollectedClientData;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;
import io.quarkus.security.webauthn.WebAuthn;
import io.quarkus.security.webauthn.WebAuthnAuthenticationMechanism;
import io.quarkus.security.webauthn.WebAuthnAuthenticatorStorage;
import io.quarkus.security.webauthn.WebAuthnBuildTimeConfig;
import io.quarkus.security.webauthn.WebAuthnRecorder;
import io.quarkus.security.webauthn.WebAuthnSecurity;
import io.quarkus.security.webauthn.WebAuthnTrustedIdentityProvider;
import io.quarkus.vertx.http.deployment.HttpAuthMechanismAnnotationBuildItem;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.deployment.VertxWebRouterBuildItem;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;

@BuildSteps(onlyIf = QuarkusSecurityWebAuthnProcessor.IsEnabled.class)
class QuarkusSecurityWebAuthnProcessor {

    @BuildStep
    public IndexDependencyBuildItem addTypesToJandex() {
        // needed by registerJacksonTypes()
        return new IndexDependencyBuildItem("com.webauthn4j", "webauthn4j-core");
    }

    @BuildStep
    public void registerJacksonTypes(BuildProducer<ReflectiveHierarchyBuildItem> reflection) {
        reflection.produce(
                ReflectiveHierarchyBuildItem.builder(AuthenticatorAssertionResponse.class).build());
        reflection.produce(
                ReflectiveHierarchyBuildItem.builder(AuthenticatorAttestationResponse.class).build());
        reflection.produce(ReflectiveHierarchyBuildItem.builder(AuthenticationRequest.class).build());
        reflection.produce(ReflectiveHierarchyBuildItem.builder(RegistrationRequest.class).build());
        reflection.produce(
                ReflectiveHierarchyBuildItem.builder(PublicKeyCredentialCreationOptions.class).build());
        reflection.produce(
                ReflectiveHierarchyBuildItem.builder(PublicKeyCredentialRequestOptions.class).build());
        reflection.produce(
                ReflectiveHierarchyBuildItem.builder(PublicKeyCredentialRpEntity.class).build());
        reflection.produce(
                ReflectiveHierarchyBuildItem.builder(PublicKeyCredentialUserEntity.class).build());
        reflection.produce(
                ReflectiveHierarchyBuildItem.builder(PublicKeyCredentialParameters.class).build());
        reflection.produce(
                ReflectiveHierarchyBuildItem.builder(PublicKeyCredentialType.class).build());
        reflection.produce(
                ReflectiveHierarchyBuildItem.builder(PublicKeyCredential.class).build());
        reflection.produce(
                ReflectiveHierarchyBuildItem.builder(AttestationObject.class).build());
        reflection.produce(
                ReflectiveHierarchyBuildItem.builder(CollectedClientData.class).build());
    }

    @BuildStep
    public void myBeans(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        AdditionalBeanBuildItem.Builder builder = AdditionalBeanBuildItem.builder().setUnremovable();

        builder.addBeanClass(WebAuthnSecurity.class)
                .addBeanClass(WebAuthnAuthenticatorStorage.class)
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
    @Record(ExecutionTime.RUNTIME_INIT)
    SyntheticBeanBuildItem initWebAuthnAuth(
            WebAuthnRecorder recorder) {
        return SyntheticBeanBuildItem.configure(WebAuthnAuthenticationMechanism.class)
                .types(HttpAuthenticationMechanism.class)
                .setRuntimeInit()
                .scope(Singleton.class)
                .supplier(recorder.setupWebAuthnAuthenticationMechanism()).done();
    }

    @BuildStep
    List<HttpAuthMechanismAnnotationBuildItem> registerHttpAuthMechanismAnnotation() {
        return List.of(
                new HttpAuthMechanismAnnotationBuildItem(DotName.createSimple(WebAuthn.class), WebAuthn.AUTH_MECHANISM_SCHEME));
    }

    public static class IsEnabled implements BooleanSupplier {
        WebAuthnBuildTimeConfig config;

        public boolean getAsBoolean() {
            return config.enabled();
        }
    }

}
