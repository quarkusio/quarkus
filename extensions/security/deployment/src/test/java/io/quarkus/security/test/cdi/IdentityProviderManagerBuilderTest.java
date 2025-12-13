package io.quarkus.security.test.cdi;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.identity.request.AnonymousAuthenticationRequest;
import io.quarkus.security.identity.request.AuthenticationRequest;
import io.quarkus.security.identity.request.BaseAuthenticationRequest;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.security.spi.runtime.IdentityProviderManagerBuilder;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Uni;

public class IdentityProviderManagerBuilderTest {

    private static final String GLOBAL_AUGMENTOR_1 = "global-augmentor-1";
    private static final String GLOBAL_AUGMENTOR_2 = "global-augmentor-2";
    private static final String LOCAL_AUGMENTOR_1 = "local-augmentor-1";
    private static final String LOCAL_AUGMENTOR_2 = "local-augmentor-2";
    private static final String GLOBAL_IDENTITY_PROVIDER_1 = "global-identity-provider-1";
    private static final String GLOBAL_IDENTITY_PROVIDER_2 = "global-identity-provider-2";
    private static final String LOCAL_IDENTITY_PROVIDER_1 = "local-identity-provider-1";
    private static final String LOCAL_IDENTITY_PROVIDER_2 = "local-identity-provider-2";

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(TestProducer.class, NamedIdentityAugmentor.class, TestAuthRequest.class,
                            TestIdentityProvider.class, TestAuthRequest2.class));

    @Inject
    IdentityProviderManagerBuilder builder;

    @Test
    void noLocalProviderOrAugmentor() {
        // verify only global augmentors and identity providers are applied
        IdentityProviderManager ipm = builder.build(null, null);
        SecurityIdentity securityIdentity = ipm.authenticateBlocking(new TestAuthRequest());
        assertThat(securityIdentity.getAttributes())
                .doesNotContainValue(LOCAL_AUGMENTOR_1)
                .doesNotContainValue(LOCAL_AUGMENTOR_2)
                .doesNotContainValue(LOCAL_IDENTITY_PROVIDER_2)
                .containsValues(GLOBAL_IDENTITY_PROVIDER_1, GLOBAL_AUGMENTOR_1, GLOBAL_AUGMENTOR_2);
        securityIdentity = ipm.authenticateBlocking(new TestAuthRequest2());
        assertThat(securityIdentity.getAttributes())
                .doesNotContainValue(LOCAL_AUGMENTOR_1)
                .doesNotContainValue(LOCAL_AUGMENTOR_2)
                .doesNotContainValue(LOCAL_IDENTITY_PROVIDER_1)
                .containsValues(GLOBAL_IDENTITY_PROVIDER_2, GLOBAL_AUGMENTOR_1, GLOBAL_AUGMENTOR_2);
        securityIdentity = ipm.authenticateBlocking(new AnonymousAuthenticationRequest());
        assertThat(securityIdentity.getAttributes())
                .doesNotContainValue(LOCAL_AUGMENTOR_1)
                .doesNotContainValue(LOCAL_AUGMENTOR_2)
                .doesNotContainValue(LOCAL_IDENTITY_PROVIDER_1)
                .doesNotContainValue(LOCAL_IDENTITY_PROVIDER_2)
                .doesNotContainValue(GLOBAL_IDENTITY_PROVIDER_1)
                .doesNotContainValue(GLOBAL_IDENTITY_PROVIDER_2)
                .containsValues(GLOBAL_AUGMENTOR_1, GLOBAL_AUGMENTOR_2);
    }

    @Test
    void onlyLocalProvidersAndAugmentors() {
        Collection<IdentityProvider<?>> providers = List.of(
                new TestIdentityProvider<>(LOCAL_IDENTITY_PROVIDER_1, TestAuthRequest.class),
                new TestIdentityProvider<>(LOCAL_IDENTITY_PROVIDER_2, TestAuthRequest2.class));
        Collection<SecurityIdentityAugmentor> augmentors = List.of(
                new NamedIdentityAugmentor(LOCAL_AUGMENTOR_1),
                new NamedIdentityAugmentor(LOCAL_AUGMENTOR_2));
        IdentityProviderManager ipm = builder.build(providers, augmentors);
        SecurityIdentity securityIdentity = ipm.authenticateBlocking(new TestAuthRequest());
        assertThat(securityIdentity.getAttributes())
                .doesNotContainValue(GLOBAL_AUGMENTOR_1)
                .doesNotContainValue(GLOBAL_AUGMENTOR_2)
                .doesNotContainValue(LOCAL_IDENTITY_PROVIDER_2)
                .doesNotContainValue(GLOBAL_IDENTITY_PROVIDER_1)
                .doesNotContainValue(GLOBAL_IDENTITY_PROVIDER_2)
                .containsValues(LOCAL_IDENTITY_PROVIDER_1, LOCAL_AUGMENTOR_1, LOCAL_AUGMENTOR_2);
        securityIdentity = ipm.authenticateBlocking(new TestAuthRequest2());
        assertThat(securityIdentity.getAttributes())
                .doesNotContainValue(GLOBAL_AUGMENTOR_1)
                .doesNotContainValue(GLOBAL_AUGMENTOR_2)
                .doesNotContainValue(LOCAL_IDENTITY_PROVIDER_1)
                .doesNotContainValue(GLOBAL_IDENTITY_PROVIDER_1)
                .doesNotContainValue(GLOBAL_IDENTITY_PROVIDER_2)
                .containsValues(LOCAL_IDENTITY_PROVIDER_2, LOCAL_AUGMENTOR_1, LOCAL_AUGMENTOR_2);
        securityIdentity = ipm.authenticateBlocking(new AnonymousAuthenticationRequest());
        assertThat(securityIdentity.getAttributes())
                .doesNotContainValue(GLOBAL_AUGMENTOR_1)
                .doesNotContainValue(GLOBAL_AUGMENTOR_2)
                .doesNotContainValue(LOCAL_IDENTITY_PROVIDER_1)
                .doesNotContainValue(LOCAL_IDENTITY_PROVIDER_2)
                .doesNotContainValue(GLOBAL_IDENTITY_PROVIDER_1)
                .doesNotContainValue(GLOBAL_IDENTITY_PROVIDER_2)
                .containsValues(LOCAL_AUGMENTOR_1, LOCAL_AUGMENTOR_2);
    }

    @Test
    void localProviderAndGlobalAugmentors() {
        Collection<IdentityProvider<?>> providers = List.of(
                new TestIdentityProvider<>(LOCAL_IDENTITY_PROVIDER_1, TestAuthRequest.class));
        IdentityProviderManager ipm = builder.build(providers, null);
        SecurityIdentity securityIdentity = ipm.authenticateBlocking(new TestAuthRequest());
        assertThat(securityIdentity.getAttributes())
                .doesNotContainValue(LOCAL_AUGMENTOR_1)
                .doesNotContainValue(LOCAL_AUGMENTOR_2)
                .doesNotContainValue(LOCAL_IDENTITY_PROVIDER_2)
                .doesNotContainValue(GLOBAL_IDENTITY_PROVIDER_1)
                .doesNotContainValue(GLOBAL_IDENTITY_PROVIDER_2)
                .containsValues(LOCAL_IDENTITY_PROVIDER_1, GLOBAL_AUGMENTOR_1, GLOBAL_AUGMENTOR_2);
        securityIdentity = ipm.authenticateBlocking(new AnonymousAuthenticationRequest());
        assertThat(securityIdentity.getAttributes())
                .doesNotContainValue(LOCAL_AUGMENTOR_1)
                .doesNotContainValue(LOCAL_AUGMENTOR_2)
                .doesNotContainValue(LOCAL_IDENTITY_PROVIDER_1)
                .doesNotContainValue(LOCAL_IDENTITY_PROVIDER_2)
                .doesNotContainValue(GLOBAL_IDENTITY_PROVIDER_1)
                .doesNotContainValue(GLOBAL_IDENTITY_PROVIDER_2)
                .containsValues(GLOBAL_AUGMENTOR_1, GLOBAL_AUGMENTOR_2);
    }

    @Test
    void globalProvidersAndLocalAugmentor() {
        Collection<SecurityIdentityAugmentor> augmentors = List.of(
                new NamedIdentityAugmentor(LOCAL_AUGMENTOR_1));
        IdentityProviderManager ipm = builder.build(null, augmentors);
        SecurityIdentity securityIdentity = ipm.authenticateBlocking(new TestAuthRequest());
        assertThat(securityIdentity.getAttributes())
                .doesNotContainValue(GLOBAL_AUGMENTOR_1)
                .doesNotContainValue(GLOBAL_AUGMENTOR_2)
                .doesNotContainValue(LOCAL_AUGMENTOR_2)
                .doesNotContainValue(LOCAL_IDENTITY_PROVIDER_2)
                .doesNotContainValue(LOCAL_IDENTITY_PROVIDER_1)
                .doesNotContainValue(GLOBAL_IDENTITY_PROVIDER_2)
                .containsValues(GLOBAL_IDENTITY_PROVIDER_1, LOCAL_AUGMENTOR_1);
        securityIdentity = ipm.authenticateBlocking(new TestAuthRequest2());
        assertThat(securityIdentity.getAttributes())
                .doesNotContainValue(GLOBAL_AUGMENTOR_1)
                .doesNotContainValue(GLOBAL_AUGMENTOR_2)
                .doesNotContainValue(LOCAL_AUGMENTOR_2)
                .doesNotContainValue(LOCAL_IDENTITY_PROVIDER_2)
                .doesNotContainValue(LOCAL_IDENTITY_PROVIDER_1)
                .doesNotContainValue(GLOBAL_IDENTITY_PROVIDER_1)
                .containsValues(GLOBAL_IDENTITY_PROVIDER_2, LOCAL_AUGMENTOR_1);
        securityIdentity = ipm.authenticateBlocking(new AnonymousAuthenticationRequest());
        assertThat(securityIdentity.getAttributes())
                .doesNotContainValue(GLOBAL_AUGMENTOR_1)
                .doesNotContainValue(GLOBAL_AUGMENTOR_2)
                .doesNotContainValue(LOCAL_IDENTITY_PROVIDER_1)
                .doesNotContainValue(LOCAL_IDENTITY_PROVIDER_2)
                .doesNotContainValue(GLOBAL_IDENTITY_PROVIDER_1)
                .doesNotContainValue(GLOBAL_IDENTITY_PROVIDER_2)
                .doesNotContainValue(LOCAL_AUGMENTOR_2)
                .containsValues(LOCAL_AUGMENTOR_1);
    }

    public static class TestProducer {

        @Produces
        @ApplicationScoped
        SecurityIdentityAugmentor getGlobalAugmentor1() {
            return new NamedIdentityAugmentor(GLOBAL_AUGMENTOR_1);
        }

        @Produces
        @ApplicationScoped
        SecurityIdentityAugmentor getGlobalAugmentor2() {
            return new NamedIdentityAugmentor(GLOBAL_AUGMENTOR_2);
        }

        @Produces
        @ApplicationScoped
        IdentityProvider<TestAuthRequest> getGlobalIdentityProvider1() {
            return new TestIdentityProvider<>(GLOBAL_IDENTITY_PROVIDER_1, TestAuthRequest.class);
        }

        @Produces
        @ApplicationScoped
        IdentityProvider<TestAuthRequest2> getGlobalIdentityProvider2() {
            return new TestIdentityProvider<>(GLOBAL_IDENTITY_PROVIDER_2, TestAuthRequest2.class);
        }

    }

    private static final class NamedIdentityAugmentor implements SecurityIdentityAugmentor {
        private final String name;

        private NamedIdentityAugmentor(String name) {
            this.name = name;
        }

        @Override
        public Uni<SecurityIdentity> augment(SecurityIdentity securityIdentity,
                AuthenticationRequestContext authenticationRequestContext) {
            return Uni.createFrom().item(QuarkusSecurityIdentity.builder(securityIdentity)
                    .addAttribute(name, name)
                    .build());
        }
    }

    private static final class TestAuthRequest extends BaseAuthenticationRequest {

    }

    private static final class TestAuthRequest2 extends BaseAuthenticationRequest {

    }

    private static final class TestIdentityProvider<T extends AuthenticationRequest> implements IdentityProvider<T> {

        private final String name;
        private final Class<T> requestClass;

        private TestIdentityProvider(String name, Class<T> requestClass) {
            this.name = name;
            this.requestClass = requestClass;
        }

        @Override
        public Class<T> getRequestType() {
            return requestClass;
        }

        @Override
        public Uni<SecurityIdentity> authenticate(T testAuthRequest,
                AuthenticationRequestContext authenticationRequestContext) {
            return Uni.createFrom().item(QuarkusSecurityIdentity.builder()
                    .setAnonymous(false)
                    .setPrincipal(new QuarkusPrincipal("test"))
                    .addAttribute(name, name)
                    .build());
        }
    }
}
