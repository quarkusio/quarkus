package io.quarkus.it.keycloak;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.RolesRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.util.JsonSerialization;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;

public class KeycloakXTestResourceLifecycleManager implements QuarkusTestResourceLifecycleManager {
    private static final Logger LOGGER = Logger.getLogger(KeycloakXTestResourceLifecycleManager.class);
    private GenericContainer<?> keycloak;

    private static String KEYCLOAK_SERVER_URL;
    private static final String KEYCLOAK_REALM = "quarkus";
    private static final String KEYCLOAK_SERVICE_CLIENT = "quarkus-service-app";
    private static final String KEYCLOAK_VERSION = System.getProperty("keycloak.version", "23.0.1");

    private static String CLIENT_KEYSTORE = "client-keystore.jks";
    private static String CLIENT_TRUSTSTORE = "client-truststore.jks";

    private static String SERVER_KEYSTORE = "server-keystore.jks";
    private static String SERVER_KEYSTORE_MOUNTED_PATH = "/etc/server-keystore.jks";
    private static String SERVER_TRUSTSTORE = "server-truststore.jks";
    private static String SERVER_TRUSTSTORE_MOUNTED_PATH = "/etc/server-truststore.jks";

    @SuppressWarnings("resource")
    @Override
    public Map<String, String> start() {
        keycloak = new GenericContainer<>("quay.io/keycloak/keycloak:" + KEYCLOAK_VERSION)
                .withExposedPorts(8080, 8443)
                .withEnv("KEYCLOAK_ADMIN", "admin")
                .withEnv("KEYCLOAK_ADMIN_PASSWORD", "admin")
                .waitingFor(Wait.forLogMessage(".*Keycloak.*started.*", 1));

        keycloak = keycloak
                .withClasspathResourceMapping(SERVER_KEYSTORE, SERVER_KEYSTORE_MOUNTED_PATH, BindMode.READ_ONLY)
                .withClasspathResourceMapping(SERVER_TRUSTSTORE, SERVER_TRUSTSTORE_MOUNTED_PATH, BindMode.READ_ONLY)
                .withCommand("build --https-client-auth=required")
                .withCommand(String.format(
                        "start --https-client-auth=required --hostname-strict=false --hostname-strict-https=false"
                                + " --https-key-store-file=%s --https-trust-store-file=%s --https-trust-store-password=password",
                        SERVER_KEYSTORE_MOUNTED_PATH, SERVER_TRUSTSTORE_MOUNTED_PATH));
        keycloak.start();
        LOGGER.info(keycloak.getLogs());

        KEYCLOAK_SERVER_URL = "https://localhost:" + keycloak.getMappedPort(8443);

        RealmRepresentation realm = createRealm(KEYCLOAK_REALM);
        postRealm(realm);

        return Map.of("quarkus.oidc.auth-server-url", KEYCLOAK_SERVER_URL + "/realms/" + KEYCLOAK_REALM);
    }

    private static void postRealm(RealmRepresentation realm) {
        try {
            createRequestSpec().auth().oauth2(getAdminAccessToken())
                    .contentType("application/json")
                    .body(JsonSerialization.writeValueAsBytes(realm))
                    .when()
                    .post(KEYCLOAK_SERVER_URL + "/admin/realms").then()
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

        realm.getUsers().add(createUser("alice", List.of("user")));
        realm.getUsers().add(createUser("admin", List.of("user", "admin")));
        realm.getUsers().add(createUser("jdoe", List.of("user", "confidential")));

        return realm;
    }

    private static String getAdminAccessToken() {
        return createRequestSpec()
                .param("grant_type", "password")
                .param("username", "admin")
                .param("password", "admin")
                .param("client_id", "admin-cli")
                .when()
                .post(KEYCLOAK_SERVER_URL + "/realms/master/protocol/openid-connect/token")
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

        return client;
    }

    private static UserRepresentation createUser(String username, List<String> realmRoles) {
        UserRepresentation user = new UserRepresentation();

        user.setUsername(username);
        user.setEnabled(true);
        user.setCredentials(new ArrayList<>());
        user.setRealmRoles(realmRoles);
        user.setEmail(username + "@gmail.com");

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
                .post(KEYCLOAK_SERVER_URL + "/realms/" + KEYCLOAK_REALM + "/protocol/openid-connect/token")
                .as(AccessTokenResponse.class).getToken();
    }

    public static String getRefreshToken(String userName) {
        return createRequestSpec().param("grant_type", "password")
                .param("username", userName)
                .param("password", userName)
                .param("client_id", KEYCLOAK_SERVICE_CLIENT)
                .param("client_secret", "secret")
                .when()
                .post(KEYCLOAK_SERVER_URL + "/realms/" + KEYCLOAK_REALM + "/protocol/openid-connect/token")
                .as(AccessTokenResponse.class).getRefreshToken();
    }

    @Override
    public void stop() {
        createRequestSpec().auth().oauth2(getAdminAccessToken())
                .when()
                .delete(KEYCLOAK_SERVER_URL + "/admin/realms/" + KEYCLOAK_REALM).then().statusCode(204);

        keycloak.stop();
    }

    private static RequestSpecification createRequestSpec() {
        return RestAssured.given().trustStore(CLIENT_TRUSTSTORE, "password")
                .keyStore(CLIENT_KEYSTORE, "password");
    }
}
