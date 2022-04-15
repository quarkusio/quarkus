package io.quarkus.it.keycloak;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.RolesRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

@Path("/admin-client")
public class AdminClientResource {

    @ConfigProperty(name = "admin-url")
    String url;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("realm")
    public RealmRepresentation getRealm() {
        try (Keycloak keycloak = keycloak()) {
            return keycloak.realm("quarkus").toRepresentation();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("newrealm")
    public RealmRepresentation createRealm() {
        RealmRepresentation newRealm = createRealm("quarkus2");

        newRealm.getClients().add(createClient("quarkus-app2"));
        newRealm.getUsers().add(createUser("alice", "user"));

        try (Keycloak keycloak = keycloak()) {
            keycloak.realms().create(newRealm);
        }

        try (Keycloak keycloak = keycloak()) {
            return keycloak.realm("quarkus2").toRepresentation();
        }
    }

    private Keycloak keycloak() {
        try {
            return KeycloakBuilder.builder()
                    .serverUrl(url)
                    .realm("master")
                    .clientId("admin-cli")
                    .username("admin")
                    .password("admin")
                    .build();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
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

        return realm;
    }

    private static ClientRepresentation createClient(String clientId) {
        ClientRepresentation client = new ClientRepresentation();

        client.setClientId(clientId);
        client.setRedirectUris(Arrays.asList("*"));
        client.setPublicClient(false);
        client.setSecret("secret");
        client.setDirectAccessGrantsEnabled(true);
        client.setEnabled(true);

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
}
