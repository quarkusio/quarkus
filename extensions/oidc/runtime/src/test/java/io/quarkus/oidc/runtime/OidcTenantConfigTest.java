package io.quarkus.oidc.runtime;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;

import org.junit.jupiter.api.Test;

import io.quarkus.oidc.OidcTenantConfig;

public class OidcTenantConfigTest {

    @Test
    public void testCopyBetweenConfigMappingAndClass() {
        var configMappingGroup = new OidcTenantConfigImpl();
        OidcTenantConfig.builder(configMappingGroup).tenantId("tested-later").build();
        for (var configMappingMethod : EnumSet.allOf(OidcTenantConfigImpl.ConfigMappingMethods.class)) {
            Boolean invoked = configMappingGroup.invocationsRecorder.get(configMappingMethod);
            assertTrue(invoked != null && invoked,
                    "OidcTenantConfig method '%s' return value is not copied from interface to class"
                            .formatted(configMappingMethod));
        }
    }

}
