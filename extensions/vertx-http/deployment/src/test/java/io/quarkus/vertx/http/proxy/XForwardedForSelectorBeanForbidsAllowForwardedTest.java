package io.quarkus.vertx.http.proxy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.vertx.http.ForwardedHandlerInitializer;

class XForwardedForSelectorBeanForbidsAllowForwardedTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(ForwardedHandlerInitializer.class, CustomXForwardedForSelector.class))
            .withConfiguration("""
                    quarkus.http.proxy.proxy-address-forwarding=true
                    quarkus.http.proxy.allow-x-forwarded=true
                    quarkus.http.proxy.allow-forwarded=true
                    quarkus.http.proxy.trusted-proxies=127.0.0.1
                    """)
            .assertException(t -> assertThat(t)
                    .hasMessageContaining(CustomXForwardedForSelector.class.getName())
                    .hasMessageContaining("quarkus.http.proxy.allow-forwarded"));

    @Test
    void testStartupFails() {
        fail("Application should not have started");
    }
}
