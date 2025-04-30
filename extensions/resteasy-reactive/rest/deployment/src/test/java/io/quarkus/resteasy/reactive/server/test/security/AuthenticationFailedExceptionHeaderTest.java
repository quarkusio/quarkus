package io.quarkus.resteasy.reactive.server.test.security;

import static io.vertx.core.http.HttpHeaders.LOCATION;
import static org.jboss.resteasy.reactive.RestResponse.StatusCode.FOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AuthenticationRequest;
import io.quarkus.security.identity.request.BaseAuthenticationRequest;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;
import io.restassured.RestAssured;
import io.restassured.http.Header;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

public class AuthenticationFailedExceptionHeaderTest {

    private static final String APP_PROPS = "" +
            "quarkus.http.auth.permission.default.paths=/*\n" +
            "quarkus.http.auth.permission.default.policy=authenticated";

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset(APP_PROPS), "application.properties"));

    @Test
    public void testHeaders() {
        // case-insensitive test that there is only one location header
        // there has been duplicate location when both default auth failure handler and auth ex mapper send challenge
        var response = RestAssured
                .given()
                .redirects()
                .follow(false)
                .when()
                .get("/secured-route");
        response.then().statusCode(FOUND);
        assertEquals(1, response.headers().asList().stream().map(Header::getName).map(String::toLowerCase)
                .filter(LOCATION.toString()::equals).count());
    }

    @ApplicationScoped
    public static class FailingAuthenticator implements HttpAuthenticationMechanism {

        @Override
        public Uni<SecurityIdentity> authenticate(RoutingContext context, IdentityProviderManager identityProviderManager) {
            return Uni.createFrom().failure(new AuthenticationFailedException());
        }

        @Override
        public Set<Class<? extends AuthenticationRequest>> getCredentialTypes() {
            return Set.of(BaseAuthenticationRequest.class);
        }

        @Override
        public Uni<ChallengeData> getChallenge(RoutingContext context) {
            return Uni.createFrom().item(new ChallengeData(FOUND, LOCATION, "http://localhost:8080/"));
        }

    }

    @ApplicationScoped
    public static class BasicIdentityProvider implements IdentityProvider<BaseAuthenticationRequest> {

        @Override
        public Class<BaseAuthenticationRequest> getRequestType() {
            return BaseAuthenticationRequest.class;
        }

        @Override
        public Uni<SecurityIdentity> authenticate(
                BaseAuthenticationRequest simpleAuthenticationRequest,
                AuthenticationRequestContext authenticationRequestContext) {
            return Uni.createFrom().nothing();
        }
    }
}
