package io.quarkus.rest.client.reactive.tls;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.netty.handler.ssl.OpenSsl;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;

/**
 * Verifies that PQC key-exchange-groups configured via the TLS registry are propagated to
 * the REST client's HttpClientOptions (via ClientBuilderImpl). The server is configured with
 * strict PQC enforcement, so a successful connection proves the groups were propagated.
 */
@Certificates(baseDir = "target/certs", certificates = @Certificate(name = "tls-pqc-rc-test", password = "secret", formats = {
        Format.PEM }))
@EnabledIf("isOpenSsl35Available")
public class PqcKeyExchangeTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Client.class, DefaultClient.class, Resource.class)
                    .addAsResource(new File("target/certs/tls-pqc-rc-test.key"), "server-key.pem")
                    .addAsResource(new File("target/certs/tls-pqc-rc-test.crt"), "server-cert.pem")
                    .addAsResource(new File("target/certs/tls-pqc-rc-test-ca.crt"), "server-ca.pem"))
            // Server: strict PQC, only accepts connections that negotiate X25519MLKEM768
            .overrideConfigKey("quarkus.tls.key-store.pem.0.cert", "server-cert.pem")
            .overrideConfigKey("quarkus.tls.key-store.pem.0.key", "server-key.pem")
            .overrideConfigKey("quarkus.tls.pqc-enforcement-policy", "strict")
            .overrideConfigKey("quarkus.tls.key-exchange-groups", "x25519mlkem768")
            // PQC REST client: named TLS bucket with trust store and matching PQC config.
            // pqc-enforcement-policy=client-negotiated is required to trigger Vert.x's
            // OpenSSL auto-selection (RELAXED uses JDK SSL, which cannot negotiate PQC groups).
            .overrideConfigKey("quarkus.tls.rest-client.trust-store.pem.certs", "server-ca.pem")
            .overrideConfigKey("quarkus.tls.rest-client.key-exchange-groups", "x25519mlkem768")
            .overrideConfigKey("quarkus.tls.rest-client.pqc-enforcement-policy", "client-negotiated")
            .overrideConfigKey("quarkus.rest-client.rc.url", "https://localhost:${quarkus.http.test-ssl-port:8444}")
            .overrideConfigKey("quarkus.rest-client.rc.tls-configuration-name", "rest-client")
            // Default REST client: trust store only, no PQC groups configured
            .overrideConfigKey("quarkus.tls.no-pqc.trust-store.pem.certs", "server-ca.pem")
            .overrideConfigKey("quarkus.rest-client.rc-no-pqc.url",
                    "https://localhost:${quarkus.http.test-ssl-port:8444}")
            .overrideConfigKey("quarkus.rest-client.rc-no-pqc.tls-configuration-name", "no-pqc");

    @RestClient
    Client client;

    @RestClient
    DefaultClient defaultClient;

    @Test
    void restClientConnectsToStrictPqcServer() {
        // A non-PQC client would fail the STRICT server handshake.
        // Success proves key-exchange-groups propagated from TLS registry to HttpClientOptions.
        assertThat(client.ping()).isEqualTo("pong");
    }

    @Test
    void restClientWithoutPqcConfigFailsToConnectToStrictServer() {
        // A REST client with no PQC groups (JDK SSL engine, classical groups only) cannot
        // negotiate a PQC group with the strict server — the handshake fails.
        assertThatThrownBy(() -> defaultClient.ping())
                .isNotNull();
    }

    static boolean isOpenSsl35Available() {
        return OpenSsl.isAvailable() && OpenSsl.version() >= 0x30500000L;
    }

    @Path("/pqc-ping")
    @RegisterRestClient(configKey = "rc")
    public interface Client {
        @GET
        String ping();
    }

    @Path("/pqc-ping")
    @RegisterRestClient(configKey = "rc-no-pqc")
    public interface DefaultClient {
        @GET
        String ping();
    }

    @Path("/pqc-ping")
    public static class Resource {
        @GET
        public String ping() {
            return "pong";
        }
    }
}
