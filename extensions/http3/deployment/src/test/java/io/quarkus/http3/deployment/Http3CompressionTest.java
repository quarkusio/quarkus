package io.quarkus.http3.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.vertx.core.runtime.VertxCoreRecorder;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;
import io.vertx.core.Vertx;
import io.vertx.core.http.Http3ClientConfig;
import io.vertx.core.http.HttpClientAgent;
import io.vertx.core.http.HttpClientConfig;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.net.ClientSSLOptions;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.Router;

@Certificates(baseDir = "target/certs", certificates = @Certificate(name = "http3-test", password = "secret", formats = {
        Format.JKS }))
class Http3CompressionTest {

    static final String TEXT = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor "
            + "incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation "
            + "ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit "
            + "in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat "
            + "non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."
            + "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor "
            + "incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation "
            + "ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit "
            + "in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat "
            + "non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";

    @TestHTTPResource(value = "/compress", tls = true)
    URL tlsUrl;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(MyBean.class)
                    .addAsResource(new File("target/certs/http3-test-keystore.jks"), "server-keystore.jks"))
            .overrideConfigKey("quarkus.tls.key-store.jks.path", "server-keystore.jks")
            .overrideConfigKey("quarkus.tls.key-store.jks.password", "secret")
            .overrideConfigKey("quarkus.http.enable-compression", "true");

    record Result(String version, int status, String contentEncoding, byte[] body) {
    }

    private Result request(Vertx vertx, int port, ClientSSLOptions sslOptions, HttpVersion version) throws Exception {
        HttpClientConfig clientConfig = new HttpClientConfig();
        clientConfig.setVersions(version);
        if (version == HttpVersion.HTTP_3) {
            clientConfig.setHttp3Config(new Http3ClientConfig());
        } else {
            clientConfig.setSsl(true);
        }

        HttpClientAgent client = vertx.httpClientBuilder().with(clientConfig).with(sslOptions).build();
        try {
            return client.request(HttpMethod.GET, port, "localhost", "/compress")
                    .compose(req -> {
                        req.putHeader("Accept-Encoding", "gzip");
                        return req.send();
                    })
                    .compose(resp -> resp.body().map(body -> new Result(
                            resp.version().name(),
                            resp.statusCode(),
                            resp.getHeader("content-encoding"),
                            body.getBytes())))
                    .await(10, TimeUnit.SECONDS);
        } finally {
            client.close().await(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void testCompressionWorksOverHttp1() throws Exception {
        Vertx vertx = VertxCoreRecorder.getVertx().get();
        int port = tlsUrl.getPort();
        ClientSSLOptions sslOptions = new ClientSSLOptions()
                .setTrustOptions(new JksOptions().setPath("target/certs/http3-test-truststore.jks").setPassword("secret"));

        Result response = request(vertx, port, sslOptions, HttpVersion.HTTP_1_1);

        assertThat(response.version).isEqualTo("HTTP_1_1");
        assertThat(response.status).isEqualTo(200);
        assertThat(response.contentEncoding).isEqualTo("gzip");
        assertThat(response.body.length).isLessThan(TEXT.length());

        String decompressed = new String(
                new GZIPInputStream(new ByteArrayInputStream(response.body)).readAllBytes());
        assertThat(decompressed).isEqualTo(TEXT);
    }

    @Test
    void testHttp3ResponseIsCorrectWithoutCompression() throws Exception {
        // Vert.x HTTP/3 (QUIC) does not currently apply server-side compression.
        // This test verifies the response is still correct (uncompressed).
        Vertx vertx = VertxCoreRecorder.getVertx().get();
        int port = tlsUrl.getPort();
        ClientSSLOptions sslOptions = new ClientSSLOptions()
                .setTrustOptions(new JksOptions().setPath("target/certs/http3-test-truststore.jks").setPassword("secret"));

        Result response = request(vertx, port, sslOptions, HttpVersion.HTTP_3);

        assertThat(response.version).isEqualTo("HTTP_3");
        assertThat(response.status).isEqualTo(200);
        assertThat(response.contentEncoding).isNull();
        assertThat(new String(response.body)).isEqualTo(TEXT);
    }

    @ApplicationScoped
    static class MyBean {
        public void register(@Observes Router router) {
            router.get("/compress").handler(rc -> {
                rc.response().headers().remove("content-encoding");
                rc.response().end(TEXT);
            });
        }
    }
}
