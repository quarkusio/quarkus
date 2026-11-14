package io.quarkus.vertx.http.security;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.quarkus.vertx.http.runtime.security.HttpSecurityPolicy;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;

/**
 * Verifies that synthetic beans initialized during the runtime init, like the Vert.x instance, can be injected
 * into the beans that are created when the HTTP Security is initialized. That is the {@link HttpSecurity} event
 * observers and custom {@link HttpSecurityPolicy} beans.
 */
public class HttpSecurityRuntimeInitBeanInjectionTest {

    private static volatile boolean observerInvoked = false;
    private static volatile boolean policyCreated = false;

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(HttpSecurityConfig.class, CustomPolicy.class));

    @Test
    void testRuntimeInitSyntheticBeansInjected() {
        assertThat(observerInvoked).isTrue();
        assertThat(policyCreated).isTrue();
    }

    @Singleton
    public static class HttpSecurityConfig {

        @Inject
        Vertx vertx;

        void configure(@Observes HttpSecurity httpSecurity, TlsConfigurationRegistry tlsConfigRegistry) {
            observerInvoked = vertx != null && tlsConfigRegistry != null;
        }

    }

    @ApplicationScoped
    public static class CustomPolicy implements HttpSecurityPolicy {

        @Inject
        Vertx vertx;

        @Override
        public Uni<CheckResult> checkPermission(RoutingContext request, Uni<SecurityIdentity> identity,
                AuthorizationRequestContext requestContext) {
            return CheckResult.permit();
        }

        @Override
        public String name() {
            policyCreated = vertx != null;
            return "custom";
        }

    }
}
