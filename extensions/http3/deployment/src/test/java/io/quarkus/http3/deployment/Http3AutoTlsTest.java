package io.quarkus.http3.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.quarkus.vertx.core.runtime.VertxCoreRecorder;
import io.vertx.core.Vertx;
import io.vertx.core.http.Http3ClientConfig;
import io.vertx.core.http.HttpClientAgent;
import io.vertx.core.http.HttpClientConfig;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.net.ClientSSLOptions;
import io.vertx.ext.web.Router;

/**
 * Verifies that HTTP/3 works when no TLS is explicitly configured:
 * the auto-TLS build step generates a self-signed certificate.
 */
class Http3AutoTlsTest {

    @TestHTTPResource(value = "/hello", tls = true)
    URL tlsUrl;

    // No TLS config, auto-TLS should kick in
    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(MyBean.class));


    @Inject
    TlsConfigurationRegistry registry;

    @Test
    void testAutoTlsCertificateRegistered() {
        assertThat(registry.get("http3-dev")).isPresent();
        assertThat(registry.get("http3-dev").get().getKeyStoreOptions()).isNotNull();
    }

    @Test
    void testHttp3WorksWithAutoTls() throws Exception {
        Vertx vertx = VertxCoreRecorder.getVertx().get();
        int port = tlsUrl.getPort();

        HttpClientConfig clientConfig = new HttpClientConfig();
        clientConfig.setVersions(HttpVersion.HTTP_3);
        clientConfig.setHttp3Config(new Http3ClientConfig());

        ClientSSLOptions sslOptions = new ClientSSLOptions().setTrustAll(true);

        HttpClientAgent client = vertx.httpClientBuilder().with(clientConfig).with(sslOptions).build();

        var response = client.request(HttpMethod.GET, port, "localhost", "/hello")
                .compose(HttpClientRequest::send)
                .compose(resp -> resp.body()
                        .map(body -> new ResponseStruct(resp.version().name(), body.toString(), resp.statusCode())))
                .await(10, TimeUnit.SECONDS);

        assertThat(response.version).isEqualTo("HTTP_3");
        assertThat(response.status).isEqualTo(200);
        assertThat(response.body).isEqualTo("auto-tls-hello");

        client.close().await(5, TimeUnit.SECONDS);
    }

    record ResponseStruct(String version, String body, int status) {
    }

    @ApplicationScoped
    static class MyBean {
        public void register(@Observes Router router) {
            router.get("/hello").handler(rc -> rc.response().end("auto-tls-hello"));
        }
    }
}
