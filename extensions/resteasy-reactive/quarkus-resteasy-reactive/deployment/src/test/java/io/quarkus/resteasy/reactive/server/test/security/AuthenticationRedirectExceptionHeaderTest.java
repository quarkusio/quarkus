package io.quarkus.resteasy.reactive.server.test.security;

import static io.vertx.core.http.HttpHeaders.CACHE_CONTROL;
import static io.vertx.core.http.HttpHeaders.LOCATION;
import static org.jboss.resteasy.reactive.RestResponse.StatusCode.FOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.AuthenticationRedirectException;
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
import io.restassured.response.Response;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

public class AuthenticationRedirectExceptionHeaderTest {

    private static final String APP_PROPS = "" +
            "quarkus.http.auth.permission.default.paths=/*\n" +
            "quarkus.http.auth.permission.default.policy=authenticated";

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset(APP_PROPS), "application.properties"));

    @Test
    public void testHeaders() {
        // case-insensitive test that Pragma, cache-control and location headers are only present once
        // there were duplicate headers when both default auth failure handler and auth ex mapper set headers
        var response = RestAssured
                .given()
                .redirects()
                .follow(false)
                .when()
                .get("/secured-route");
        response.then().statusCode(FOUND);
        assertEquals(1, getHeaderCount(response, LOCATION.toString()));
        assertEquals(1, getHeaderCount(response, CACHE_CONTROL.toString()));
        assertEquals(1, getHeaderCount(response, "Pragma"));
    }

    private static int getHeaderCount(Response response, String headerName) {
        headerName = headerName.toLowerCase();
        return (int) response.headers().asList().stream().map(Header::getName).map(String::toLowerCase)
                .filter(headerName::equals).count();
    }

    @ApplicationScoped
    public static class RedirectingAuthenticator implements HttpAuthenticationMechanism {

        @Override
        public Uni<SecurityIdentity> authenticate(RoutingContext context, IdentityProviderManager identityProviderManager) {
            return Uni.createFrom().failure(new AuthenticationRedirectException(FOUND, "https://quarkus.io/"));
        }

        @Override
        public Set<Class<? extends AuthenticationRequest>> getCredentialTypes() {
            return Set.of(BaseAuthenticationRequest.class);
        }

        @Override
        public Uni<ChallengeData> getChallenge(RoutingContext context) {
            return Uni.createFrom().item(new ChallengeData(FOUND, "header-name", "header-value"));
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
