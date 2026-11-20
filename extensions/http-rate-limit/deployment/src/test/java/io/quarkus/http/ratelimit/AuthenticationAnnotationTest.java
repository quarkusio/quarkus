package io.quarkus.http.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AuthenticationRequest;
import io.quarkus.security.identity.request.TrustedAuthenticationRequest;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.HttpCredentialTransport;
import io.quarkus.vertx.http.runtime.security.annotation.BasicAuthentication;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpResponse;

class AuthenticationAnnotationTest {

    private static final String AFTER = "quarkus.http.rate-limit.rules.after.";

    @RegisterExtension
    static final QuarkusExtensionTest config = TestHttp.ipRule(TestHttp.withUsers(new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(TestHttp.class, TestClient.class, AdminResource.class,
                    HeaderMechanism.class))),
            "before", "/before/*", 2, "1m")
            .overrideConfigKey("quarkus.vertx.event-loops-pool-size", "1")
            // required by the annotation
            .overrideConfigKey("quarkus.http.auth.proactive", "false")
            .overrideConfigKey(AFTER + "paths", "/after/*")
            .overrideConfigKey(AFTER + "phase", "post-authentication")
            .overrideConfigKey(AFTER + "request-key", "principal")
            .overrideRuntimeConfigKey(AFTER + "permits", "100")
            .overrideRuntimeConfigKey(AFTER + "window", "1m")
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-elytron-security-properties-file-deployment", Version.getVersion())));

    @Inject
    Vertx vertx;

    @TestHTTPResource
    URL url;

    TestClient http;

    @BeforeEach
    void createClient() {
        http = new TestClient(vertx, url);
    }

    @AfterEach
    void closeClient() {
        http.close();
    }

    @Test
    void preAuthenticationRuleLeavesTheSelectionToTheAnnotation() {
        // both mechanisms accept the request, and the annotation selects basic authentication
        HttpResponse<Buffer> served = http.get("/before/admin", aliceAndBob());
        assertThat(served.statusCode()).isEqualTo(200);
        assertThat(served.bodyAsString()).isEqualTo("alice");

        // the selected mechanism sends the challenge, and a request that fails authentication spends a permit too
        HttpResponse<Buffer> challenged = http.get("/before/admin");
        assertThat(challenged.statusCode()).isEqualTo(401);
        assertThat(challenged.getHeader("WWW-Authenticate")).startsWithIgnoringCase("basic");

        assertThat(http.get("/before/admin", aliceAndBob()).statusCode()).isEqualTo(429);
    }

    @Test
    void postAuthenticationRuleAuthenticatesBeforeTheAnnotationApplies() {
        // only basic authentication accepts the request, which is the mechanism that the annotation requires
        HttpResponse<Buffer> served = http.get("/after/admin", TestHttp.basic("alice", "alice"));
        assertThat(served.statusCode()).isEqualTo(200);
        assertThat(served.bodyAsString()).isEqualTo("alice");

        // the mechanism with the higher priority authenticates the request first, and the annotation does not accept it
        assertThat(http.get("/after/admin", aliceAndBob()).statusCode()).isEqualTo(401);
        assertThat(http.get("/after/admin", Map.of("X-User", "bob")).statusCode()).isEqualTo(401);
        assertThat(http.get("/after/admin").statusCode()).isEqualTo(401);
    }

    private static Map<String, String> aliceAndBob() {
        Map<String, String> headers = new HashMap<>(TestHttp.basic("alice", "alice"));
        headers.put("X-User", "bob");
        return headers;
    }

    @Path("/")
    public static class AdminResource {

        @Inject
        SecurityIdentity identity;

        @GET
        @Path("{phase}/admin")
        @BasicAuthentication
        public String user() {
            return identity.getPrincipal().getName();
        }
    }

    /**
     * Trusts the user named by the {@code X-User} header, and is tried before basic authentication.
     */
    @ApplicationScoped
    public static class HeaderMechanism implements HttpAuthenticationMechanism {

        @Override
        public Uni<SecurityIdentity> authenticate(RoutingContext context, IdentityProviderManager identityProviderManager) {
            String user = context.request().getHeader("X-User");
            if (user == null) {
                return Uni.createFrom().nullItem();
            }
            return identityProviderManager.authenticate(new TrustedAuthenticationRequest(user));
        }

        @Override
        public Uni<ChallengeData> getChallenge(RoutingContext context) {
            return Uni.createFrom().item(new ChallengeData(401, "WWW-Authenticate", "x-user"));
        }

        @Override
        public Set<Class<? extends AuthenticationRequest>> getCredentialTypes() {
            return Set.of(TrustedAuthenticationRequest.class);
        }

        @Override
        public Uni<HttpCredentialTransport> getCredentialTransport(RoutingContext context) {
            return Uni.createFrom()
                    .item(new HttpCredentialTransport(HttpCredentialTransport.Type.OTHER_HEADER, "X-User", "x-user"));
        }

        @Override
        public int getPriority() {
            return 5000;
        }
    }
}
