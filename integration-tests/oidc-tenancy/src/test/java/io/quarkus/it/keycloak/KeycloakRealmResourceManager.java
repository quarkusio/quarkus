package io.quarkus.it.keycloak;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.RolesRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import io.quarkus.test.common.DevServicesContext;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.keycloak.client.KeycloakTestClient;

public class KeycloakRealmResourceManager implements QuarkusTestResourceLifecycleManager, DevServicesContext.ContextAware {

    private static final String KEYCLOAK_REALM = "quarkus-";
    final KeycloakTestClient client = new KeycloakTestClient();

    @Override
    public Map<String, String> start() {
        for (String realmId : Arrays.asList("a", "b", "c", "d", "e", "f", "webapp", "webapp2", "hybrid")) {
            RealmRepresentation realm = createRealm(KEYCLOAK_REALM + realmId);

            realm.getClients().add(createClient("quarkus-app-" + realmId));
            if ("b".equals(realmId)) {
                realm.getClients().add(createClient("quarkus-app-b2"));
            }
            realm.getUsers().add(createUser("alice", "user"));
            realm.getUsers().add(createUser("admin", "user", "admin"));
            realm.getUsers().add(createUser("jdoe", "user", "confidential"));

            client.createRealm(realm);
        }
        return Collections.emptyMap();
    }

    private static RealmRepresentation createRealm(String name) {
        RealmRepresentation realm = new RealmRepresentation();

        realm.setRealm(name);
        realm.setEnabled(true);
        realm.setUsers(new ArrayList<>());
        realm.setClients(new ArrayList<>());

        RolesRepresentation roles = new RolesRepresentation();
        List<RoleRepresentation> realmRoles = new ArrayList<>();

        roles.setRealm(realmRoles);
        realm.setRoles(roles);

        realm.getRoles().getRealm().add(new RoleRepresentation("user", null, false));
        realm.getRoles().getRealm().add(new RoleRepresentation("admin", null, false));
        realm.getRoles().getRealm().add(new RoleRepresentation("confidential", null, false));

        return realm;
    }

    private static ClientRepresentation createClient(String clientId) {
        ClientRepresentation client = new ClientRepresentation();

        client.setClientId(clientId);
        client.setPublicClient(false);
        client.setSecret("secret");
        client.setDirectAccessGrantsEnabled(true);
        client.setEnabled(true);
        client.setDefaultRoles(new String[] { "role-" + clientId });
        if (clientId.startsWith("quarkus-app-webapp") || clientId.equals("quarkus-app-hybrid")) {
            client.setRedirectUris(Arrays.asList("*"));
        }
        if (clientId.equals("quarkus-app-webapp") || clientId.equals("quarkus-app-hybrid")) {
            // This instructs Keycloak to include the roles with the ID token too
            client.setDefaultClientScopes(Arrays.asList("microprofile-jwt"));
        }
        return client;
    }

    private static UserRepresentation createUser(String username, String... realmRoles) {
        UserRepresentation user = new UserRepresentation();

        user.setUsername(username);
        user.setEnabled(true);
        user.setCredentials(new ArrayList<>());
        user.setRealmRoles(Arrays.asList(realmRoles));

        CredentialRepresentation credential = new CredentialRepresentation();

        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(username);
        credential.setTemporary(false);

        user.getCredentials().add(credential);

        return user;
    }

    @Override
    public void stop() {
        for (String realmId : Arrays.asList("a", "b", "c", "d", "webapp", "webapp2", "hybrid")) {
            try {
                client.deleteRealm(realmId);
            } catch (Throwable t) {

            }
        }
    }

    @Override
    public void setIntegrationTestContext(DevServicesContext context) {
        client.setIntegrationTestContext(context);
    }
}
