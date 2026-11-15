package io.quarkus.keycloak.pep.runtime;

import io.quarkus.keycloak.admin.client.common.runtime.KeycloakAdminClientJsonMapper;

/**
 * Support for the Keycloak admin client extensions when this extension is present: the admin client then
 * (de)serializes the Jackson 2 variant of the Keycloak representations brought by this extension, whose
 * {@code JsonNode} fields are bridged by {@link Jackson2JsonNodeModule}.
 */
public final class KeycloakAdminClientJackson2Bridge {

    private KeycloakAdminClientJackson2Bridge() {
    }

    public static KeycloakAdminClientJsonMapper jsonMapper() {
        return new KeycloakAdminClientJsonMapper(KeycloakAdminClientJsonMapper.keycloak().mapper().rebuild()
                .addModule(new Jackson2JsonNodeModule())
                .build());
    }
}
