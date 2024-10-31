package io.quarkus.oidc.client.registration;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;

import org.junit.jupiter.api.Test;

public class OidcClientRegistrationConfigTest {

    @Test
    public void testCopyBetweenConfigMappingAndClass() {
        var configMappingGroup = new OidcClientRegistrationConfigImpl();
        new OidcClientRegistrationConfig(configMappingGroup);
        for (var configMappingMethod : EnumSet.allOf(OidcClientRegistrationConfigImpl.ConfigMappingMethods.class)) {
            Boolean invoked = configMappingGroup.invocationsRecorder.get(configMappingMethod);
            assertTrue(invoked != null && invoked,
                    "OidcClientRegistrationConfig method '%s' return value is not copied from interface to class"
                            .formatted(configMappingMethod));
        }
    }

}
