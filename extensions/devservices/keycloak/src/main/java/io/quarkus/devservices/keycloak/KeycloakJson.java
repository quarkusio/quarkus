package io.quarkus.devservices.keycloak;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;

import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.util.JsonSerialization;

import tools.jackson.databind.json.JsonMapper;

/**
 * (De)serializes the Keycloak representations with the Jackson 3 mapper of the Keycloak admin client, or with Keycloak's
 * Jackson 2 {@code JsonSerialization} when the Jackson 2 variant of the representations is on the classpath (it is
 * brought by the Keycloak authorization extension and then wins over the Jackson 3 variant).
 */
final class KeycloakJson {

    private static final boolean JACKSON2_REPRESENTATIONS = isPresent("org.keycloak.util.JsonSerialization");
    private static volatile JsonMapper jackson3Mapper;

    private KeycloakJson() {
    }

    static RealmRepresentation readRealm(InputStream is) throws IOException {
        if (JACKSON2_REPRESENTATIONS) {
            return JsonSerialization.readValue(is, RealmRepresentation.class);
        }
        return jackson3Mapper().readValue(is, RealmRepresentation.class);
    }

    static String writeRealm(RealmRepresentation realm) throws IOException {
        if (JACKSON2_REPRESENTATIONS) {
            return JsonSerialization.writeValueAsString(realm);
        }
        return jackson3Mapper().writeValueAsString(realm);
    }

    private static JsonMapper jackson3Mapper() {
        JsonMapper mapper = jackson3Mapper;
        if (mapper == null) {
            // the mapper of the Keycloak admin client, held by a package-private holder
            // TODO: make the holder public in Keycloak, then call it directly
            try {
                Field field = Class.forName("org.keycloak.admin.client.jackson3.Jackson3MapperHolder")
                        .getDeclaredField("MAPPER");
                field.setAccessible(true);
                mapper = (JsonMapper) field.get(null);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Failed to obtain the Jackson 3 mapper of the Keycloak admin client", e);
            }
            jackson3Mapper = mapper;
        }
        return mapper;
    }

    private static boolean isPresent(String className) {
        try {
            Class.forName(className, false, KeycloakJson.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
