package io.quarkus.keycloak.admin.client.common.runtime;

import tools.jackson.databind.json.JsonMapper;

/**
 * The Jackson 3 {@link JsonMapper} used by the Keycloak admin client extensions to (de)serialize the Keycloak
 * representations, registered as a service at build time: the mapper of the Keycloak admin client by
 * {@code KeycloakAdminClientProcessor}, or a mapper handling the Jackson 2 variant of the representations by the
 * Keycloak authorization extension, which is the one bringing that variant.
 */
public final class KeycloakAdminClientJsonMapper {

    private final JsonMapper mapper;

    public KeycloakAdminClientJsonMapper(JsonMapper mapper) {
        this.mapper = mapper;
    }

    public JsonMapper mapper() {
        return mapper;
    }

    /**
     * @return the mapper of the Keycloak admin client
     */
    public static KeycloakAdminClientJsonMapper keycloak() {
        return new KeycloakAdminClientJsonMapper(keycloakMapper());
    }

    /**
     * Returns {@code org.keycloak.admin.client.jackson3.Jackson3MapperHolder#MAPPER}, the mapper of the Keycloak admin client.
     * The holder is package-private, so the body of this method is generated at build time by
     * {@code KeycloakAdminClientProcessor}.
     * TODO: make the holder public in Keycloak, then call it directly and drop the generation
     */
    static native JsonMapper keycloakMapper();
}
