package io.quarkus.oidc.test;

import static io.quarkus.oidc.runtime.OidcUtils.DEFAULT_TENANT_ID;
import static io.quarkus.oidc.runtime.OidcUtils.TENANT_ID_ATTRIBUTE;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.oidc.AuthenticationContext;
import io.quarkus.oidc.BearerTokenAuthentication;
import io.quarkus.oidc.Tenant;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.jwt.build.Jwt;
import io.vertx.ext.web.RoutingContext;

/**
 * The compiler adds a synthetic bridge method when a class overrides a generic method with a more specific type,
 * or when a public class inherits a public method from a package-private class. The bridge method has copies of
 * the method-level annotations, but not of the class-level annotations.
 */
class AnnotationBasedSecurityOnBridgeMethodsTest {

    private static final String PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAlivFI8qB4D0y2jy0CfEqFyy46R0o7S8TKpsx5xbHKoU1VWg6QkQm+ntyIv1p4kE1sPEQO73+HY8+Bzs75XwRTYL1BmR1w8J5hmjVWjc6R2BTBGAYRPFRhor3kpM6ni2SPmNNhurEAHw7TaqszP5eUF/F9+KEBWkwVta+PZ37bwqSE4sCb1soZFrVz/UT/LF4tYpuVYt3YbqToZ3pZOZ9AX2o1GCG3xwOjkc4x0W7ezbQZdC9iftPxVHR8irOijJRRjcPDtA6vPKpzLl6CyYnsIYPd99ltwxTHjr3npfv/3Lw50bAkbT4HeLFxTx4flEoZLKO/g0bAoV2uqBhkA9xnQIDAQAB";
    private static final String DEFAULT_TENANT_ISSUER = "default-tenant-issuer";
    private static final String HR_TENANT_ISSUER = "hr-tenant-issuer";

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(GenericEndpoints.class, TenantOnClassResource.class, MechanismOnClassResource.class,
                            AuthenticationContextOnClassResource.class, GenericJaxRsEndpoint.class,
                            GenericJaxRsEndpointResource.class, GenericParameterEndpoint.class,
                            GenericParameterEndpointResource.class, PackagePrivateResource.class,
                            PublicSubclassResource.class, WithoutMechanismAnnotationResource.class)
                    // the test methods sign tokens with this key, and they run with the application class loader
                    .addAsResource("privateKey.pem"))
            .withConfiguration("""
                    quarkus.keycloak.devservices.enabled=false
                    quarkus.oidc.devservices.enabled=false
                    quarkus.http.auth.proactive=false
                    quarkus.http.auth.basic=true
                    quarkus.security.users.embedded.enabled=true
                    """)
            .withRuntimeConfiguration("""
                    quarkus.security.users.embedded.plain-text=true
                    quarkus.security.users.embedded.users.alice=alice
                    # the Keycloak test resource of other tests in this module sets the default tenant server URL
                    quarkus.oidc.auth-server-url=
                    quarkus.oidc.public-key=%1$s
                    quarkus.oidc.token.issuer=%2$s
                    quarkus.oidc.hr.public-key=%1$s
                    quarkus.oidc.hr.token.issuer=%3$s
                    """.formatted(PUBLIC_KEY, DEFAULT_TENANT_ISSUER, HR_TENANT_ISSUER));

    @Test
    void tenantOnClassAndMechanismOnMethod() {
        String path = "/tenant-on-class/mechanism-on-method";
        assertTenant(path, token(HR_TENANT_ISSUER), "hr");
        // class-level @Tenant("hr")
        assertUnauthorized(path, token(DEFAULT_TENANT_ISSUER));
        // method-level @BearerTokenAuthentication
        assertBasicAuthenticationRejected(path);
    }

    @Test
    void tenantOnClassAndAuthenticationContextOnMethod() {
        String path = "/tenant-on-class/authentication-context-on-method";
        assertTenant(path, tokenWithGoldAcr(HR_TENANT_ISSUER), "hr");
        // class-level @Tenant("hr")
        assertUnauthorized(path, tokenWithGoldAcr(DEFAULT_TENANT_ISSUER));
        // method-level @AuthenticationContext("gold")
        assertInsufficientAuthentication(path, token(HR_TENANT_ISSUER));
    }

    @Test
    void mechanismOnClassAndTenantOnMethod() {
        String path = "/mechanism-on-class/tenant-on-method";
        assertTenant(path, token(HR_TENANT_ISSUER), "hr");
        // class-level @BearerTokenAuthentication
        assertBasicAuthenticationRejected(path);
        // method-level @Tenant("hr")
        assertUnauthorized(path, token(DEFAULT_TENANT_ISSUER));
    }

    @Test
    void mechanismOnClassAndAuthenticationContextOnMethod() {
        String path = "/mechanism-on-class/authentication-context-on-method";
        assertTenant(path, tokenWithGoldAcr(DEFAULT_TENANT_ISSUER), DEFAULT_TENANT_ID);
        // class-level @BearerTokenAuthentication
        assertBasicAuthenticationRejected(path);
        // method-level @AuthenticationContext("gold")
        assertInsufficientAuthentication(path, token(DEFAULT_TENANT_ISSUER));
    }

    @Test
    void authenticationContextOnClassAndTenantOnMethod() {
        String path = "/authentication-context-on-class/tenant-on-method";
        assertTenant(path, tokenWithGoldAcr(HR_TENANT_ISSUER), "hr");
        // class-level @AuthenticationContext("gold")
        assertInsufficientAuthentication(path, token(HR_TENANT_ISSUER));
        // method-level @Tenant("hr")
        assertUnauthorized(path, tokenWithGoldAcr(DEFAULT_TENANT_ISSUER));
    }

    @Test
    void authenticationContextOnClassAndMechanismOnMethod() {
        String path = "/authentication-context-on-class/mechanism-on-method";
        assertTenant(path, tokenWithGoldAcr(DEFAULT_TENANT_ISSUER), DEFAULT_TENANT_ID);
        // class-level @AuthenticationContext("gold")
        assertInsufficientAuthentication(path, token(DEFAULT_TENANT_ISSUER));
        // method-level @BearerTokenAuthentication
        assertBasicAuthenticationRejected(path);
    }

    @Test
    void jaxRsAnnotationsOnGenericInterface() {
        String path = "/jax-rs-annotations-on-interface";
        assertTenant(path, token(HR_TENANT_ISSUER), "hr");
        // class-level @Tenant("hr")
        assertUnauthorized(path, token(DEFAULT_TENANT_ISSUER));
        // method-level @BearerTokenAuthentication
        assertBasicAuthenticationRejected(path);
    }

    @Test
    void bridgeMethodWithDifferentParameterTypes() {
        String path = "/parameter-bridge/1";
        assertTenant(path, token(HR_TENANT_ISSUER), "hr");
        // class-level @Tenant("hr")
        assertUnauthorized(path, token(DEFAULT_TENANT_ISSUER));
        // method-level @BearerTokenAuthentication
        assertBasicAuthenticationRejected(path);
    }

    @Test
    void bridgeMethodOfMethodInheritedFromPackagePrivateClass() {
        String path = "/public-subclass";
        assertTenant(path, token(HR_TENANT_ISSUER), "hr");
        // method-level @Tenant("hr")
        assertUnauthorized(path, token(DEFAULT_TENANT_ISSUER));
        // method-level @BearerTokenAuthentication
        assertBasicAuthenticationRejected(path);
    }

    private static void assertTenant(String path, String token, String expectedTenantId) {
        given().auth().oauth2(token).get(path).then().statusCode(200).body(is(expectedTenantId));
    }

    private static void assertUnauthorized(String path, String token) {
        given().auth().oauth2(token).get(path).then().statusCode(401);
    }

    private static void assertInsufficientAuthentication(String path, String token) {
        given().auth().oauth2(token).get(path).then().statusCode(401)
                .header("www-authenticate", containsString("insufficient_user_authentication"));
    }

    private static void assertBasicAuthenticationRejected(String path) {
        // the credentials are valid, so only the @BearerTokenAuthentication annotation can reject them at the path
        given().auth().preemptive().basic("alice", "alice").get("/without-mechanism-annotation")
                .then().statusCode(200).body(is("alice"));
        // preemptive, because RestAssured otherwise only sends the credentials after a basic authentication challenge
        given().auth().preemptive().basic("alice", "alice").get(path).then().statusCode(401);
    }

    private static String token(String issuer) {
        return Jwt.preferredUserName("alice").issuer(issuer).sign("/privateKey.pem");
    }

    private static String tokenWithGoldAcr(String issuer) {
        return Jwt.preferredUserName("alice").issuer(issuer).claim("acr", "gold").sign("/privateKey.pem");
    }

    /**
     * Implementations return {@code String}, therefore the compiler adds bridge methods {@code Object first()}
     * and {@code Object second()} to them. Each bridge method has the same name and parameters as the endpoint.
     */
    interface GenericEndpoints<T> {

        T first();

        T second();
    }

    @Tenant("hr")
    @Path("/tenant-on-class")
    public static class TenantOnClassResource implements GenericEndpoints<String> {

        @Inject
        RoutingContext routingContext;

        @BearerTokenAuthentication
        @GET
        @Path("/mechanism-on-method")
        @Override
        public String first() {
            return routingContext.get(TENANT_ID_ATTRIBUTE);
        }

        @AuthenticationContext("gold")
        @GET
        @Path("/authentication-context-on-method")
        @Override
        public String second() {
            return routingContext.get(TENANT_ID_ATTRIBUTE);
        }
    }

    @BearerTokenAuthentication
    @Path("/mechanism-on-class")
    public static class MechanismOnClassResource implements GenericEndpoints<String> {

        @Inject
        RoutingContext routingContext;

        @Tenant("hr")
        @GET
        @Path("/tenant-on-method")
        @Override
        public String first() {
            return routingContext.get(TENANT_ID_ATTRIBUTE);
        }

        @AuthenticationContext("gold")
        @GET
        @Path("/authentication-context-on-method")
        @Override
        public String second() {
            return routingContext.get(TENANT_ID_ATTRIBUTE);
        }
    }

    @AuthenticationContext("gold")
    @Path("/authentication-context-on-class")
    public static class AuthenticationContextOnClassResource implements GenericEndpoints<String> {

        @Inject
        RoutingContext routingContext;

        @Tenant("hr")
        @GET
        @Path("/tenant-on-method")
        @Override
        public String first() {
            return routingContext.get(TENANT_ID_ATTRIBUTE);
        }

        @BearerTokenAuthentication
        @GET
        @Path("/mechanism-on-method")
        @Override
        public String second() {
            return routingContext.get(TENANT_ID_ATTRIBUTE);
        }
    }

    /**
     * This interface must be public, because RESTEasy Classic invokes its method.
     */
    @Path("/jax-rs-annotations-on-interface")
    public interface GenericJaxRsEndpoint<T> {

        @GET
        T current();
    }

    /**
     * The compiler adds a bridge method {@code Object current()} to this class.
     */
    @Tenant("hr")
    public static class GenericJaxRsEndpointResource implements GenericJaxRsEndpoint<String> {

        @Inject
        RoutingContext routingContext;

        @BearerTokenAuthentication
        @Override
        public String current() {
            return routingContext.get(TENANT_ID_ATTRIBUTE);
        }
    }

    interface GenericParameterEndpoint<T> {

        String find(T id);
    }

    /**
     * The compiler adds a bridge method {@code String find(Object)} to this class.
     * The bridge method has different parameter types than the endpoint.
     */
    @Tenant("hr")
    @Path("/parameter-bridge")
    public static class GenericParameterEndpointResource implements GenericParameterEndpoint<String> {

        @Inject
        RoutingContext routingContext;

        @BearerTokenAuthentication
        @GET
        @Path("/{id}")
        @Override
        public String find(@PathParam("id") String id) {
            return routingContext.get(TENANT_ID_ATTRIBUTE);
        }
    }

    static class PackagePrivateResource {

        @Inject
        RoutingContext routingContext;

        @Tenant("hr")
        @BearerTokenAuthentication
        @GET
        public String inherited() {
            return routingContext.get(TENANT_ID_ATTRIBUTE);
        }
    }

    /**
     * This class must be public, because the compiler only adds the bridge method {@code String inherited()}
     * when a public class inherits a public method from a package-private class.
     * RESTEasy Classic invokes the bridge method.
     */
    @Path("/public-subclass")
    public static class PublicSubclassResource extends PackagePrivateResource {
    }

    /**
     * Has the same annotations as the other endpoints, except the {@code @BearerTokenAuthentication} annotation.
     */
    @Tenant("hr")
    @AuthenticationContext("gold")
    @Path("/without-mechanism-annotation")
    public static class WithoutMechanismAnnotationResource {

        @Inject
        SecurityIdentity identity;

        @GET
        public String principalName() {
            return identity.getPrincipal().getName();
        }
    }
}
