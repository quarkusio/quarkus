package io.quarkus.vertx.http.security;

import java.util.Set;
import java.util.function.Supplier;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Priority;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AuthenticationRequest;
import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.quarkus.vertx.http.runtime.security.FormAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.HttpCredentialTransport;
import io.restassured.RestAssured;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

public class CustomFormAuthChallengeTest {

    private static final int EXPECTED_STATUS = 203;
    private static final String EXPECTED_HEADER_NAME = "ElizabethII";
    private static final String EXPECTED_HEADER_VALUE = "CharlesIV";
    private static final String ADMIN = "admin";
    private static final String APP_PROPS = "" +
            "quarkus.http.auth.form.enabled=true\n" +
            "quarkus.http.auth.form.login-page=login\n" +
            "quarkus.http.auth.form.error-page=error\n" +
            "quarkus.http.auth.form.landing-page=landing\n" +
            "quarkus.http.auth.policy.r1.roles-allowed=admin\n" +
            "quarkus.http.auth.session.encryption-key=CHANGEIT-CHANGEIT-CHANGEIT-CHANGEIT-CHANGEIT\n";

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest().setArchiveProducer(new Supplier<>() {
        @Override
        public JavaArchive get() {
            return ShrinkWrap.create(JavaArchive.class)
                    .addClasses(CustomFormAuthenticator.class, TestIdentityProvider.class, TestIdentityController.class,
                            TestTrustedIdentityProvider.class, PathHandler.class)
                    .addAsResource(new StringAsset(APP_PROPS), "application.properties");
        }
    });

    @BeforeAll
    public static void setup() {
        TestIdentityController.resetRoles().add(ADMIN, ADMIN, ADMIN);
    }

    @Test
    public void testCustomGetChallengeIsCalled() {
        RestAssured
                .given()
                .when()
                .formParam("j_username", ADMIN)
                .formParam("j_password", "wrong_password")
                .post("/j_security_check")
                .then()
                .assertThat()
                .statusCode(EXPECTED_STATUS)
                .header(EXPECTED_HEADER_NAME, Matchers.is(EXPECTED_HEADER_VALUE));
    }

    @Alternative
    @Priority(1)
    @ApplicationScoped
    public static class CustomFormAuthenticator implements HttpAuthenticationMechanism {

        @Inject
        FormAuthenticationMechanism delegate;

        @Override
        public Uni<SecurityIdentity> authenticate(RoutingContext context, IdentityProviderManager identityProviderManager) {
            final var authenticate = delegate.authenticate(context, identityProviderManager);
            context.put(HttpAuthenticationMechanism.class.getName(), this);
            return authenticate;
        }

        @Override
        public Uni<ChallengeData> getChallenge(RoutingContext context) {
            return Uni.createFrom().item(new ChallengeData(EXPECTED_STATUS, EXPECTED_HEADER_NAME, EXPECTED_HEADER_VALUE));
        }

        @Override
        public Set<Class<? extends AuthenticationRequest>> getCredentialTypes() {
            return delegate.getCredentialTypes();
        }

        @Override
        public Uni<HttpCredentialTransport> getCredentialTransport(RoutingContext context) {
            return delegate.getCredentialTransport(context);
        }
    }

}
