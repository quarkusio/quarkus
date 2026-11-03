package io.quarkus.it.tls.letsencrypt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.net.PemTrustOptions;

@QuarkusTest
@QuarkusTestResource(value = PebbleResource.class, restrictToAnnotatedClass = true)
public class LetsEncryptClientTest {

    private static final int HTTPS_PORT = 8444;

    private static Vertx vertx;

    @BeforeAll
    static void startVertx() {
        vertx = Vertx.vertx();
    }

    @AfterAll
    static void stopVertx() throws Exception {
        vertx.close().await(10, TimeUnit.SECONDS);
    }

    @Test
    void ordersAndServesTheCertificateOfTheAcmeServer() throws Exception {
        X509Certificate root = pebbleRoot();

        await().atMost(Duration.ofMinutes(2)).untilAsserted(() -> {
            List<X509Certificate> chain = serverChain();
            assertThat(chain).hasSizeGreaterThanOrEqualTo(2);
            chain.get(0).verify(chain.get(1).getPublicKey());
            chain.get(1).verify(root.getPublicKey());
            assertThat(chain.get(0).getSubjectAlternativeNames())
                    .extracting(name -> (Object) name.get(1))
                    .containsExactly("localhost");
        });

        assertThat(PebbleResource.STORAGE.resolve("lets-encrypt.crt")).exists();
        assertThat(PebbleResource.STORAGE.resolve("lets-encrypt.key")).exists();
        assertThat(PebbleResource.STORAGE.resolve("account-key.pem")).exists();
    }

    private static List<X509Certificate> serverChain() throws Exception {
        HttpClient client = vertx.createHttpClient(new HttpClientOptions().setSsl(true).setTrustAll(true)
                .setVerifyHost(false));
        try {
            HttpClientResponse response = client
                    .request(new RequestOptions().setMethod(HttpMethod.GET).setHost("localhost").setPort(HTTPS_PORT)
                            .setURI("/hello"))
                    .compose(request -> request.send())
                    .await(10, TimeUnit.SECONDS);
            assertThat(response.statusCode()).isEqualTo(200);
            return response.request().connection().peerCertificates().stream()
                    .map(X509Certificate.class::cast)
                    .toList();
        } finally {
            client.close().await(10, TimeUnit.SECONDS);
        }
    }

    private static X509Certificate pebbleRoot() throws Exception {
        HttpClient client = vertx.createHttpClient(new HttpClientOptions().setSsl(true)
                .setTrustOptions(new PemTrustOptions().addCertPath(PebbleResource.PEBBLE_CA.toString())));
        try {
            Buffer body = client
                    .request(new RequestOptions().setMethod(HttpMethod.GET).setHost("localhost")
                            .setPort(PebbleResource.MANAGEMENT_PORT).setURI("/roots/0"))
                    .compose(request -> request.send())
                    .compose(HttpClientResponse::body)
                    .await(10, TimeUnit.SECONDS);
            return (X509Certificate) CertificateFactory.getInstance("X.509")
                    .generateCertificate(new ByteArrayInputStream(body.toString().getBytes(StandardCharsets.US_ASCII)));
        } finally {
            client.close().await(10, TimeUnit.SECONDS);
        }
    }
}
