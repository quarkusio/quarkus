package io.quarkus.oidc.client;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;

import org.junit.jupiter.api.Test;

public class OidcClientConfigTest {

    @Test
    public void testCopyBetweenConfigMappingAndClass() {
        var configMappingGroup = new OidcClientConfigImpl();
        new OidcClientConfig(configMappingGroup);
        for (var configMappingMethod : EnumSet.allOf(OidcClientConfigImpl.ConfigMappingMethods.class)) {
            Boolean invoked = configMappingGroup.invocationsRecorder.get(configMappingMethod);
            assertTrue(invoked != null && invoked,
                    "OidcClientConfig method '%s' return value is not copied from interface to class"
                            .formatted(configMappingMethod));
        }
    }

}
