package io.quarkus.it.keycloak;

import jakarta.inject.Inject;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.ClientProfilesRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;

@Path("/keycloak-admin-client")
public class KeycloakAdminClientResource {

    @Inject
    Keycloak keycloak;

    @PUT
    @Path("/empty-realm-client-profiles")
    @Produces(MediaType.TEXT_PLAIN)
    public String putEmptyRealmClientProfiles() {
        RealmRepresentation realm = keycloak.realm("quarkus").toRepresentation();
        // we want to create serialization and/or deserialization so that object mapper is initialized
        realm.setParsedClientProfiles(new ClientProfilesRepresentation());
        keycloak.realm("quarkus").update(realm);
        ClientProfilesRepresentation profiles = realm.getParsedClientProfiles();
        int profilesCount = profiles == null ? -1 : 0;
        return realm.getRealm() + ":" + profilesCount;
    }
}
