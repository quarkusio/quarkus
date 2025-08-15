package io.quarkus.oidc.test;

import static org.hamcrest.Matchers.is;

import java.io.File;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.oidc.BearerTokenAuthentication;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.test.QuarkusDevModeTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.keycloak.server.KeycloakTestResourceLifecycleManager;
import io.quarkus.vertx.http.runtime.security.annotation.MTLSAuthentication;
import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;

/**
 * This test ensures OIDC runs before mTLS authentication mechanism when inclusive authentication is not enabled.
 */
@QuarkusTestResource(KeycloakTestResourceLifecycleManager.class)
@Certificates(baseDir = "target/certs", certificates = @Certificate(name = "mtls-test", password = "secret", formats = {
        Format.PKCS12, Format.PEM }, client = true))
public class OidcMtlsDisabledInclusiveAuthTest {

    private static final String BASE_URL = "https://localhost:8443/mtls-bearer/";
    private static final String CONFIGURATION = """
            # Disable Dev Services, we use a test resource manager
            quarkus.keycloak.devservices.enabled=false

            quarkus.tls.key-store.pem.0.cert=server.crt
            quarkus.tls.key-store.pem.0.key=server.key
            quarkus.tls.trust-store.pem.certs=ca.crt
            quarkus.http.ssl.client-auth=REQUIRED
            quarkus.http.insecure-requests=disabled
            quarkus.oidc.auth-server-url=${keycloak.url}/realms/quarkus
            quarkus.oidc.client-id=quarkus-service-app
            quarkus.oidc.credentials.secret=secret
            quarkus.http.auth.proactive=false
            """;

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MtlsBearerResource.class)
                    .addAsResource(new StringAsset(CONFIGURATION), "application.properties")
                    .addAsResource(new File("target/certs/mtls-test.key"), "server.key")
                    .addAsResource(new File("target/certs/mtls-test.crt"), "server.crt")
                    .addAsResource(new File("target/certs/mtls-test-server-ca.crt"), "ca.crt"));

    @Test
    public void testOidcHasHighestPriority() {
        givenWithCerts().get(BASE_URL + "only-mtls").then().statusCode(200).body(is("CN=localhost"));
        givenWithCerts().auth().oauth2(getAccessToken()).get(BASE_URL + "only-bearer").then().statusCode(200).body(is("alice"));
        // this needs to be OIDC because when inclusive auth is disabled, OIDC has higher priority
        givenWithCerts().auth().oauth2(getAccessToken()).get(BASE_URL + "both").then().statusCode(200).body(is("alice"));
        // OIDC must run first and thus authentication fails over invalid credentials
        givenWithCerts().auth().oauth2("invalid-token").get(BASE_URL + "both").then().statusCode(401);
        // mTLS authentication mechanism still runs when OIDC doesn't produce the identity
        givenWithCerts().get(BASE_URL + "both").then().statusCode(200).body(is("CN=localhost"));
    }

    private static RequestSpecification givenWithCerts() {
        return RestAssured.given()
                .keyStore("target/certs/mtls-test-client-keystore.p12", "secret")
                .trustStore("target/certs/mtls-test-client-truststore.p12", "secret");
    }

    private static String getAccessToken() {
        return KeycloakTestResourceLifecycleManager.getAccessToken("alice");
    }

    @Path("mtls-bearer")
    public static class MtlsBearerResource {

        @Inject
        SecurityIdentity securityIdentity;

        @GET
        @Authenticated
        @Path("both")
        public String both() {
            return securityIdentity.getPrincipal().getName();
        }

        @GET
        @MTLSAuthentication
        @Path("only-mtls")
        public String onlyMTLS() {
            return securityIdentity.getPrincipal().getName();
        }

        @GET
        @BearerTokenAuthentication
        @Path("only-bearer")
        public String onlyBearer() {
            return securityIdentity.getPrincipal().getName();
        }
    }
}
