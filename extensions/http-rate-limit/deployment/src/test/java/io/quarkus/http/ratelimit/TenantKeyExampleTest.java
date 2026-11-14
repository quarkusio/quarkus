package io.quarkus.http.ratelimit;

import static io.restassured.RestAssured.given;

import java.security.Permission;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.security.credential.Credential;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.common.annotation.Identifier;
import io.smallrye.mutiny.Uni;

public class TenantKeyExampleTest {
    private static final String RULE = "quarkus.http.rate-limit.rules.api.";

    @RegisterExtension
    static final QuarkusExtensionTest app = TestHttp.withUsers(new QuarkusExtensionTest())
            .withApplicationRoot(jar -> jar.addClasses(TestHttp.class, TestHttp.Resource.class, TenantKeyResolver.class,
                    TenantIdentity.class, TenantSecurityIdentity.class))
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-elytron-security-properties-file-deployment", Version.getVersion())))
            .overrideConfigKey("quarkus.security.users.embedded.users.carol", "carol")
            .overrideConfigKey("quarkus.security.users.embedded.roles.carol", "user")
            .overrideConfigKey(RULE + "paths", "/*")
            .overrideConfigKey(RULE + "phase", "post-authentication")
            .overrideConfigKey(RULE + "request-key", "custom:tenant")
            .overrideRuntimeConfigKey(RULE + "permits", "1")
            .overrideRuntimeConfigKey(RULE + "window", "1h");

    @Test
    void anonymousMissingAndBlankTenantsDoNotSkipTheRule() {
        given().get("/x").then().statusCode(503);
        given().auth().preemptive().basic("bob", "bob").get("/x").then().statusCode(503);
        given().auth().preemptive().basic("carol", "carol").get("/x").then().statusCode(503);
        given().auth().preemptive().basic("alice", "alice").get("/x").then().statusCode(200);
        given().auth().preemptive().basic("alice", "alice").get("/x").then().statusCode(429);
    }

    /** The key resolver from the guide; authentication supplies its tenant attribute. */
    @Singleton
    @Identifier("tenant")
    public static class TenantKeyResolver implements RateLimitKeyResolver {
        @Override
        public Uni<Optional<String>> resolve(RateLimitContext context) {
            Optional<String> tenant = context.identity()
                    .filter(identity -> !identity.isAnonymous())
                    .map(identity -> identity.<String> getAttribute("tenant-id"))
                    .filter(value -> !value.isBlank());
            if (tenant.isEmpty()) {
                return Uni.createFrom().failure(new IllegalStateException("Tenant identity is unavailable"));
            }
            return Uni.createFrom().item(Optional.of("tenant:" + tenant.get()));
        }
    }

    @Singleton
    public static class TenantIdentity implements SecurityIdentityAugmentor {
        @Override
        public Uni<SecurityIdentity> augment(SecurityIdentity identity, AuthenticationRequestContext context) {
            if (identity.isAnonymous()) {
                return Uni.createFrom().item(identity);
            }
            return Uni.createFrom().item(switch (identity.getPrincipal().getName()) {
                case "alice" -> new TenantSecurityIdentity(identity, "tenant-one");
                case "carol" -> new TenantSecurityIdentity(identity, " ");
                default -> identity;
            });
        }
    }

    /** Add a tenant attribute while retaining the identity supplied by authentication. */
    public record TenantSecurityIdentity(SecurityIdentity delegate, String tenant) implements SecurityIdentity {
        @Override
        public Principal getPrincipal() {
            return delegate.getPrincipal();
        }

        @Override
        public boolean isAnonymous() {
            return delegate.isAnonymous();
        }

        @Override
        public Set<String> getRoles() {
            return delegate.getRoles();
        }

        @Override
        public boolean hasRole(String role) {
            return delegate.hasRole(role);
        }

        @Override
        public Set<Permission> getPermissions() {
            return delegate.getPermissions();
        }

        @Override
        public <T extends Credential> T getCredential(Class<T> type) {
            return delegate.getCredential(type);
        }

        @Override
        public Set<Credential> getCredentials() {
            return delegate.getCredentials();
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getAttribute(String name) {
            return "tenant-id".equals(name) ? (T) tenant : delegate.getAttribute(name);
        }

        @Override
        public Map<String, Object> getAttributes() {
            Map<String, Object> attributes = new HashMap<>(delegate.getAttributes());
            attributes.put("tenant-id", tenant);
            return Map.copyOf(attributes);
        }

        @Override
        public Uni<Boolean> checkPermission(Permission permission) {
            return delegate.checkPermission(permission);
        }
    }
}
