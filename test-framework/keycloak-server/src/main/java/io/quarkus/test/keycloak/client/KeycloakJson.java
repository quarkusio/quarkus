package io.quarkus.test.keycloak.client;

import java.io.IOException;
import java.io.InputStream;

import org.keycloak.admin.client.jackson3.Jackson3JsonMapper;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.util.JsonSerialization;

/**
 * (De)serializes the Keycloak representations with the Jackson 3 mapper of the Keycloak admin client, or with Keycloak's
 * Jackson 2 {@code JsonSerialization} when the Jackson 2 variant of the representations is on the classpath (it is
 * brought by the Keycloak authorization extension and then wins over the Jackson 3 variant).
 * <p>
 * Use it in tests instead of Keycloak's {@code JsonSerialization}, which this test framework no longer brings.
 */
public final class KeycloakJson {

    private static final boolean JACKSON2_REPRESENTATIONS = isPresent("org.keycloak.util.JsonSerialization");

    private KeycloakJson() {
    }

    public static RealmRepresentation readRealm(InputStream is) throws IOException {
        if (JACKSON2_REPRESENTATIONS) {
            return JsonSerialization.readValue(is, RealmRepresentation.class);
        }
        return jackson3Mapper().readValue(is, RealmRepresentation.class);
    }

    public static String writeRealm(RealmRepresentation realm) throws IOException {
        if (JACKSON2_REPRESENTATIONS) {
            return JsonSerialization.writeValueAsString(realm);
        }
        return jackson3Mapper().writeValueAsString(realm);
    }

    private static Jackson3JsonMapper jackson3Mapper() {
        return Jackson3.MAPPER;
    }

    /**
     * Holds the Jackson 3 mapper of the Keycloak admin client, so that it is only loaded when the Jackson 3 variant
     * of the representations is in use.
     */
    private static final class Jackson3 {
        static final Jackson3JsonMapper MAPPER = new Jackson3JsonMapper();
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
