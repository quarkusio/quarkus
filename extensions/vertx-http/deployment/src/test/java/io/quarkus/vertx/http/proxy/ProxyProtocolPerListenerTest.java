package io.quarkus.vertx.http.proxy;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManagerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.deployment.util.IoUtil;
import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;
import io.vertx.ext.web.Router;

/**
 * The PROXY protocol is enabled for the HTTPS listener only: HTTPS connections must start with a PROXY header, whose
 * client address is reported as the remote address, while plain HTTP connections are served without one.
 */
@Certificates(baseDir = "target/certs", certificates = @Certificate(name = "proxy-protocol-test", password = "secret", formats = {
        Format.JKS, Format.PKCS12, Format.PEM }))
public class ProxyProtocolPerListenerTest {

    private static final String CONFIGURATION = """
            quarkus.http.insecure-requests=enabled
            quarkus.tls.key-store.pem.0.cert=server-cert.crt
            quarkus.tls.key-store.pem.0.key=server-key.key
            quarkus.http.proxy.use-proxy-protocol=true
            quarkus.http.proxy.proxy-protocol-listeners=https
            """;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(RemoteAddressRoute.class)
                    .addAsResource(new StringAsset(CONFIGURATION), "application.properties")
                    .addAsResource(new File("target/certs/proxy-protocol-test.key"), "server-key.key")
                    .addAsResource(new File("target/certs/proxy-protocol-test.crt"), "server-cert.crt"));

    @Test
    public void plainHttpIsServedWithoutProxyHeader() {
        RestAssured.get("/remote").then().statusCode(200).body(Matchers.is("127.0.0.1"));
    }

    @Test
    public void httpsReportsTheClientAddressFromTheProxyHeader() throws Exception {
        KeyStore trustStore = KeyStore.getInstance("JKS");
        try (FileInputStream in = new FileInputStream("target/certs/proxy-protocol-test-truststore.jks")) {
            trustStore.load(in, "secret".toCharArray());
        }
        TrustManagerFactory trustManagerFactory = TrustManagerFactory
                .getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustManagerFactory.getTrustManagers(), null);

        try (Socket raw = new Socket("localhost", 8444)) {
            raw.getOutputStream().write("PROXY TCP4 1.2.3.4 127.0.0.1 1234 8444\r\n".getBytes(StandardCharsets.US_ASCII));
            raw.getOutputStream().flush();
            try (SSLSocket tls = (SSLSocket) sslContext.getSocketFactory().createSocket(raw, "localhost", 8444, true)) {
                tls.startHandshake();
                tls.getOutputStream().write(
                        "GET /remote HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n"
                                .getBytes(StandardCharsets.US_ASCII));
                tls.getOutputStream().flush();
                String response = new String(IoUtil.readBytes(tls.getInputStream()), StandardCharsets.UTF_8);
                assertTrue(response.startsWith("HTTP/1.1 200"), response);
                assertTrue(response.endsWith("1.2.3.4"), response);
            }
        }
    }

    @ApplicationScoped
    public static class RemoteAddressRoute {

        void register(@Observes Router router) {
            router.get("/remote").handler(rc -> rc.response().end(rc.request().remoteAddress().host()));
        }
    }
}
