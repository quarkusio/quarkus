package io.quarkus.it.http3.autotls;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.vertx.core.Vertx;
import io.vertx.core.http.Http3ClientConfig;
import io.vertx.core.http.HttpClientAgent;
import io.vertx.core.http.HttpClientConfig;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.net.ClientSSLOptions;
import io.vertx.core.net.PfxOptions;

@QuarkusTest
class Http3AutoTlsTest {

    @TestHTTPResource(value = "/hello", tls = true)
    URL tlsUrl;

    static Vertx vertx;

    record ResponseStruct(String version, String body, int status) {
    }

    @BeforeAll
    static void setup() {
        vertx = Vertx.vertx();
    }

    @AfterAll
    static void tearDown() throws TimeoutException {
        vertx.close().await(10, TimeUnit.SECONDS);
    }

    @Test
    void testHttp3WithTrustAll() throws Exception {
        HttpClientConfig clientConfig = new HttpClientConfig();
        clientConfig.setVersions(HttpVersion.HTTP_3);
        clientConfig.setHttp3Config(new Http3ClientConfig());
        ClientSSLOptions sslOptions = new ClientSSLOptions()
                .setTrustAll(true);
        HttpClientAgent client = vertx.httpClientBuilder().with(clientConfig).with(sslOptions).build();

        int port = tlsUrl.getPort();
        var response = client.request(HttpMethod.GET, port, "localhost", "/hello")
                .compose(HttpClientRequest::send)
                .compose(resp -> resp.body()
                        .map(body -> new ResponseStruct(resp.version().name(), body.toString(), resp.statusCode())))
                .await(10, TimeUnit.SECONDS);

        assertThat(response.version()).isEqualTo("HTTP_3");
        assertThat(response.status()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("hello");

        client.close().await(5, TimeUnit.SECONDS);
    }

    @Test
    void testHttp3WithTrustStore() throws Exception {
        HttpClientConfig clientConfig = new HttpClientConfig();
        clientConfig.setVersions(HttpVersion.HTTP_3);
        clientConfig.setHttp3Config(new Http3ClientConfig());
        ClientSSLOptions sslOptions = new ClientSSLOptions()
                .setTrustOptions(new PfxOptions()
                        .setPath("target/http3-dev-cert/http3-dev-truststore.p12")
                        .setPassword("http3-dev-password"));
        HttpClientAgent client = vertx.httpClientBuilder().with(clientConfig).with(sslOptions).build();

        int port = tlsUrl.getPort();
        var response = client.request(HttpMethod.GET, port, "localhost", "/version")
                .compose(HttpClientRequest::send)
                .compose(resp -> resp.body()
                        .map(body -> new ResponseStruct(resp.version().name(), body.toString(), resp.statusCode())))
                .await(10, TimeUnit.SECONDS);

        assertThat(response.version()).isEqualTo("HTTP_3");
        assertThat(response.status()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("HTTP_3");

        client.close().await(5, TimeUnit.SECONDS);
    }
}
