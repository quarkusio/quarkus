package io.quarkus.resteasy.test.security;

import static org.hamcrest.Matchers.is;

import java.io.File;
import java.net.URL;

import jakarta.annotation.security.DenyAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.vertx.http.runtime.security.annotation.BasicAuthentication;
import io.quarkus.vertx.http.runtime.security.annotation.HttpAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.annotation.MTLSAuthentication;
import io.quarkus.vertx.http.security.AuthorizationPolicy;
import io.restassured.RestAssured;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;

@Certificates(baseDir = "target/certs", certificates = {
        @Certificate(name = "mtls-test", password = "password", formats = { Format.PKCS12 }, client = true)
})
public class MtlsBasicAnnotationBasedAuthMechSelectionTest {

    @TestHTTPResource(value = "/mtls", tls = true)
    URL mtlsUrl;

    @TestHTTPResource(value = "/basic", tls = true)
    URL basicUrl;

    @TestHTTPResource(value = "/basic-or-mtls", tls = true)
    URL basicOrMtlsUrl;

    @TestHTTPResource(value = "/class-level/mtls", tls = true)
    URL overrideClassLevelMtlsUrl;

    @TestHTTPResource(value = "/class-level/basic", tls = true)
    URL overrideClassLevelBasicUrl;

    @TestHTTPResource(value = "/class-level/custom", tls = true)
    URL overrideClassLevelCustomUrl;

    @TestHTTPResource(value = "/class-level/custom-repeated", tls = true)
    URL overrideClassLevelCustomRepeatedUrl;

    @TestHTTPResource(value = "/custom-policy", tls = true)
    URL customAuthorizationPolicyUrl;

    @TestHTTPResource(value = "/class-level-custom-policy", tls = true)
    URL classLevelCustomAuthorizationPolicyUrl;

    @TestHTTPResource(value = "/custom-repeated", tls = true)
    URL customRepeatedUrl;

    @TestHTTPResource(value = "/custom-repeated-deny-all", tls = true)
    URL customRepeatedDenyAllUrl;

    @TestHTTPResource(value = "/class-level/basic-or-mtls-or-custom-5", tls = true)
    URL classLevelBasicOrMtlsOrCustom5Url;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MethodLevelSecurityResource.class, CustomHeaderAuthenticateMechanism.class,
                            ClassLevelSecurityResource.class, CustomHttpSecurityPolicy.class,
                            ClassLevelPolicyResource.class)
                    .addClasses(TestIdentityProvider.class, TestTrustedIdentityProvider.class, TestIdentityController.class)
                    .addAsResource(new StringAsset("""
                            quarkus.http.ssl.certificate.key-store-file=target/certs/mtls-test-keystore.p12
                            quarkus.http.ssl.certificate.key-store-password=password
                            quarkus.http.ssl.certificate.trust-store-file=target/certs/mtls-test-server-truststore.p12
                            quarkus.http.ssl.certificate.trust-store-password=password
                            quarkus.http.ssl.client-auth=REQUEST
                            quarkus.http.auth.basic=true
                            quarkus.http.auth.proactive=false
                            """), "application.properties"));

    @BeforeAll
    public static void setup() {
        TestIdentityController.resetRoles()
                .add("admin", "admin", "admin");
    }

    @Test
    public void testOtherMechanismNotAllowed() {
        // MTLS select, don't allow anything else
        RestAssured.given()
                .trustStore(new File("target/certs/mtls-test-client-truststore.p12"), "password")
                .header("custom-auth", "ignored")
                .get(mtlsUrl).then().statusCode(401);
        // Basic selected, don't allow anything else
        RestAssured.given()
                .trustStore(new File("target/certs/mtls-test-client-truststore.p12"), "password")
                .header("custom-auth", "ignored")
                .get(basicUrl).then().statusCode(401);
        // Basic or Mutual TLS selected, don't allow anything else
        RestAssured.given()
                .trustStore(new File("target/certs/mtls-test-client-truststore.p12"), "password")
                .header("custom-auth", "ignored")
                .get(basicOrMtlsUrl).then().statusCode(401);
        RestAssured.given()
                .trustStore(new File("target/certs/mtls-test-client-truststore.p12"), "password")
                .header("custom-auth", "ignored")
                .get(classLevelBasicOrMtlsOrCustom5Url).then().statusCode(401);
        // only custom auth with postfix 5 must be allowed
        RestAssured.given()
                .header("custom-auth", "ignored")
                .header("custom-auth-postfix", "4")
                .trustStore(new File("target/certs/mtls-test-client-truststore.p12"), "password")
                .get(classLevelBasicOrMtlsOrCustom5Url).then().statusCode(401);
    }

    @Test
    public void testMethodLevelMutualTlsOrBasicAuthenticationEnforced() {
        // anonymous user must not be allowed as we used an annotation selecting authentication mechanism
        RestAssured.given()
                .trustStore(new File("target/certs/mtls-test-client-truststore.p12"), "password")
                .get(basicOrMtlsUrl).then().statusCode(401);
        // endpoint is annotated with @MTLSAuthentication, therefore mTLS must pass
        RestAssured.given()
                .keyStore(new File("target/certs/mtls-test-client-keystore.p12"), "password")
                .trustStore(new File("target/certs/mtls-test-client-truststore.p12"), "password")
                .get(basicOrMtlsUrl).then().statusCode(200).body(is("CN=localhost"));
        // endpoint is annotated with @BasicAuthentication, therefore basic must pass
        RestAssured.given()
                .auth()
                .preemptive()
                .basic("admin", "admin")
                .trustStore(new File("target/certs/mtls-test-client-truststore.p12"), "password")
                .get(basicOrMtlsUrl).then().statusCode(200).body(is("admin"));
    }

    @Test
    public void testClassLevelMutualTlsOrBasicAuthenticationEnforced() {
        // anonymous user must not be allowed as we used an annotation selecting authentication mechanism
        RestAssured.given()
                .trustStore(new File("target/certs/mtls-test-client-truststore.p12"), "password")
                .get(classLevelBasicOrMtlsOrCustom5Url).then().statusCode(401);
        // resource is annotated with @MTLSAuthentication, therefore mTLS must pass
        RestAssured.given()
                .keyStore(new File("target/certs/mtls-test-client-keystore.p12"), "password")
                .trustStore(new File("target/certs/mtls-test-client-truststore.p12"), "password")
                .get(classLevelBasicOrMtlsOrCustom5Url).then().statusCode(200)
                .body(is("CN=localhost"));
        // resource is annotated with @BasicAuthentication, therefore basic must pass
        RestAssured.given()
                .auth()
                .preemptive()
                .basic("admin", "admin")
                .trustStore(new File("target/certs/mtls-test-client-truststore.p12"), "password")
                .get(classLevelBasicOrMtlsOrCustom5Url).then().statusCode(200).body(is("admin"));
        // resource is annotated with @HttpAuthenticationMechanism("custom-head-mech-5"), therefore it must pass
        RestAssured.given()
                .header("custom-auth", "ignored")
                .header("custom-auth-postfix", "5")
                .trustStore(new File("target/certs/mtls-test-client-truststore.p12"), "password")
                .get(classLevelBasicOrMtlsOrCustom5Url).then().statusCode(200).body(is("donald"));
    }

    @Test
    public void testMutualTLSAuthenticationEnforced() {
        // endpoint is annotated with @MTLS, therefore mTLS must pass while anything less fail
        RestAssured.given()
                .trustStore(new File("target/certs/mtls-test-client-truststore.p12"), "password")
                .get(mtlsUrl).then().statusCode(401);
        RestAssured.given()
                .keyStore(new File("target/certs/mtls-test-client-keystore.p12"), "password")
                .trustStore(new File("target/certs/mtls-test-client-truststore.p12"), "password")
                .get(mtlsUrl).then().statusCode(200).body(is("CN=localhost"));
        RestAssured.given()
                .auth()
                .preemptive()
                .basic("admin", "admin")
                .trustStore(new File("target/certs/mtls-test-client-truststore.p12"), "password")
                .get(mtlsUrl).then().statusCode(401);
        // same expectations for the method-level annotation overriding the class-level annotation
        RestAssured.given()
                .trustStore(new File("target/certs/mtls-test-client-truststore.p12"), "password")
                .get(overrideClassLevelMtlsUrl).then().statusCode(401);
        RestAssured.given()
                .keyStore(new File("target/certs/mtls-test-client-keystore.p12"), "password")
                .trustStore(new File("target/certs/mtls-test-client-truststore.p12"), "password")
                .get(overrideClassLevelMtlsUrl).then().statusCode(200)
                .body(is("CN=localhost"));
        RestAssured.given()
                .auth()
                .preemptive()
                .basic("admin", "admin")
                .trustStore(new File("target/certs/mtls-test-client-truststore.p12"), "password")
                .get(overrideClassLevelMtlsUrl).then().statusCode(401);
    }

    @Test
    public void testBasicAuthenticationEnforced() {
        // endpoint is annotated with @Basic, therefore basic auth must pass while anything less fail
        RestAssured.given()
                .trustStore(new File("target/certs/mtls-test-client-truststore.p12"), "password")
                .get(basicUrl).then().statusCode(401);
        RestAssured.given()
                .auth()
                .preemptive()
                .basic("admin", "admin")
                .trustStore(new File("target/certs/mtls-test-client-truststore.p12"), "password")
                .get(basicUrl).then().statusCode(200).body(is("admin"));
        RestAssured.given()
                .keyStore(new File("target/certs/mtls-test-client-keystore.p12"), "password")
                .trustStore(new File("target/certs/mtls-test-client-truststore.p12"), "password")
                .get(basicUrl).then().statusCode(401);
        // same expectations for the method-level annotation overriding the class-level annotation
        RestAssured.given()
                .trustStore(new File("target/certs/mtls-test-client-truststore.p12"), "password")
                .get(overrideClassLevelBasicUrl).then().statusCode(401);
        RestAssured.given()
                .auth()
                .preemptive()
                .basic("admin", "admin")
                .trustStore(new File("target/certs/mtls-test-client-truststore.p12"), "password")
                .get(overrideClassLevelBasicUrl).then().statusCode(200).body(is("admin"));
        RestAssured.given()
                .keyStore(new File("target/certs/mtls-test-client-keystore.p12"), "password")
                .trustStore(new File("target/certs/mtls-test-client-truststore.p12"), "password")
                .get(overrideClassLevelBasicUrl).then().statusCode(401);
    }

    @Test
    public void testCustomAuthenticationMechanismEnforced() {
        // anonymous not allowed
        RestAssured.given()
                .trustStore(new File("target/certs/mtls-test-client-truststore.p12"), "password")
                .get(overrideClassLevelCustomUrl).then().statusCode(401);
        // basic not allowed
        RestAssured.given()
                .auth()
                .preemptive()
                .basic("admin", "admin")
                .trustStore(new File("target/certs/mtls-test-client-truststore.p12"), "password")
                .get(overrideClassLevelCustomUrl).then().statusCode(401);
        // mTLS not allowed
        RestAssured.given()
                .keyStore(new File("target/certs/mtls-test-client-keystore.p12"), "password")
                .trustStore(new File("target/certs/mtls-test-client-truststore.p12"), "password")
                .get(overrideClassLevelCustomUrl).then().statusCode(401);
        // custom allowed
        RestAssured.given()
                .header("custom-auth", "ignored")
                .trustStore(new File("target/certs/mtls-test-client-truststore.p12"), "password")
                .get(overrideClassLevelCustomUrl).then().statusCode(200).body(is("donald"));
    }

    @Test
    public void testRepeatedCustomAuthenticationMechanismEnforced() {
        URL[] urls = { overrideClassLevelCustomRepeatedUrl, customRepeatedUrl };
        for (URL url : urls) {
            // anonymous not allowed
            RestAssured.given()
                    .trustStore(new File("target/certs/mtls-test-client-truststore.p12"), "password")
                    .get(url).then().statusCode(401);
            // basic not allowed
            RestAssured.given()
                    .auth()
                    .preemptive()
                    .basic("admin", "admin")
                    .trustStore(new File("target/certs/mtls-test-client-truststore.p12"), "password")
                    .get(url).then().statusCode(401);
            // mTLS not allowed
            RestAssured.given()
                    .keyStore(new File("target/certs/mtls-test-client-keystore.p12"), "password")
                    .trustStore(new File("target/certs/mtls-test-client-truststore.p12"), "password")
                    .get(url).then().statusCode(401);
            // 'custom-head-mech' not allowed
            RestAssured.given()
                    .header("custom-auth", "ignored")
                    .trustStore(new File("target/certs/mtls-test-client-truststore.p12"), "password")
                    .get(url).then().statusCode(401);
            // 'custom-head-mech-1' allowed
            RestAssured.given()
                    .header("custom-auth", "ignored")
                    .header("custom-auth-postfix", "1")
                    .trustStore(new File("target/certs/mtls-test-client-truststore.p12"), "password")
                    .get(url).then().statusCode(200).body(is("donald"));
            // 'custom-head-mech-2' allowed
            RestAssured.given()
                    .header("custom-auth", "ignored")
                    .header("custom-auth-postfix", "2")
                    .trustStore(new File("target/certs/mtls-test-client-truststore.p12"), "password")
                    .get(url).then().statusCode(200).body(is("donald"));
            // 'custom-head-mech-3' not allowed
            RestAssured.given()
                    .header("custom-auth", "ignored")
                    .header("custom-auth-postfix", "3")
                    .trustStore(new File("target/certs/mtls-test-client-truststore.p12"), "password")
                    .get(url).then().statusCode(401);
            // 'custom-head-mech-4' allowed
            RestAssured.given()
                    .header("custom-auth", "ignored")
                    .header("custom-auth-postfix", "4")
                    .trustStore(new File("target/certs/mtls-test-client-truststore.p12"), "password")
                    .get(url).then().statusCode(200).body(is("donald"));
        }
    }

    @Test
    public void testDenyAllEnforced() {
        // the endpoint is annotated with
        // @HttpAuthenticationMechanism("custom-head-mech-1")
        // @HttpAuthenticationMechanism("custom-head-mech-2")
        // @HttpAuthenticationMechanism("custom-head-mech-4")
        // @DenyAll
        // therefore we check that the endpoint is denied rather than authenticated only
        RestAssured.given()
                .header("custom-auth", "ignored")
                .header("custom-auth-postfix", "2")
                .trustStore(new File("target/certs/mtls-test-client-truststore.p12"), "password")
                .get(customRepeatedDenyAllUrl).then().statusCode(403);
    }

    @Test
    public void testMethodLevelCustomAuthorizationPolicyWithMechanismSelection() {
        RestAssured.given()
                .header("custom-auth", "ignored")
                .header("auth-required", "ignored")
                .trustStore(new File("target/certs/mtls-test-client-truststore.p12"), "password")
                .get(customAuthorizationPolicyUrl).then().statusCode(200).body(is("donald"));
        RestAssured.given()
                .header("custom-auth", "ignored")
                .header("deny-access", "ignored")
                .trustStore(new File("target/certs/mtls-test-client-truststore.p12"), "password")
                .get(customAuthorizationPolicyUrl).then().statusCode(403);
        RestAssured.given()
                .auth()
                .preemptive()
                .basic("admin", "admin")
                .header("auth-required", "ignored")
                .trustStore(new File("target/certs/mtls-test-client-truststore.p12"), "password")
                .get(customAuthorizationPolicyUrl).then().statusCode(401);
    }

    @Test
    public void testClassLevelCustomAuthorizationPolicyWithMechanismSelection() {
        // this resource is annotated with
        // @MTLSAuthentication
        // @HttpAuthenticationMechanism("custom-head-mech")
        // @AuthorizationPolicy(name = "custom")
        // therefore expect that mTLS and custom auth are allowed, while basic is not
        // and that the custom policy is applied
        RestAssured.given()
                .header("custom-auth", "ignored")
                .header("auth-required", "ignored")
                .trustStore(new File("target/certs/mtls-test-client-truststore.p12"), "password")
                .get(classLevelCustomAuthorizationPolicyUrl).then().statusCode(200).body(is("donald"));
        RestAssured.given()
                .keyStore(new File("target/certs/mtls-test-client-keystore.p12"), "password")
                .trustStore(new File("target/certs/mtls-test-client-truststore.p12"), "password")
                .header("auth-required", "ignored")
                .get(classLevelCustomAuthorizationPolicyUrl).then().statusCode(200)
                .body(is("CN=localhost"));
        RestAssured.given()
                .header("custom-auth", "ignored")
                .header("deny-access", "ignored")
                .trustStore(new File("target/certs/mtls-test-client-truststore.p12"), "password")
                .get(classLevelCustomAuthorizationPolicyUrl).then().statusCode(403);
        RestAssured.given()
                .auth()
                .preemptive()
                .basic("admin", "admin")
                .header("auth-required", "ignored")
                .trustStore(new File("target/certs/mtls-test-client-truststore.p12"), "password")
                .get(classLevelCustomAuthorizationPolicyUrl).then().statusCode(401);
    }

    @Path("/")
    public static class MethodLevelSecurityResource {

        @Inject
        SecurityIdentity identity;

        @MTLSAuthentication
        @Path("mtls")
        @GET
        public String mtls() {
            return identity.getPrincipal().getName();
        }

        @BasicAuthentication
        @Path("basic")
        @GET
        public String basic() {
            return identity.getPrincipal().getName();
        }

        @MTLSAuthentication
        @BasicAuthentication
        @Path("basic-or-mtls")
        @GET
        public String basicOrMtls() {
            return identity.getPrincipal().getName();
        }

        @HttpAuthenticationMechanism("custom-head-mech-1")
        @HttpAuthenticationMechanism("custom-head-mech-2")
        @HttpAuthenticationMechanism("custom-head-mech-4")
        @Path("custom-repeated")
        @GET
        public String customRepeated() {
            return identity.getPrincipal().getName();
        }

        @HttpAuthenticationMechanism("custom-head-mech-1")
        @HttpAuthenticationMechanism("custom-head-mech-2")
        @HttpAuthenticationMechanism("custom-head-mech-4")
        @DenyAll
        @Path("custom-repeated-deny-all")
        @GET
        public String customRepeatedDenyAll() {
            return identity.getPrincipal().getName();
        }

        @HttpAuthenticationMechanism("custom-head-mech")
        @AuthorizationPolicy(name = "custom")
        @Path("custom-policy")
        @GET
        public String customAuthorizationPolicy() {
            return identity.getPrincipal().getName();
        }
    }

    @MTLSAuthentication
    @HttpAuthenticationMechanism("custom-head-mech")
    @AuthorizationPolicy(name = "custom")
    @Path("class-level-custom-policy")
    public static class ClassLevelPolicyResource {

        @Inject
        SecurityIdentity identity;

        @GET
        public String customAuthorizationPolicy() {
            return identity.getPrincipal().getName();
        }
    }

    @HttpAuthenticationMechanism("custom-head-mech-5")
    @MTLSAuthentication
    @BasicAuthentication
    @Path("/class-level")
    public static class ClassLevelSecurityResource {

        @Inject
        SecurityIdentity identity;

        @MTLSAuthentication
        @Path("mtls")
        @GET
        public String mtls() {
            return identity.getPrincipal().getName();
        }

        @BasicAuthentication
        @Path("basic")
        @GET
        public String basic() {
            return identity.getPrincipal().getName();
        }

        @HttpAuthenticationMechanism("custom-head-mech")
        @Path("custom")
        @GET
        public String custom() {
            return identity.getPrincipal().getName();
        }

        @HttpAuthenticationMechanism("custom-head-mech-1")
        @HttpAuthenticationMechanism("custom-head-mech-2")
        @HttpAuthenticationMechanism("custom-head-mech-4")
        @Path("custom-repeated")
        @GET
        public String customRepeated() {
            return identity.getPrincipal().getName();
        }

        @Path("basic-or-mtls-or-custom-5")
        @GET
        public String basicOrMtlsOrCustom5() {
            return identity.getPrincipal().getName();
        }
    }
}
