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
class Http3AltSvcEnabledTest {

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

    @Test
    void testAltSvcOnHttp1() throws Exception {
        int port = tlsUrl.getPort();

        HttpClientConfig clientConfig = new HttpClientConfig();
        clientConfig.setVersions(HttpVersion.HTTP_1_1);
        clientConfig.setSsl(true);

        ClientSSLOptions sslOptions = new ClientSSLOptions()
                .setTrustOptions(new JksOptions().setPath("target/certs/http3-test-truststore.jks").setPassword("secret"));

        HttpClientAgent client = vertx.httpClientBuilder().with(clientConfig).with(sslOptions).build();

        try {
            var resp = client.request(HttpMethod.GET, port, "localhost", "/hello")
                    .compose(HttpClientRequest::send)
                    .await(5, TimeUnit.SECONDS);

            assertThat(resp.getHeader("Alt-Svc")).isEqualTo("h3=\":" + port + "\"; ma=86400");
        } finally {
            client.close().await(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void testAltSvcOnHttp2() throws Exception {
        int port = tlsUrl.getPort();

        HttpClientConfig clientConfig = new HttpClientConfig();
        clientConfig.setVersions(HttpVersion.HTTP_2);
        clientConfig.setSsl(true);

        ClientSSLOptions sslOptions = new ClientSSLOptions()
                .setTrustOptions(new JksOptions().setPath("target/certs/http3-test-truststore.jks").setPassword("secret"));

        HttpClientAgent client = vertx.httpClientBuilder().with(clientConfig).with(sslOptions).build();

        try {
            var resp = client.request(HttpMethod.GET, port, "localhost", "/hello")
                    .compose(HttpClientRequest::send)
                    .await(5, TimeUnit.SECONDS);

            // With writeAltSvc, HTTP/2 delivers Alt-Svc as an ALTSVC frame instead of a response header.
            assertThat(resp.getHeader("Alt-Svc")).isNull();
        } finally {
            client.close().await(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void testUpgradeFromHttp2ToHttp3() throws Exception {
        int port = tlsUrl.getPort();

        HttpClientConfig clientConfig = new HttpClientConfig();
        clientConfig.setVersions(HttpVersion.HTTP_3, HttpVersion.HTTP_2, HttpVersion.HTTP_1_1);
        clientConfig.setSsl(true);
        clientConfig.setHttp3Config(new Http3ClientConfig());
        clientConfig.setFollowAlternativeServices(true);

        ClientSSLOptions sslOptions = new ClientSSLOptions()
                .setTrustOptions(new JksOptions().setPath("target/certs/http3-test-truststore.jks").setPassword("secret"));

        HttpClientAgent client = vertx.httpClientBuilder().with(clientConfig).with(sslOptions).build();

        try {
            // First request goes over TCP (ALPN picks HTTP/2); the server advertises HTTP/3 with an ALTSVC frame
            assertThat(get(client, port)).isEqualTo(HttpVersion.HTTP_2);

            // The client processes the advertisement asynchronously, so allow a few attempts
            HttpVersion version = null;
            for (int i = 0; i < 20 && version != HttpVersion.HTTP_3; i++) {
                version = get(client, port);
                if (version != HttpVersion.HTTP_3) {
                    Thread.sleep(100);
                }
            }
            assertThat(version).isEqualTo(HttpVersion.HTTP_3);
        } finally {
            client.close().await(5, TimeUnit.SECONDS);
        }
    }

    private static HttpVersion get(HttpClientAgent client, int port) throws Exception {
        // Vert.x only follows Alt-Svc for hosts containing a dot, so "localhost" would never upgrade
        var resp = client.request(HttpMethod.GET, port, "127.0.0.1", "/hello")
                .compose(HttpClientRequest::send)
                .await(10, TimeUnit.SECONDS);
        resp.body().await(5, TimeUnit.SECONDS);
        return resp.version();
    }

    @Test
    void testNoAltSvcOnHttp3() throws Exception {
        int port = tlsUrl.getPort();

        HttpClientConfig clientConfig = new HttpClientConfig();
        clientConfig.setVersions(HttpVersion.HTTP_3);
        clientConfig.setHttp3Config(new Http3ClientConfig());

        ClientSSLOptions sslOptions = new ClientSSLOptions()
                .setTrustOptions(new JksOptions().setPath("target/certs/http3-test-truststore.jks").setPassword("secret"));

        HttpClientAgent client = vertx.httpClientBuilder().with(clientConfig).with(sslOptions).build();

        try {
            var resp = client.request(HttpMethod.GET, port, "localhost", "/hello")
                    .compose(HttpClientRequest::send)
                    .await(10, TimeUnit.SECONDS);

            assertThat(resp.getHeader("Alt-Svc")).isNull();
        } finally {
            client.close().await(5, TimeUnit.SECONDS);
        }
    }

    @ApplicationScoped
    static class MyBean {
        public void register(@Observes Router router) {
            router.get("/hello").handler(rc -> rc.response().end("hello"));
        }
    }
}
