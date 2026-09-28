package io.quarkus.vertx.http.proxy;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileInputStream;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.function.Consumer;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManagerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.deployment.util.IoUtil;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.quarkus.vertx.http.runtime.options.HttpServerOptionsUtils;
import io.restassured.RestAssured;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;
import io.vertx.core.Handler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

@Certificates(baseDir = "target/certs", certificates = @Certificate(name = "proxy-protocol-test", formats = Format.PEM))
public class ProxyProtocolPerListenerTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(RemoteAddressRoute.class)
                    .addAsResource(new File("target/certs/proxy-protocol-test.key"), "server-key.key")
                    .addAsResource(new File("target/certs/proxy-protocol-test.crt"), "server-cert.crt"))
            .overrideConfigKey("quarkus.http.insecure-requests", "enabled")
            .overrideConfigKey("quarkus.tls.key-store.pem.0.cert", "server-cert.crt")
            .overrideConfigKey("quarkus.tls.key-store.pem.0.key", "server-key.key")
            .overrideConfigKey("quarkus.http.proxy.use-proxy-protocol", "true")
            .overrideConfigKey("quarkus.http.proxy.proxy-protocol-listeners", "https")
            .overrideConfigKey("quarkus.management.enabled", "true")
            .overrideConfigKey("quarkus.management.proxy.use-proxy-protocol", "true")
            .overrideConfigKey("quarkus.management.proxy.proxy-protocol-listeners", "https")
            .addBuildChainCustomizer(buildCustomizer())
            .setLogRecordPredicate(r -> r.getLoggerName().equals(HttpServerOptionsUtils.class.getName()))
            .assertLogRecords(records -> assertThat(records).isEmpty());

    @TestHTTPResource(value = "/remote", tls = true)
    URL mainUrl;

    @TestHTTPResource(value = "/management-remote", management = true, tls = true)
    URL managementUrl;

    @Test
    public void plainHttpIsServedWithoutProxyHeader() {
        RestAssured.get("/remote").then().statusCode(200).body(Matchers.is("127.0.0.1"));
    }

    @Test
    public void httpsReportsTheClientAddressFromTheProxyHeader() throws Exception {
        String response = requestWithProxyHeader(mainUrl);
        assertThat(response).startsWith("HTTP/1.1 200").endsWith("1.2.3.4");
    }

    @Test
    public void managementHttpsReportsTheClientAddressFromTheProxyHeader() throws Exception {
        String response = requestWithProxyHeader(managementUrl);
        assertThat(response).startsWith("HTTP/1.1 200").endsWith("1.2.3.4");
    }

    private static String requestWithProxyHeader(URL url) throws Exception {
        X509Certificate certificate;
        try (FileInputStream in = new FileInputStream("target/certs/proxy-protocol-test.crt")) {
            certificate = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(in);
        }
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);
        trustStore.setCertificateEntry("server", certificate);
        TrustManagerFactory trustManagerFactory = TrustManagerFactory
                .getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustManagerFactory.getTrustManagers(), null);

        try (Socket raw = new Socket(url.getHost(), url.getPort())) {
            raw.getOutputStream().write(("PROXY TCP4 1.2.3.4 127.0.0.1 1234 " + url.getPort() + "\r\n")
                    .getBytes(StandardCharsets.US_ASCII));
            raw.getOutputStream().flush();
            try (SSLSocket tls = (SSLSocket) sslContext.getSocketFactory().createSocket(raw, url.getHost(), url.getPort(),
                    true)) {
                tls.startHandshake();
                tls.getOutputStream().write(
                        ("GET " + url.getPath() + " HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n")
                                .getBytes(StandardCharsets.US_ASCII));
                tls.getOutputStream().flush();
                return new String(IoUtil.readBytes(tls.getInputStream()), StandardCharsets.UTF_8);
            }
        }
    }

    static Consumer<BuildChainBuilder> buildCustomizer() {
        return builder -> builder.addBuildStep(new BuildStep() {
            @Override
            public void execute(BuildContext context) {
                NonApplicationRootPathBuildItem buildItem = context.consume(NonApplicationRootPathBuildItem.class);
                context.produce(buildItem.routeBuilder()
                        .management()
                        .route("management-remote")
                        .handler(new RemoteAddressHandler())
                        .build());
            }
        }).produces(RouteBuildItem.class)
                .consumes(NonApplicationRootPathBuildItem.class)
                .build();
    }

    public static class RemoteAddressHandler implements Handler<RoutingContext> {
        @Override
        public void handle(RoutingContext rc) {
            rc.response().end(rc.request().remoteAddress().host());
        }
    }

    @ApplicationScoped
    public static class RemoteAddressRoute {

        void register(@Observes Router router) {
            router.get("/remote").handler(new RemoteAddressHandler());
        }
    }
}
