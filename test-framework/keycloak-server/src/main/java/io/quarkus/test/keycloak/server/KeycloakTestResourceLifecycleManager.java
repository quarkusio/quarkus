package io.quarkus.test.keycloak.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.RolesRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.util.JsonSerialization;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;

public class KeycloakTestResourceLifecycleManager implements QuarkusTestResourceLifecycleManager {

    private static KeycloakContainer keycloak;

    private static final String KEYCLOAK_REALM = System.getProperty("keycloak.realm", "quarkus");
    private static final String KEYCLOAK_SERVICE_CLIENT = System.getProperty("keycloak.service.client", "quarkus-service-app");
    private static final String KEYCLOAK_WEB_APP_CLIENT = System.getProperty("keycloak.web-app.client", "quarkus-web-app");
    private static final Boolean KEYCLOAK_USE_HTTPS = Boolean.valueOf(System.getProperty("keycloak.use.https", "true"));

    private static final String TOKEN_USER_ROLES = System.getProperty("keycloak.token.user-roles", "user");
    private static final String TOKEN_ADMIN_ROLES = System.getProperty("keycloak.token.admin-roles", "user,admin");

    @SuppressWarnings("resource")
    @Override
    public Map<String, String> start() {
        keycloak = new KeycloakContainer()
                .withUseHttps(KEYCLOAK_USE_HTTPS);
        keycloak.start();

        RealmRepresentation realm = createRealm(KEYCLOAK_REALM);
        postRealm(realm);

        Map<String, String> conf = new HashMap<>();
        conf.put("keycloak.url", keycloak.getServerUrl());
        conf.put("quarkus.oidc.auth-server-url", keycloak.getServerUrl() + "/realms/" + KEYCLOAK_REALM);

        return conf;
    }

    private static void postRealm(RealmRepresentation realm) {
        try {
            createRequestSpec().auth().oauth2(getAdminAccessToken())
                    .contentType("application/json")
                    .body(JsonSerialization.writeValueAsBytes(realm))
                    .when()
                    .post(keycloak.getServerUrl() + "/admin/realms").then()
                    .statusCode(201);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static RealmRepresentation createRealm(String name) {
        RealmRepresentation realm = new RealmRepresentation();

        realm.setRealm(name);
        realm.setEnabled(true);
        realm.setUsers(new ArrayList<>());
        realm.setClients(new ArrayList<>());
        realm.setAccessTokenLifespan(3);
        realm.setSsoSessionMaxLifespan(3);

        RolesRepresentation roles = new RolesRepresentation();
        List<RoleRepresentation> realmRoles = new ArrayList<>();

        roles.setRealm(realmRoles);
        realm.setRoles(roles);

        realm.getRoles().getRealm().add(new RoleRepresentation("user", null, false));
        realm.getRoles().getRealm().add(new RoleRepresentation("admin", null, false));
        realm.getRoles().getRealm().add(new RoleRepresentation("confidential", null, false));

        realm.getClients().add(createServiceClient(KEYCLOAK_SERVICE_CLIENT));
        realm.getClients().add(createWebAppClient(KEYCLOAK_WEB_APP_CLIENT));

        realm.getUsers().add(createUser("alice", getUserRoles()));
        realm.getUsers().add(createUser("admin", getAdminRoles()));
        realm.getUsers().add(createUser("jdoe", Arrays.asList("user", "confidential")));

        return realm;
    }

    private static String getAdminAccessToken() {
        return createRequestSpec()
                .param("grant_type", "password")
                .param("username", "admin")
                .param("password", "admin")
                .param("client_id", "admin-cli")
                .when()
                .post(keycloak.getServerUrl() + "/realms/master/protocol/openid-connect/token")
                .as(AccessTokenResponse.class).getToken();
    }

    private static ClientRepresentation createServiceClient(String clientId) {
        ClientRepresentation client = new ClientRepresentation();

        client.setClientId(clientId);
        client.setPublicClient(false);
        client.setSecret("secret");
        client.setDirectAccessGrantsEnabled(true);
        client.setServiceAccountsEnabled(true);
        client.setEnabled(true);
        client.setDefaultClientScopes(List.of("microprofile-jwt", "basic"));

        return client;
    }

    private static ClientRepresentation createWebAppClient(String clientId) {
        ClientRepresentation client = new ClientRepresentation();

        client.setClientId(clientId);
        client.setPublicClient(false);
        client.setSecret("secret");
        client.setRedirectUris(Arrays.asList("*"));
        client.setEnabled(true);
        client.setDefaultClientScopes(List.of("microprofile-jwt", "basic"));

        return client;
    }

    private static UserRepresentation createUser(String username, List<String> realmRoles) {
        UserRepresentation user = new UserRepresentation();

        user.setUsername(username);
        user.setEnabled(true);
        user.setCredentials(new ArrayList<>());
        user.setRealmRoles(realmRoles);
        user.setEmail(username + "@gmail.com");
        user.setEmailVerified(true);
        user.setFirstName(username);
        user.setLastName(username);

        CredentialRepresentation credential = new CredentialRepresentation();

        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(username);
        credential.setTemporary(false);

        user.getCredentials().add(credential);

        return user;
    }

    public static String getAccessToken(String userName) {
        return createRequestSpec().param("grant_type", "password")
                .param("username", userName)
                .param("password", userName)
                .param("client_id", KEYCLOAK_SERVICE_CLIENT)
                .param("client_secret", "secret")
                .when()
                .post(keycloak.getServerUrl() + "/realms/" + KEYCLOAK_REALM + "/protocol/openid-connect/token")
                .as(AccessTokenResponse.class).getToken();
    }

    public static String getRefreshToken(String userName) {
        return createRequestSpec().param("grant_type", "password")
                .param("username", userName)
                .param("password", userName)
                .param("client_id", KEYCLOAK_SERVICE_CLIENT)
                .param("client_secret", "secret")
                .when()
                .post(keycloak.getServerUrl() + "/realms/" + KEYCLOAK_REALM + "/protocol/openid-connect/token")
                .as(AccessTokenResponse.class).getRefreshToken();
    }

    @Override
    public void stop() {
        createRequestSpec().auth().oauth2(getAdminAccessToken())
                .when()
                .delete(keycloak.getServerUrl() + "/admin/realms/" + KEYCLOAK_REALM).then().statusCode(204);

        keycloak.stop();
    }

    private static List<String> getAdminRoles() {
        return Arrays.asList(TOKEN_ADMIN_ROLES.split(","));
    }

    private static List<String> getUserRoles() {
        return Arrays.asList(TOKEN_USER_ROLES.split(","));
    }

    private static RequestSpecification createRequestSpec() {
        return RestAssured.given();
    }
}
