package io.quarkus.it.http3.restclient;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;
import io.vertx.core.Vertx;
import io.vertx.core.http.Http3ClientConfig;
import io.vertx.core.http.HttpClientAgent;
import io.vertx.core.http.HttpClientConfig;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.net.ClientSSLOptions;
import io.vertx.core.net.PfxOptions;

@Certificates(baseDir = "target/certs", certificates = @Certificate(name = "http3-test", password = "secret", formats = {
        Format.PKCS12 }))
@QuarkusTest
class Http3RestClientTest {

    @TestHTTPResource(value = "/ping", tls = true)
    URL tlsUrl;

    static Vertx vertx;

    @BeforeAll
    static void setup() {
        vertx = Vertx.vertx();
    }

    @AfterAll
    static void tearDown() throws TimeoutException {
        vertx.close().await(10, TimeUnit.SECONDS);
    }

    record ResponseStruct(String version, String body, int status) {
    }

    @Test
    void testRestClientUsesHttp3() throws Exception {
        int port = tlsUrl.getPort();

        HttpClientConfig clientConfig = new HttpClientConfig();
        clientConfig.setVersions(HttpVersion.HTTP_2);
        clientConfig.setSsl(true);
        ClientSSLOptions sslOptions = new ClientSSLOptions()
                .setTrustOptions(new PfxOptions().setPath("target/certs/http3-test-truststore.p12").setPassword("secret"));
        HttpClientAgent client = vertx.httpClientBuilder().with(clientConfig).with(sslOptions).build();

        var response = client.request(HttpMethod.GET, port, "localhost", "/client/ping")
                .compose(HttpClientRequest::send)
                .compose(resp -> resp.body()
                        .map(body -> new ResponseStruct(resp.version().name(), body.toString(), resp.statusCode())))
                .await(10, TimeUnit.SECONDS);

        assertThat(response.status).isEqualTo(200);
        assertThat(response.body).isEqualTo("pong");

        client.close().await(5, TimeUnit.SECONDS);
    }

    @Test
    void testDirectHttp3() throws Exception {
        int port = tlsUrl.getPort();

        HttpClientConfig clientConfig = new HttpClientConfig();
        clientConfig.setVersions(HttpVersion.HTTP_3);
        clientConfig.setHttp3Config(new Http3ClientConfig());
        ClientSSLOptions sslOptions = new ClientSSLOptions()
                .setTrustOptions(new PfxOptions().setPath("target/certs/http3-test-truststore.p12").setPassword("secret"));
        HttpClientAgent client = vertx.httpClientBuilder().with(clientConfig).with(sslOptions).build();

        var response = client.request(HttpMethod.GET, port, "localhost", "/ping")
                .compose(HttpClientRequest::send)
                .compose(resp -> resp.body()
                        .map(body -> new ResponseStruct(resp.version().name(), body.toString(), resp.statusCode())))
                .await(10, TimeUnit.SECONDS);

        assertThat(response.version).isEqualTo("HTTP_3");
        assertThat(response.status).isEqualTo(200);
        assertThat(response.body).isEqualTo("pong");

        client.close().await(5, TimeUnit.SECONDS);
    }
}
