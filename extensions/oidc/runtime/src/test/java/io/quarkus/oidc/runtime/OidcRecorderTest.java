package io.quarkus.oidc.runtime;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import io.quarkus.oidc.common.runtime.OidcCommonConfig.Proxy;
import io.quarkus.oidc.common.runtime.OidcCommonUtils;
import io.vertx.core.http.HttpClientOptions;

public class OidcRecorderTest {

    @Test
    public void testWithoutProxyConfigurationNameCheckNonPresent() {
        Proxy proxy = new Proxy();
        HttpClientOptions options = new HttpClientOptions();
        OidcCommonUtils.configureProxy(proxy, options, null);
        assertNull(options.getProxyOptions());
    }

}
