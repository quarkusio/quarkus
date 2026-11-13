package io.quarkus.vertx.http;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.net.InetSocketAddress;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.Address;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;

@Certificates(baseDir = "target/certs", certificates = @Certificate(name = "address-test", password = "secret", formats = {
        Format.PEM }))
class VertxHttpSecureAddressTest {
    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset("""
                            quarkus.http.insecure-requests=REDIRECT
                            quarkus.http.ssl.certificate.files=server-cert.crt
                            quarkus.http.ssl.certificate.key-files=server-key.key
                            """), "application.properties")
                    .addAsResource(new File("target/certs/address-test.key"), "server-key.key")
                    .addAsResource(new File("target/certs/address-test.crt"), "server-cert.crt"));

    @Inject
    @Named("vertx-http")
    Address http;

    @Inject
    @Named("vertx-https")
    Address https;

    @Inject
    HttpServer httpServer;

    @Test
    void httpAndHttpsAreSeparateAddresses() {
        assertThat(http.protocol()).isEqualTo("http");
        assertThat(http.inet()).contains(new InetSocketAddress("localhost", 8081));
        assertThat(https.protocol()).isEqualTo("https");
        assertThat(https.inet()).contains(new InetSocketAddress("localhost", 8444));
        assertThat(httpServer.getAddress()).contains(http);
        assertThat(httpServer.getSecureAddress()).contains(https);
    }
}
