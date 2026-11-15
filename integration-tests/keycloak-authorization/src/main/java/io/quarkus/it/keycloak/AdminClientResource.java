package io.quarkus.it.keycloak;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.ClientProfilesRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.RolesRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

@Path("/admin-client")
public class AdminClientResource {

    @Inject
    Keycloak keycloak;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("realm")
    public String getRealm() {
        return keycloak.realm("quarkus").toRepresentation().getRealm();
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("realms")
    public String getRealms() {
        return keycloak.realms().findAll().stream().map(r -> r.getRealm()).sorted().collect(Collectors.joining("-"));
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("roles")
    public String getRoles() {
        return keycloak.realm("quarkus").roles().list().stream().map(r -> r.getName()).sorted()
                .collect(Collectors.joining("-"));
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("users")
    public String getUsers() {
        return keycloak.realm("quarkus").users().list().stream().map(r -> r.getUsername()).sorted()
                .collect(Collectors.joining("-"));
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("newrealm")
    public String createRealm() {
        RealmRepresentation newRealm = createRealm("quarkus2");

        newRealm.getClients().add(createClient("quarkus-app2"));
        newRealm.getUsers().add(createUser("alice", "user"));
        keycloak.realms().create(newRealm);
        return keycloak.realm("quarkus2").toRepresentation().getRealm();
    }

    @PUT
    @Produces(MediaType.TEXT_PLAIN)
    @Path("empty-realm-client-profiles")
    public String putEmptyRealmClientProfiles() {
        // the Jackson 2 variant of the Keycloak representations (used by the Keycloak policy enforcer) wins over
        // the Jackson 3 variant in this application, so the client profiles are a Jackson 2 JsonNode that
        // the Jackson 3 based admin client must bridge when it (de)serializes the realm
        RealmRepresentation realm = keycloak.realm("quarkus").toRepresentation();
        realm.setParsedClientProfiles(new ClientProfilesRepresentation());
        keycloak.realm("quarkus").update(realm);
        ClientProfilesRepresentation profiles = realm.getParsedClientProfiles();
        int profilesCount = profiles == null ? -1 : profiles.getProfiles().size();
        boolean hasGlobalProfiles = !keycloak.realm("quarkus").clientPoliciesProfilesResource().getProfiles(true)
                .getGlobalProfiles().isEmpty();
        return realm.getRealm() + ":" + profilesCount + ":" + hasGlobalProfiles;
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
