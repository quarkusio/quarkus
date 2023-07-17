package io.quarkus.vertx.http.security;

import java.util.Set;
import java.util.function.Supplier;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
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
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

public class DisabledProactiveSecIdentityProviderTest {

    private static final String APP_PROPS = "" +
            "quarkus.http.auth.basic=true\n" +
            "quarkus.http.auth.proactive=false\n" +
            "quarkus.http.auth.policy.r1.roles-allowed=admin\n" +
            "quarkus.http.auth.permission.roles1.paths=/admin\n" +
            "quarkus.http.auth.permission.roles1.policy=r1\n";

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest().setArchiveProducer(new Supplier<>() {
        @Override
        public JavaArchive get() {
            return ShrinkWrap.create(JavaArchive.class)
                    .addClasses(PathHandler.class, FailingAuthenticationMechanism.class, BasicIdentityProvider.class)
                    .addAsResource(new StringAsset(APP_PROPS), "application.properties");
        }
    });

    @Test
    public void testAuthenticationIsAttempted() {
        // path requires authentication
        RestAssured
                .given()
                .auth().preemptive().basic("admin", "admin")
                .redirects().follow(false)
                .when()
                .get("/admin")
                .then()
                .assertThat()
                .statusCode(401);
    }

    @Test
    public void testAuthenticationIsNotAttempted() {
        // path does not require authentication
        RestAssured
                .given()
                .auth().preemptive().basic("admin", "admin")
                .redirects().follow(false)
                .when()
                .get("/anonymous")
                .then()
                .assertThat()
                .statusCode(200);
    }

    @ApplicationScoped
    public static class FailingAuthenticationMechanism implements HttpAuthenticationMechanism {

        @Override
        public Uni<SecurityIdentity> authenticate(
                final RoutingContext context, final IdentityProviderManager identityProviderManager) {
            throw new AuthenticationFailedException();
        }

        @Override
        public Set<Class<? extends AuthenticationRequest>> getCredentialTypes() {
            return Set.of(BaseAuthenticationRequest.class);
        }

        @Override
        public Uni<ChallengeData> getChallenge(final RoutingContext context) {
            return Uni.createFrom().nullItem();
        }

        @Override
        public Uni<Boolean> sendChallenge(final RoutingContext context) {
            return Uni.createFrom().item(false);
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
