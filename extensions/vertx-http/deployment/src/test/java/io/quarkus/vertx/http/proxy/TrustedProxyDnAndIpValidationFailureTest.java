package io.quarkus.vertx.http.proxy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.vertx.http.ForwardedHandlerInitializer;

class TrustedProxyDnAndIpValidationFailureTest {

    private static final String configuration = """
            quarkus.http.proxy.proxy-address-forwarding=true
            quarkus.http.proxy.allow-forwarded=true
            quarkus.http.proxy.trusted-proxies=localhost
            quarkus.http.proxy.trusted-proxy-dns=CN=my-trusted-proxy
            """;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(ForwardedHandlerInitializer.class)
                    .addAsResource(new StringAsset(configuration), "application.properties"))
            .assertException(t -> assertThat(t).hasMessageContaining("quarkus.http.proxy.trusted-proxies")
                    .hasMessageContaining("quarkus.http.proxy.trusted-proxy-dns"));

    @Test
    void testStartupFails() {
        fail("Application should not have started");
    }
}
