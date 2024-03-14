package io.quarkus.resteasy.reactive.server.test.security;

import static org.jboss.resteasy.reactive.RestResponse.StatusCode.FOUND;

import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
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
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

public class AuthenticationRedirectExceptionMapperTest {

    private static final int EXPECTED_STATUS = 409;
    private static final String APP_PROPS = "" +
            "quarkus.http.auth.proactive=false\n" +
            "quarkus.http.auth.permission.default.paths=/*\n" +
            "quarkus.http.auth.permission.default.policy=authenticated";

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset(APP_PROPS), "application.properties"));

    @Test
    public void testAuthenticationRedirectExceptionMapper() {
        RestAssured
                .given()
                .redirects()
                .follow(false)
                .when()
                .get("/secured-route")
                .then()
                .statusCode(EXPECTED_STATUS);
    }

    public static final class AuthenticationRedirectExceptionMapper {

        @ServerExceptionMapper(AuthenticationRedirectException.class)
        public Response authenticationRedirectException() {
            return Response.status(EXPECTED_STATUS).build();
        }
    }

    @ApplicationScoped
    public static class RedirectingAuthenticator implements HttpAuthenticationMechanism {

        @Override
        public Uni<SecurityIdentity> authenticate(RoutingContext context, IdentityProviderManager identityProviderManager) {
            throw new AuthenticationRedirectException(FOUND, "https://quarkus.io/");
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
