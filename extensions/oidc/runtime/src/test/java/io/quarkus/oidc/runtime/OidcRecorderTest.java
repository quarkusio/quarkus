package io.quarkus.oidc.runtime;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.common.runtime.OidcCommonUtils;
import io.vertx.core.http.HttpClientOptions;

public class OidcRecorderTest {

    @Test
    public void testWithoutProxyConfigurationNameCheckNonPresent() {
        OidcTenantConfig config = OidcTenantConfig.builder().build();
        HttpClientOptions options = new HttpClientOptions();
        OidcCommonUtils.configureProxy(config.proxy(), options, null);
        assertNull(options.getProxyOptions());
    }

}
