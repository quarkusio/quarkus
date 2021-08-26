package io.quarkus.it.keycloak;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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

import io.quarkus.test.common.DevServicesContext;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.restassured.RestAssured;

public class KeycloakRealmResourceManager implements QuarkusTestResourceLifecycleManager, DevServicesContext.ContextAware {

    private static final String KEYCLOAK_REALM = "quarkus";
    String authServerUrl;

    @Override
    public Map<String, String> start() {

        RealmRepresentation realm = createRealm(KEYCLOAK_REALM);
        realm.setRevokeRefreshToken(true);
        realm.setRefreshTokenMaxReuse(0);
        realm.setAccessTokenLifespan(3);

        realm.getClients().add(createClient("quarkus-app"));
        realm.getClients().add(createClient("quarkus-app-exchange"));
        realm.getUsers().add(createUser("alice", "user"));
        realm.getUsers().add(createUser("bob", "user"));

        try {
            //dev services sets up a realm for us, we delete it because we want different settings
            RestAssured
                    .given()
                    .auth().oauth2(getAdminAccessToken())
                    .contentType("application/json")
                    .when()
                    .delete(authServerUrl + "/admin/realms/quarkus").then()
                    .statusCode(204);
            RestAssured
                    .given()
                    .auth().oauth2(getAdminAccessToken())
                    .contentType("application/json")
                    .body(JsonSerialization.writeValueAsBytes(realm))
                    .when()
                    .post(authServerUrl + "/admin/realms").then()
                    .statusCode(201);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return Collections.emptyMap();
    }

    private String getAdminAccessToken() {
        return RestAssured
                .given()
                .param("grant_type", "password")
                .param("username", "admin")
                .param("password", "admin")
                .param("client_id", "admin-cli")
                .when()
                .post(authServerUrl + "/realms/master/protocol/openid-connect/token")
                .as(AccessTokenResponse.class).getToken();
    }

    private RealmRepresentation createRealm(String name) {
        RealmRepresentation realm = new RealmRepresentation();

        realm.setRealm(name);
        realm.setEnabled(true);
        realm.setUsers(new ArrayList<>());
        realm.setClients(new ArrayList<>());
        realm.setAccessTokenLifespan(3);

        RolesRepresentation roles = new RolesRepresentation();
        List<RoleRepresentation> realmRoles = new ArrayList<>();

        roles.setRealm(realmRoles);
        realm.setRoles(roles);

        realm.getRoles().getRealm().add(new RoleRepresentation("user", null, false));
        realm.getRoles().getRealm().add(new RoleRepresentation("admin", null, false));

        return realm;
    }

    private ClientRepresentation createClient(String clientId) {
        ClientRepresentation client = new ClientRepresentation();

        client.setClientId(clientId);
        client.setPublicClient(false);
        client.setSecret("secret");
        client.setDirectAccessGrantsEnabled(true);
        client.setServiceAccountsEnabled(true);
        client.setEnabled(true);

        return client;
    }

    private UserRepresentation createUser(String username, String... realmRoles) {
        UserRepresentation user = new UserRepresentation();

        user.setUsername(username);
        user.setEnabled(true);
        user.setCredentials(new ArrayList<>());
        user.setRealmRoles(Arrays.asList(realmRoles));
        user.setEmail(username + "@gmail.com");

        CredentialRepresentation credential = new CredentialRepresentation();

        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(username);
        credential.setTemporary(false);

        user.getCredentials().add(credential);

        return user;
    }

    @Override
    public void stop() {

        RestAssured
                .given()
                .auth().oauth2(getAdminAccessToken())
                .when()
                .delete(authServerUrl + "/admin/realms/" + KEYCLOAK_REALM).then().statusCode(204);
    }

    @Override
    public void setIntegrationTestContext(DevServicesContext context) {
        try {
            var uri = new URI(context.devServicesProperties().get("quarkus.oidc.auth-server-url"));
            authServerUrl = new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(), "/auth", null, null)
                    .toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
