package io.quarkus.it.keycloak;

import java.util.ArrayList;
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
import io.quarkus.test.keycloak.client.KeycloakTestClient.Tls;

public class KeycloakXTestResourceLifecycleManager
        implements QuarkusTestResourceLifecycleManager, DevServicesContext.ContextAware {

    private static final String KEYCLOAK_REALM = "quarkus";
    private static final String KEYCLOAK_SERVICE_CLIENT = "quarkus-app";

    final KeycloakTestClient client = new KeycloakTestClient(new Tls());

    @Override
    public Map<String, String> start() {

        RealmRepresentation realm = createRealm(KEYCLOAK_REALM);
        client.createRealm(realm);

        return Map.of();
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

    @Override
    public void setIntegrationTestContext(DevServicesContext context) {
        client.setIntegrationTestContext(context);
    }

    @Override
    public void stop() {
    }

}
