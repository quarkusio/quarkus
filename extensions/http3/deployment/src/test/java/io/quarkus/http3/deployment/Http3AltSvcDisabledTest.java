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
class Http3AltSvcDisabledTest {

    @TestHTTPResource(value = "/hello", tls = true)
    URL tlsUrl;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(MyBean.class)
                    .addAsResource(new File("target/certs/http3-test-keystore.jks"), "server-keystore.jks"))
            .overrideConfigKey("quarkus.tls.key-store.jks.path", "server-keystore.jks")
            .overrideConfigKey("quarkus.tls.key-store.jks.password", "secret")
            .overrideConfigKey("quarkus.http3.alt-svc", "false");

    @Inject
    Vertx vertx;

    @Test
    void testNoAltSvcHeader() throws Exception {
        int port = tlsUrl.getPort();

        HttpClientConfig clientConfig = new HttpClientConfig();
        clientConfig.setVersions(HttpVersion.HTTP_1_1);
        clientConfig.setSsl(true);

        ClientSSLOptions sslOptions = new ClientSSLOptions()
                .setTrustOptions(new JksOptions().setPath("target/certs/http3-test-truststore.jks").setPassword("secret"));

        HttpClientAgent client = vertx.httpClientBuilder().with(clientConfig).with(sslOptions).build();

        var resp = client.request(HttpMethod.GET, port, "localhost", "/hello")
                .compose(HttpClientRequest::send)
                .await(5, TimeUnit.SECONDS);

        assertThat(resp.getHeader("Alt-Svc")).isNull();

        client.close().await(5, TimeUnit.SECONDS);
    }

    @ApplicationScoped
    static class MyBean {
        public void register(@Observes Router router) {
            router.get("/hello").handler(rc -> rc.response().end("hello"));
        }
    }
}
