package io.quarkus.oidc.common.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.vertx.core.net.ProxyOptions;

public class OidcCommonUtilsTest {

    @Test
    public void testProxyOptionsWithHostWithoutScheme() throws Exception {
        OidcCommonConfig.Proxy config = new OidcCommonConfig.Proxy();
        config.host = Optional.of("localhost");
        config.port = 8080;
        config.username = Optional.of("user");
        config.password = Optional.of("password");

        ProxyOptions options = OidcCommonUtils.toProxyOptions(config).get();
        assertEquals("localhost", options.getHost());
        assertEquals(8080, options.getPort());
        assertEquals("user", options.getUsername());
        assertEquals("password", options.getPassword());
    }

    @Test
    public void testProxyOptionsWithHostWithScheme() throws Exception {
        OidcCommonConfig.Proxy config = new OidcCommonConfig.Proxy();
        config.host = Optional.of("http://localhost");
        config.port = 8080;
        config.username = Optional.of("user");
        config.password = Optional.of("password");

        assertEquals("http", URI.create(config.host.get()).getScheme());

        ProxyOptions options = OidcCommonUtils.toProxyOptions(config).get();
        assertEquals("localhost", options.getHost());
        assertEquals(8080, options.getPort());
        assertEquals("user", options.getUsername());
        assertEquals("password", options.getPassword());
    }
}
