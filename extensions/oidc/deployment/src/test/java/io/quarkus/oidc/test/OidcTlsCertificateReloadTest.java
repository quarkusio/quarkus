package io.quarkus.oidc.test;

import static org.awaitility.Awaitility.await;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.Authenticated;
import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;

@Certificates(baseDir = "target/certs", certificates = {
        @Certificate(name = "oidc-reload-A", password = "password", formats = Format.PKCS12),
        @Certificate(name = "oidc-reload-B", password = "password", formats = Format.PKCS12),
})
class OidcTlsCertificateReloadTest {

    private static final File temp = new File("target/test-certificates-" + UUID.randomUUID());

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(ProtectedEndpoint.class, MockIntrospectionEndpoint.class))
            .withConfiguration("""
                    quarkus.keycloak.devservices.enabled=false
                    quarkus.http.auth.proactive=false
                    """)
            .withRuntimeConfiguration("""
                    quarkus.tls.key-store.p12.path=target/certs/oidc-reload-A-keystore.p12
                    quarkus.tls.key-store.p12.password=password
                    # configure keystore as reload is only intended for scenarios with keystore as well
                    quarkus.tls.oidc-provider.key-store.p12.path=target/certs/oidc-reload-A-keystore.p12
                    quarkus.tls.oidc-provider.key-store.p12.password=password
                    quarkus.tls.oidc-provider.trust-store.p12.path=%1$s/truststore.p12
                    quarkus.tls.oidc-provider.trust-store.p12.password=password
                    quarkus.tls.oidc-provider.reload-period=1s
                    quarkus.oidc.auth-server-url=https://localhost:8444
                    quarkus.oidc.discovery-enabled=false
                    quarkus.oidc.introspection-path=/introspect
                    quarkus.oidc.client-id=test-client
                    quarkus.oidc.credentials.secret=test-secret
                    quarkus.oidc.tls.tls-configuration-name=oidc-provider
                    quarkus.http.idle-timeout=1s
                    loc=%1$s
                    """.formatted(temp.getAbsolutePath()))
            .setBeforeAllCustomizer(() -> {
                try {
                    Files.createDirectories(temp.toPath());
                    Files.copy(new File("target/certs/oidc-reload-A-truststore.p12").toPath(),
                            new File(temp, "truststore.p12").toPath());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            })
            .setAfterAllCustomizer(() -> {
                try {
                    Files.deleteIfExists(new File(temp, "truststore.p12").toPath());
                    Files.deleteIfExists(temp.toPath());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

    @ConfigProperty(name = "loc")
    File certs; // we need to inject location as the test currently uses different classloader

    @Test
    void certReloadUpdatesOidcWebClientSslOptions() throws IOException {
        callProtectedEndpoint().statusCode(200);

        Files.copy(new File("target/certs/oidc-reload-B-truststore.p12").toPath(),
                new File(certs, "truststore.p12").toPath(), StandardCopyOption.REPLACE_EXISTING);

        await().atMost(15, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> callProtectedEndpoint().statusCode(401));
    }

    private static ValidatableResponse callProtectedEndpoint() {
        return RestAssured.given()
                .auth().oauth2("any-token")
                .get("/protected")
                .then();
    }

    @Path("/protected")
    @Authenticated
    public static class ProtectedEndpoint {

        @GET
        public String get() {
            return "OK";
        }
    }

    @Path("/introspect")
    public static class MockIntrospectionEndpoint {

        @POST
        @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
        @Produces(MediaType.APPLICATION_JSON)
        public String introspect() {
            return "{\"active\":true,\"sub\":\"test-user\",\"username\":\"test-user\"}";
        }
    }
}
