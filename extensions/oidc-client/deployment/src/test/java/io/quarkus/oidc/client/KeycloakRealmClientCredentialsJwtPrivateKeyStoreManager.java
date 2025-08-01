package io.quarkus.oidc.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.util.JsonSerialization;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.restassured.RestAssured;
import io.smallrye.jwt.util.KeyUtils;
import io.smallrye.jwt.util.ResourceUtils;

public class KeycloakRealmClientCredentialsJwtPrivateKeyStoreManager implements QuarkusTestResourceLifecycleManager {
    private static final String KEYCLOAK_SERVER_URL = System.getProperty("keycloak.url", "http://localhost:8180/auth");
    private static final String KEYCLOAK_REALM = "quarkus6";

    @Override
    public Map<String, String> start() {
        try {
            RealmRepresentation realm = createRealm(KEYCLOAK_REALM);

            realm.getClients().add(createClient("quarkus-app"));

            RestAssured
                    .given()
                    .auth().oauth2(getAdminAccessToken())
                    .contentType("application/json")
                    .body(JsonSerialization.writeValueAsBytes(realm))
                    .when()
                    .post(KEYCLOAK_SERVER_URL + "/admin/realms").then()
                    .statusCode(201);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return Collections.emptyMap();
    }

    private static String getAdminAccessToken() {
        return RestAssured
                .given()
                .param("grant_type", "password")
                .param("username", "admin")
                .param("password", "admin")
                .param("client_id", "admin-cli")
                .when()
                .post(KEYCLOAK_SERVER_URL + "/realms/master/protocol/openid-connect/token")
                .as(AccessTokenResponse.class).getToken();
    }

    private static RealmRepresentation createRealm(String name) {
        RealmRepresentation realm = new RealmRepresentation();

        realm.setRealm(name);
        realm.setEnabled(true);
        realm.setClients(new ArrayList<>());
        realm.setAccessTokenLifespan(3);
        realm.setRevokeRefreshToken(true);
        realm.setRefreshTokenMaxReuse(0);
        realm.setDefaultGroups(Arrays.asList("user"));
        realm.setDefaultRoles(Arrays.asList("user"));
        realm.setRequiredActions(List.of());

        return realm;
    }

    private static ClientRepresentation createClient(String clientId) throws Exception {
        ClientRepresentation client = new ClientRepresentation();
        String privateKey = KeyUtils.removePemKeyBeginEnd(ResourceUtils.readResource("/exportedPrivateKey.pem"));
        String publicCert = removeCertBeginEnd(ResourceUtils.readResource("/exportedCertificate.pem"));
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("jwt.credential.certificate", publicCert);
        attributes.put("jwt.credential.private.key", privateKey);
        client.setAttributes(attributes);
        client.setClientId(clientId);
        client.setPublicClient(false);
        client.setClientAuthenticatorType("client-jwt");
        client.setDirectAccessGrantsEnabled(true);
        client.setServiceAccountsEnabled(true);
        client.setEnabled(true);

        return client;
    }

    private static String removeCertBeginEnd(String pem) {
        pem = pem.replaceAll("-----BEGIN(.*?)CERTIFICATE-----", "");
        pem = pem.replaceAll("-----END(.*?)CERTIFICATE-----", "");
        pem = pem.replaceAll("\r\n", "");
        pem = pem.replaceAll("\n", "");
        pem = pem.replaceAll("\\\\n", "");
        return pem.trim();
    }

    @Override
    public void stop() {
        RestAssured
                .given()
                .auth().oauth2(getAdminAccessToken())
                .when()
                .delete(KEYCLOAK_SERVER_URL + "/admin/realms/" + KEYCLOAK_REALM).thenReturn().prettyPrint();
    }
}
