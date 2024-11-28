package io.quarkus.oidc.runtime;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.quarkus.oidc.common.runtime.OidcCommonConfig.Proxy;

public class OidcRecorderTest {

    @Test
    public void testtoProxyOptionsWithHostCheckPresent() {
        Proxy proxy = new Proxy();
        proxy.host = Optional.of("server.example.com");
        assertTrue(OidcRecorder.toProxyOptions(proxy).isPresent());
    }

    @Test
    public void testtoProxyOptionsWithoutHostCheckNonPresent() {
        Proxy proxy = new Proxy();
        assertFalse(OidcRecorder.toProxyOptions(proxy).isPresent());
    }

}
