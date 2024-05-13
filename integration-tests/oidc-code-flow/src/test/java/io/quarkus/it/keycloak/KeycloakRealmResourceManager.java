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

    private static final String KEYCLOAK_REALM = "quarkus";
    final KeycloakTestClient client = new KeycloakTestClient();

    private List<RealmRepresentation> realms = new ArrayList<>();

    @Override
    public Map<String, String> start() {

        RealmRepresentation realm = createRealm(KEYCLOAK_REALM, "secret");
        client.createRealm(realm);
        realms.add(realm);

        RealmRepresentation logoutRealm = createRealm("logout-realm", "eUk1p7UB3nFiXZGUXi0uph1Y9p34YhBU");
        // revoke refresh tokens so that they can only be used once
        logoutRealm.setRevokeRefreshToken(true);
        logoutRealm.setRefreshTokenMaxReuse(0);
        logoutRealm.setSsoSessionMaxLifespan(10);
        logoutRealm.setAccessTokenLifespan(5);
        client.createRealm(logoutRealm);
        realms.add(logoutRealm);
        return Collections.emptyMap();
    }

    private static RealmRepresentation createRealm(String name, String defaultClientSecret) {
        RealmRepresentation realm = new RealmRepresentation();

        realm.setRealm(name);
        realm.setEnabled(true);
        realm.setUsers(new ArrayList<>());
        realm.setClients(new ArrayList<>());
        realm.setSsoSessionMaxLifespan(3); // sec
        realm.setAccessTokenLifespan(4); // 3 seconds

        RolesRepresentation roles = new RolesRepresentation();
        List<RoleRepresentation> realmRoles = new ArrayList<>();

        roles.setRealm(realmRoles);
        realm.setRoles(roles);

        realm.getRoles().getRealm().add(new RoleRepresentation("user", null, false));
        realm.getRoles().getRealm().add(new RoleRepresentation("admin", null, false));
        realm.getRoles().getRealm().add(new RoleRepresentation("confidential", null, false));

        realm.getClients().add(createClient("quarkus-app", defaultClientSecret));
        realm.getClients().add(createClientJwt("quarkus-app-jwt"));
        realm.getUsers().add(createUser("alice", "user"));
        realm.getUsers().add(createUser("admin", "user", "admin"));
        realm.getUsers().add(createUser("jdoe", "user", "confidential"));

        return realm;
    }

    private static ClientRepresentation createClientJwt(String clientId) {
        ClientRepresentation client = new ClientRepresentation();

        client.setClientId(clientId);
        client.setEnabled(true);
        client.setRedirectUris(Arrays.asList("*"));
        client.setClientAuthenticatorType("client-secret-jwt");
        client.setSecret("AyM1SysPpbyDfgZld3umj1qzKObwVMkoqQ-EstJQLr_T-1qS0gZH75aKtMN3Yj0iPS4hcgUuTwjAzZr1Z9CAow");

        return client;
    }

    private static ClientRepresentation createClient(String clientId, String secret) {
        ClientRepresentation client = new ClientRepresentation();

        client.setClientId(clientId);
        client.setEnabled(true);
        client.setRedirectUris(Arrays.asList("*"));
        client.setClientAuthenticatorType("client-secret");
        client.setSecret(secret);

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
        for (RealmRepresentation realm : realms) {
            client.deleteRealm(realm);
        }
    }

    @Override
    public void setIntegrationTestContext(DevServicesContext context) {
        client.setIntegrationTestContext(context);
    }

}
