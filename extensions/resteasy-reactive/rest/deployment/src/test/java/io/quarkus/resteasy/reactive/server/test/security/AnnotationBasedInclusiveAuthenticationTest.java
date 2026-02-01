package io.quarkus.resteasy.reactive.server.test.security;

import static org.hamcrest.Matchers.is;

import java.io.File;
import java.net.URL;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

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
                    .addAsResource("mtls/mtls-basic-jks.conf", "application.properties")
                    .addAsResource("mtls/server-keystore.jks", "server-keystore.jks")
                    .addAsResource("mtls/server-truststore.jks", "server-truststore.jks"))
            .overrideConfigKey("quarkus.http.auth.inclusive", "true");

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
                .trustStore(new File("src/test/resources/mtls/client-truststore.jks"), "password")
                .header("custom-auth", "ignored")
                .get(url).then().statusCode(401);
        // only basic not allowed
        RestAssured.given()
                .auth()
                .preemptive()
                .basic("admin", "admin")
                .trustStore(new File("src/test/resources/mtls/client-truststore.jks"), "password")
                .get(url).then().statusCode(401);
        // only mTLS not allowed
        RestAssured.given()
                .keyStore(new File("src/test/resources/mtls/client-keystore.jks"), "password")
                .trustStore(new File("src/test/resources/mtls/client-truststore.jks"), "password")
                .get(url).then().statusCode(401);
        // basic and mTLS allowed
        RestAssured.given()
                .auth()
                .preemptive()
                .basic("admin", "admin")
                .keyStore(new File("src/test/resources/mtls/client-keystore.jks"), "password")
                .trustStore(new File("src/test/resources/mtls/client-truststore.jks"), "password")
                .get(url).then().statusCode(200).body(is("CN=client,OU=cert,O=quarkus,L=city,ST=state,C=AU"));
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
