package io.quarkus.http3.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
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
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.Router;

@Certificates(baseDir = "target/certs", certificates = @Certificate(name = "http3-test", password = "secret", formats = {
        Format.JKS }))
class Http3ServerTest {

    @TestHTTPResource(value = "/hello", tls = true)
    URL tlsUrl;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(MyBean.class)
                    .addAsResource(new File("target/certs/http3-test-keystore.jks"), "server-keystore.jks"))
            .overrideConfigKey("quarkus.tls.key-store.jks.path", "server-keystore.jks")
            .overrideConfigKey("quarkus.tls.key-store.jks.password", "secret");

    @Inject
    Vertx vertx;

    record ResponseStruct(String version, String body, int status) {
    }

    @Test
    void testHttp3Connection() throws Exception {
        int port = tlsUrl.getPort();

        HttpClientConfig clientConfig = new HttpClientConfig();
        clientConfig.setVersions(HttpVersion.HTTP_3);
        clientConfig.setHttp3Config(new Http3ClientConfig());

        ClientSSLOptions sslOptions = new ClientSSLOptions()
                .setTrustOptions(new JksOptions().setPath("target/certs/http3-test-truststore.jks").setPassword("secret"));

        HttpClientAgent client = vertx.httpClientBuilder().with(clientConfig).with(sslOptions).build();

        var response = client.request(HttpMethod.GET, port, "localhost", "/hello")
                .compose(HttpClientRequest::send)
                .compose(resp -> resp.body()
                        .map(body -> new ResponseStruct(resp.version().name(), body.toString(), resp.statusCode())))
                .await(10, TimeUnit.SECONDS);

        assertThat(response.version).isEqualTo("HTTP_3");
        assertThat(response.status).isEqualTo(200);
        assertThat(response.body).isEqualTo(HttpVersion.HTTP_3 + "-hello");

        client.close().await(5, TimeUnit.SECONDS);
    }

    @Test
    void testHttp3PostWithBody() throws Exception {
        int port = tlsUrl.getPort();

        HttpClientConfig clientConfig = new HttpClientConfig();
        clientConfig.setVersions(HttpVersion.HTTP_3);
        clientConfig.setHttp3Config(new Http3ClientConfig());

        ClientSSLOptions sslOptions = new ClientSSLOptions()
                .setTrustOptions(new JksOptions().setPath("target/certs/http3-test-truststore.jks").setPassword("secret"));

        HttpClientAgent client = vertx.httpClientBuilder().with(clientConfig).with(sslOptions).build();

        String requestBody = "{\"message\":\"hello from http3\"}";
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
    void testHttp3LargeResponse() throws Exception {
        int port = tlsUrl.getPort();

        HttpClientConfig clientConfig = new HttpClientConfig();
        clientConfig.setVersions(HttpVersion.HTTP_3);
        clientConfig.setHttp3Config(new Http3ClientConfig());

        ClientSSLOptions sslOptions = new ClientSSLOptions()
                .setTrustOptions(new JksOptions().setPath("target/certs/http3-test-truststore.jks").setPassword("secret"));

        HttpClientAgent client = vertx.httpClientBuilder().with(clientConfig).with(sslOptions).build();

        var response = client.request(HttpMethod.GET, port, "localhost", "/large")
                .compose(HttpClientRequest::send)
                .compose(resp -> resp.body()
                        .map(body -> new ResponseStruct(resp.version().name(), body.toString(), resp.statusCode())))
                .await(15, TimeUnit.SECONDS);

        assertThat(response.version).isEqualTo("HTTP_3");
        assertThat(response.status).isEqualTo(200);
        assertThat(response.body).hasSize(100 * 1024);
        assertThat(response.body).matches("A+");

        client.close().await(5, TimeUnit.SECONDS);
    }

    @ApplicationScoped
    static class MyBean {
        public void register(@Observes Router router) {
            router.get("/hello").handler(rc -> {
                rc.response().end(rc.request().version() + "-hello");
            });
            router.post("/echo").handler(io.vertx.ext.web.handler.BodyHandler.create());
            router.post("/echo").handler(rc -> {
                rc.response().end(rc.body().buffer());
            });
            router.get("/large").handler(rc -> {
                rc.response().end("A".repeat(100 * 1024));
            });
        }
    }
}
