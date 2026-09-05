package io.quarkus.oidc.runtime;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import io.quarkus.oidc.common.runtime.OidcCommonConfig.Proxy;
import io.quarkus.oidc.common.runtime.OidcCommonUtils;

public class OidcRecorderTest {

    @Test
    public void testtoProxyOptionsWithoutProxyConfigurationNameCheckNonPresent() {
        Proxy proxy = new Proxy();
        assertFalse(OidcCommonUtils.toProxyOptions(proxy, null).isPresent());
    }

}
