package io.quarkus.oidc.runtime;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.common.runtime.OidcCommonUtils;

public class OidcRecorderTest {

    @Test
    public void testtoProxyOptionsWithHostCheckPresent() {
        OidcTenantConfig config = OidcTenantConfig.builder().proxy("server.example.com", 80).build();
        assertTrue(OidcCommonUtils.toProxyOptions(config.proxy(), null).isPresent());
    }

    @Test
    public void testtoProxyOptionsWithoutHostCheckNonPresent() {
        OidcTenantConfig config = OidcTenantConfig.builder().build();
        assertFalse(OidcCommonUtils.toProxyOptions(config.proxy(), null).isPresent());
    }

}
