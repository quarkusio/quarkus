package io.quarkus.resteasy.test.security;

import static org.hamcrest.Matchers.is;

import java.io.File;
import java.net.URL;

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
import io.quarkus.vertx.http.runtime.security.annotation.MTLSAuthentication;
import io.restassured.RestAssured;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;

@Certificates(baseDir = "target/certs", certificates = {
        @Certificate(name = "mtls-test", password = "password", formats = { Format.PKCS12 }, client = true)
})
public class AnnotationBasedInclusiveAuthenticationTest {

    @TestHTTPResource(value = "/method-level", tls = true)
    URL methodLevelUrl;

    @TestHTTPResource(value = "/class-level", tls = true)
    URL classLevelUrl;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MethodLevelSecuredResource.class, CustomHeaderAuthenticateMechanism.class)
                    .addClass(ClassLevelSecuredResource.class)
                    .addClasses(TestIdentityProvider.class, TestTrustedIdentityProvider.class, TestIdentityController.class)
                    .addAsResource(new StringAsset("""
                            quarkus.http.ssl.certificate.key-store-file=target/certs/mtls-test-keystore.p12
                            quarkus.http.ssl.certificate.key-store-password=password
                            quarkus.http.ssl.certificate.trust-store-file=target/certs/mtls-test-server-truststore.p12
                            quarkus.http.ssl.certificate.trust-store-password=password
                            quarkus.http.ssl.client-auth=REQUEST
                            quarkus.http.auth.basic=true
                            quarkus.http.auth.proactive=false
                            quarkus.http.auth.inclusive=true
                            """), "application.properties"));

    @BeforeAll
    public static void setup() {
        TestIdentityController.resetRoles()
                .add("admin", "admin", "admin");
    }

    @Test
    public void testMethodLevelAuthMechanisms() {
        testUsingUrl(methodLevelUrl);
    }

    @Test
    public void testClassLevelAuthMechanisms() {
        testUsingUrl(classLevelUrl);
    }

    private static void testUsingUrl(URL url) {
        // custom auth not allowed
        RestAssured.given()
                .trustStore(new File("target/certs/mtls-test-client-truststore.p12"), "password")
                .header("custom-auth", "ignored")
                .get(url).then().statusCode(401);
        // only basic not allowed
        RestAssured.given()
                .auth()
                .preemptive()
                .basic("admin", "admin")
                .trustStore(new File("target/certs/mtls-test-client-truststore.p12"), "password")
                .get(url).then().statusCode(401);
        // only mTLS not allowed
        RestAssured.given()
                .keyStore(new File("target/certs/mtls-test-client-keystore.p12"), "password")
                .trustStore(new File("target/certs/mtls-test-client-truststore.p12"), "password")
                .get(url).then().statusCode(401);
        // basic and mTLS allowed
        RestAssured.given()
                .auth()
                .preemptive()
                .basic("admin", "admin")
                .keyStore(new File("target/certs/mtls-test-client-keystore.p12"), "password")
                .trustStore(new File("target/certs/mtls-test-client-truststore.p12"), "password")
                .get(url).then().statusCode(200).body(is("CN=localhost"));
    }

    @Path("method-level")
    public static class MethodLevelSecuredResource {

        @Inject
        SecurityIdentity identity;

        @BasicAuthentication
        @MTLSAuthentication
        @GET
        public String mTlsAndBasic() {
            return identity.getPrincipal().getName();
        }

    }

    @BasicAuthentication
    @MTLSAuthentication
    @Path("class-level")
    public static class ClassLevelSecuredResource {

        @Inject
        SecurityIdentity identity;

        @GET
        public String mTlsAndBasic() {
            return identity.getPrincipal().getName();
        }

    }
}
