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

public class MtlsBasicAnnotationBasedAuthMechSelectionTest {

    @TestHTTPResource(value = "/mtls", ssl = true)
    URL mtlsUrl;

    @TestHTTPResource(value = "/basic", ssl = true)
    URL basicUrl;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MtlsResource.class)
                    .addClasses(TestIdentityProvider.class, TestTrustedIdentityProvider.class, TestIdentityController.class)
                    .addAsResource("mtls/mtls-basic-jks.conf", "application.properties")
                    .addAsResource("mtls/server-keystore.jks", "server-keystore.jks")
                    .addAsResource("mtls/server-truststore.jks", "server-truststore.jks"));

    @BeforeAll
    public static void setup() {
        TestIdentityController.resetRoles()
                .add("admin", "admin", "admin");
    }

    @Test
    public void testMutualTLSAuthenticationEnforced() {
        // endpoint is annotated with @MTLS, therefore mTLS must pass while anything less fail
        RestAssured.given()
                .trustStore(new File("src/test/resources/mtls/client-truststore.jks"), "password")
                .get(mtlsUrl).then().statusCode(401);
        RestAssured.given()
                .keyStore(new File("src/test/resources/mtls/client-keystore.jks"), "password")
                .trustStore(new File("src/test/resources/mtls/client-truststore.jks"), "password")
                .get(mtlsUrl).then().statusCode(200).body(is("CN=client,OU=cert,O=quarkus,L=city,ST=state,C=AU"));
        RestAssured.given()
                .auth()
                .preemptive()
                .basic("admin", "admin")
                .trustStore(new File("src/test/resources/mtls/client-truststore.jks"), "password")
                .get(mtlsUrl).then().statusCode(401);
    }

    @Test
    public void testBasicAuthenticationEnforced() {
        // endpoint is annotated with @Basic, therefore basic auth must pass while anything less fail
        RestAssured.given()
                .trustStore(new File("src/test/resources/mtls/client-truststore.jks"), "password")
                .get(basicUrl).then().statusCode(401);
        RestAssured.given()
                .auth()
                .preemptive()
                .basic("admin", "admin")
                .trustStore(new File("src/test/resources/mtls/client-truststore.jks"), "password")
                .get(basicUrl).then().statusCode(200).body(is("admin"));
        RestAssured.given()
                .keyStore(new File("src/test/resources/mtls/client-keystore.jks"), "password")
                .trustStore(new File("src/test/resources/mtls/client-truststore.jks"), "password")
                .get(basicUrl).then().statusCode(401);
    }

    @Path("/")
    public static class MtlsResource {

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

    }
}
