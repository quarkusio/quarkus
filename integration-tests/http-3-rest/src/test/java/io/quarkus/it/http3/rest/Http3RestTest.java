package io.quarkus.it.http3.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.vertx.core.runtime.VertxCoreRecorder;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
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
class Http3RestTest {

    @TestHTTPResource(value = "/hello", tls = true)
    URL tlsUrl;

    record ResponseStruct(String version, String body, int status) {
    }

    private HttpClientAgent createHttp3Client() {
        Vertx vertx = VertxCoreRecorder.getVertx().get();
        HttpClientConfig clientConfig = new HttpClientConfig();
        clientConfig.setVersions(HttpVersion.HTTP_3);
        clientConfig.setHttp3Config(new Http3ClientConfig());
        ClientSSLOptions sslOptions = new ClientSSLOptions()
                .setTrustOptions(new PfxOptions().setPath("target/certs/http3-test-truststore.p12").setPassword("secret"));
        return vertx.httpClientBuilder().with(clientConfig).with(sslOptions).build();
    }

    @Test
    void testHttp3Get() throws Exception {
        HttpClientAgent client = createHttp3Client();
        int port = tlsUrl.getPort();

        var response = client.request(HttpMethod.GET, port, "localhost", "/hello")
                .compose(HttpClientRequest::send)
                .compose(resp -> resp.body()
                        .map(body -> new ResponseStruct(resp.version().name(), body.toString(), resp.statusCode())))
                .await(10, TimeUnit.SECONDS);

        assertThat(response.version).isEqualTo("HTTP_3");
        assertThat(response.status).isEqualTo(200);
        assertThat(response.body).isEqualTo("hello");

        client.close().await(5, TimeUnit.SECONDS);
    }

    @Test
    void testHttp3Post() throws Exception {
        HttpClientAgent client = createHttp3Client();
        int port = tlsUrl.getPort();

        String requestBody = "echo me over http3";
        var response = client.request(HttpMethod.POST, port, "localhost", "/echo")
                .compose(req -> req.send(Buffer.buffer(requestBody)))
                .compose(resp -> resp.body()
                        .map(body -> new ResponseStruct(resp.version().name(), body.toString(), resp.statusCode())))
                .await(10, TimeUnit.SECONDS);

        assertThat(response.version).isEqualTo("HTTP_3");
        assertThat(response.status).isEqualTo(200);
        assertThat(response.body).isEqualTo(requestBody);

        client.close().await(5, TimeUnit.SECONDS);
    }

    @Test
    void testHttp3Version() throws Exception {
        HttpClientAgent client = createHttp3Client();
        int port = tlsUrl.getPort();

        var response = client.request(HttpMethod.GET, port, "localhost", "/version")
                .compose(HttpClientRequest::send)
                .compose(resp -> resp.body()
                        .map(body -> new ResponseStruct(resp.version().name(), body.toString(), resp.statusCode())))
                .await(10, TimeUnit.SECONDS);

        assertThat(response.version).isEqualTo("HTTP_3");
        assertThat(response.status).isEqualTo(200);
        assertThat(response.body).isEqualTo("HTTP_3");

        client.close().await(5, TimeUnit.SECONDS);
    }

    @Test
    void testAltSvcHeader() throws Exception {
        Vertx vertx = VertxCoreRecorder.getVertx().get();
        int port = tlsUrl.getPort();

        HttpClientConfig clientConfig = new HttpClientConfig();
        clientConfig.setVersions(HttpVersion.HTTP_2);
        clientConfig.setSsl(true);
        ClientSSLOptions sslOptions = new ClientSSLOptions()
                .setTrustOptions(new PfxOptions().setPath("target/certs/http3-test-truststore.p12").setPassword("secret"));
        HttpClientAgent client = vertx.httpClientBuilder().with(clientConfig).with(sslOptions).build();

        var resp = client.request(HttpMethod.GET, port, "localhost", "/hello")
                .compose(HttpClientRequest::send)
                .await(10, TimeUnit.SECONDS);

        String altSvc = resp.getHeader("Alt-Svc");
        assertThat(altSvc).isNotNull();
        assertThat(altSvc).startsWith("h3=\":");

        resp.body().await(5, TimeUnit.SECONDS);
        client.close().await(5, TimeUnit.SECONDS);
    }
}
