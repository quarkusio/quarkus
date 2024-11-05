package io.quarkus.oidc.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;

import org.junit.jupiter.api.Test;

import io.quarkus.oidc.OidcTenantConfig;

public class OidcTenantConfigTest {

    @Test
    public void testCopyBetweenConfigMappingAndClass() {
        var configMappingGroup = new OidcTenantConfigImpl();
        new OidcTenantConfig(configMappingGroup, "tested-later");
        for (var configMappingMethod : EnumSet.allOf(OidcTenantConfigImpl.ConfigMappingMethods.class)) {
            Boolean invoked = configMappingGroup.invocationsRecorder.get(configMappingMethod);
            assertTrue(invoked != null && invoked,
                    "OidcTenantConfig method '%s' return value is not copied from interface to class"
                            .formatted(configMappingMethod));
        }
    }

    @Test
    public void testTenantIdConfiguration() {
        // prefer tenant id configured by user
        var configMappingGroup = new OidcTenantConfigImpl("user-defined-value");
        var tenantConfig = new OidcTenantConfig(configMappingGroup, "fallback");
        assertEquals("user-defined-value", tenantConfig.getTenantId().get());

        // fallback to tenant key
        configMappingGroup = new OidcTenantConfigImpl();
        tenantConfig = new OidcTenantConfig(configMappingGroup, "tenant-key");
        assertEquals("tenant-key", tenantConfig.getTenantId().get());
    }

}
