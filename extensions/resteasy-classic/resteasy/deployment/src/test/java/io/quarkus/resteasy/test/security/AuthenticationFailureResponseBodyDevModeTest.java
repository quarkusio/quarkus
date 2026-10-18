package io.quarkus.resteasy.test.security;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.Authenticated;
import io.quarkus.security.AuthenticationCompletionException;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AuthenticationRequest;
import io.quarkus.security.identity.request.CertificateAuthenticationRequest;
import io.quarkus.test.QuarkusDevModeTest;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;
import io.restassured.RestAssured;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

public class AuthenticationFailureResponseBodyDevModeTest {

    private static final String RESPONSE_BODY = "failure";

    public enum AuthFailure {
        AUTH_FAILED_WITH_BODY(() -> new AuthenticationFailedException(RESPONSE_BODY), true),
        AUTH_COMPLETION_WITH_BODY(() -> new AuthenticationCompletionException(RESPONSE_BODY), true),
        AUTH_FAILED_WITHOUT_BODY(AuthenticationFailedException::new, false),
        AUTH_COMPLETION_WITHOUT_BODY(AuthenticationCompletionException::new, false);

        public final Supplier<Throwable> failureSupplier;
        private final boolean expectBody;

        AuthFailure(Supplier<Throwable> failureSupplier, boolean expectBody) {
            this.failureSupplier = failureSupplier;
            this.expectBody = expectBody;
        }
    }

    @RegisterExtension
    static QuarkusDevModeTest runner = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(SecuredResource.class, FailingAuthenticator.class, AuthFailure.class)
                    .addAsResource(new StringAsset("""
                            quarkus.http.auth.proactive=false
                            """), "application.properties"));

    @Test
    public void testAuthenticationFailedExceptionBody() {
        assertExceptionBody(AuthFailure.AUTH_FAILED_WITHOUT_BODY, false);
        assertExceptionBody(AuthFailure.AUTH_FAILED_WITHOUT_BODY, true);
        assertExceptionBody(AuthFailure.AUTH_FAILED_WITH_BODY, false);
        assertExceptionBody(AuthFailure.AUTH_FAILED_WITH_BODY, true);
    }

    @Test
    public void testAuthenticationCompletionExceptionBody() {
        assertExceptionBody(AuthFailure.AUTH_COMPLETION_WITHOUT_BODY, false);
        assertExceptionBody(AuthFailure.AUTH_COMPLETION_WITH_BODY, false);
    }

    private static void assertExceptionBody(AuthFailure failure, boolean challenge) {
        int statusCode = challenge ? 302 : 401;
        boolean expectBody = failure.expectBody && statusCode == 401;
        RestAssured
                .given()
                .redirects().follow(false)
                .header("auth-failure", failure.toString())
                .header("challenge-data", challenge)
                .get("/secured")
                .then()
                .statusCode(statusCode)
                .body(expectBody ? Matchers.equalTo(RESPONSE_BODY) : Matchers.not(Matchers.containsString(RESPONSE_BODY)));
    }

    @Authenticated
    @Path("secured")
    public static class SecuredResource {

        @GET
        public String ignored() {
            return "ignored";
        }

    }

    @ApplicationScoped
    public static class FailingAuthenticator implements HttpAuthenticationMechanism {

        @Override
        public Uni<SecurityIdentity> authenticate(RoutingContext context, IdentityProviderManager identityProviderManager) {
            return Uni.createFrom().failure(getFailureProducer(context));
        }

        private static Supplier<Throwable> getFailureProducer(RoutingContext context) {
            return getAuthFailure(context).failureSupplier;
        }

        private static AuthFailure getAuthFailure(RoutingContext context) {
            return AuthFailure.valueOf(context.request().getHeader("auth-failure"));
        }

        @Override
        public Set<Class<? extends AuthenticationRequest>> getCredentialTypes() {
            // so that we don't need to implement an identity provider
            return Collections.singleton(CertificateAuthenticationRequest.class);
        }

        @Override
        public Uni<ChallengeData> getChallenge(RoutingContext context) {
            if (Boolean.parseBoolean(context.request().getHeader("challenge-data"))) {
                return Uni.createFrom().item(new ChallengeData(302, null, null));
            } else {
                return Uni.createFrom().optional(Optional.empty());
            }
        }

    }
}
